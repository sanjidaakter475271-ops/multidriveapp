/**
 * MultiDrive OAuth Backend
 *
 * ARCHITECTURE:
 *   [Android App] ──Chrome Custom Tabs──> [This Backend /auth/start]
 *        ──redirect──> [Google OAuth consent]
 *        ──code──> [This Backend /oauth/callback]
 *        ──token exchange──> [https://oauth2.googleapis.com/token]
 *        ──deep link──> multidrive://auth-done?account=email&status=ok
 *        ──> [App reopens, shows new account]
 *
 * IMPORTANT:
 *   - GOOGLE_CLIENT_SECRET is only here, never in the Android app
 *   - Uses response_type=code + access_type=offline → refresh_token included
 *   - State param prevents CSRF attacks
 *   - Tokens stored in-memory for demo; replace with encrypted DB in production
 *
 * DEPLOY:
 *   1. Set up a domain (Cloudflare Tunnel / DuckDNS + Let's Encrypt)
 *   2. Copy .env.example → .env and fill in values
 *   3. npm install && npm start
 *   4. Test: curl https://yourdomain.com/health
 */

require('dotenv').config();
const express = require('express');
const axios = require('axios');
const crypto = require('crypto');
const rateLimit = require('express-rate-limit');

const app = express();
app.use(express.json());

// Rate limiting to prevent abuse
const limiter = rateLimit({
  windowMs: 15 * 60 * 1000, // 15 minutes
  max: 100
});
app.use(limiter);

// ─────────────────────────────────────────────
// Config (from .env)
// ─────────────────────────────────────────────
const {
  GOOGLE_CLIENT_ID,
  GOOGLE_CLIENT_SECRET,
  REDIRECT_URI,
  BASE_URL,
  STATE_SECRET = 'change_me',
  PORT = 3000
} = process.env;

const SCOPES = [
  'https://www.googleapis.com/auth/drive',
  'https://www.googleapis.com/auth/userinfo.email',
  'https://www.googleapis.com/auth/userinfo.profile'
].join(' ');

// In-memory token store — replace with encrypted DB in production
// Structure: { email: { accessToken, refreshToken, expiresAt, email, displayName, photoUrl } }
const tokenStore = {};

// In-memory CSRF state store — replace with Redis in production
const stateStore = new Map();

// ─────────────────────────────────────────────
// Helpers
// ─────────────────────────────────────────────
function generateState() {
  return crypto.randomBytes(32).toString('hex');
}

function buildDeepLink(email, status = 'ok', error = null) {
  const base = `multidrive://auth-done?account=${encodeURIComponent(email)}&status=${status}`;
  return error ? `${base}&error=${encodeURIComponent(error)}` : base;
}

// ─────────────────────────────────────────────
// Health check
// ─────────────────────────────────────────────
app.get('/health', (req, res) => {
  res.json({ status: 'ok', version: '1.0.0' });
});

// ─────────────────────────────────────────────
// Step 1: App opens this URL in Chrome Custom Tabs
// GET /auth/start
// Redirects to Google OAuth consent screen
// ─────────────────────────────────────────────
app.get('/auth/start', (req, res) => {
  const state = generateState();

  // Store state with expiry (10 minutes)
  stateStore.set(state, { createdAt: Date.now() });

  // Clean up old states
  for (const [key, val] of stateStore.entries()) {
    if (Date.now() - val.createdAt > 10 * 60 * 1000) stateStore.delete(key);
  }

  const authUrl = new URL('https://accounts.google.com/o/oauth2/v2/auth');
  authUrl.searchParams.set('client_id', GOOGLE_CLIENT_ID);
  authUrl.searchParams.set('redirect_uri', REDIRECT_URI);
  authUrl.searchParams.set('response_type', 'code');        // Authorization Code flow
  authUrl.searchParams.set('access_type', 'offline');       // Get refresh_token
  authUrl.searchParams.set('scope', SCOPES);
  authUrl.searchParams.set('prompt', 'consent select_account'); // Always show account chooser
  authUrl.searchParams.set('state', state);

  console.log(`[auth/start] Redirecting to Google OAuth, state=${state.slice(0, 8)}...`);
  res.redirect(authUrl.toString());
});

// ─────────────────────────────────────────────
// Step 2: Google redirects here after user grants permission
// GET /oauth/callback?code=...&state=...
// Exchanges authorization code for access + refresh tokens
// Then redirects to multidrive://auth-done deep link
// ─────────────────────────────────────────────
app.get('/oauth/callback', async (req, res) => {
  const { code, state, error } = req.query;

  // Handle user-denied or error case
  if (error || !code) {
    console.error(`[oauth/callback] OAuth error: ${error || 'no code'}`);
    return res.redirect(`multidrive://auth-done?status=error&error=${encodeURIComponent(error || 'No authorization code')}`);
  }

  // CSRF state verification
  if (!state || !stateStore.has(state)) {
    console.error('[oauth/callback] Invalid or expired state parameter');
    return res.redirect('multidrive://auth-done?status=error&error=invalid_state');
  }
  stateStore.delete(state);

  try {
    // Exchange authorization code for access_token + refresh_token
    const tokenResponse = await axios.post('https://oauth2.googleapis.com/token', null, {
      params: {
        code,
        client_id: GOOGLE_CLIENT_ID,
        client_secret: GOOGLE_CLIENT_SECRET,
        redirect_uri: REDIRECT_URI,
        grant_type: 'authorization_code'
      }
    });

    const {
      access_token: accessToken,
      refresh_token: refreshToken,
      expires_in: expiresIn
    } = tokenResponse.data;

    // Fetch user info to get email and display name
    const userInfoResponse = await axios.get('https://www.googleapis.com/oauth2/v3/userinfo', {
      headers: { Authorization: `Bearer ${accessToken}` }
    });

    const { email, name, picture, sub: googleSubjectId } = userInfoResponse.data;

    // Store tokens per account (encrypt refresh_token in production!)
    tokenStore[email] = {
      email,
      displayName: name || email,
      photoUrl: picture || null,
      googleSubjectId,
      accessToken,
      refreshToken: refreshToken || tokenStore[email]?.refreshToken, // Preserve if not re-issued
      expiresAt: Date.now() + (expiresIn * 1000),
      status: 'CONNECTED',
      storageQuota: 0,
      storageUsed: 0
    };

    console.log(`[oauth/callback] Account authorized: ${email}`);

    // Redirect app via deep link: multidrive://auth-done?account=email&status=ok
    res.redirect(buildDeepLink(email, 'ok'));

  } catch (err) {
    console.error('[oauth/callback] Token exchange error:', err.response?.data || err.message);
    res.redirect(`multidrive://auth-done?status=error&error=${encodeURIComponent(err.message || 'Token exchange failed')}`);
  }
});

// ─────────────────────────────────────────────
// API: App fetches account token for Drive API calls
// GET /api/token/:email
// Returns access_token (refreshing if needed)
// ─────────────────────────────────────────────
app.get('/api/token/:email', async (req, res) => {
  const { email } = req.params;
  const account = tokenStore[email];

  if (!account) {
    return res.status(404).json({ error: 'Account not found' });
  }

  // Refresh access_token if expired
  if (Date.now() >= account.expiresAt && account.refreshToken) {
    try {
      const refreshResponse = await axios.post('https://oauth2.googleapis.com/token', null, {
        params: {
          client_id: GOOGLE_CLIENT_ID,
          client_secret: GOOGLE_CLIENT_SECRET,
          refresh_token: account.refreshToken,
          grant_type: 'refresh_token'
        }
      });
      account.accessToken = refreshResponse.data.access_token;
      account.expiresAt = Date.now() + (refreshResponse.data.expires_in * 1000);
      account.status = 'CONNECTED';
      console.log(`[api/token] Refreshed token for ${email}`);
    } catch (err) {
      console.error(`[api/token] Refresh failed for ${email}:`, err.response?.data || err.message);
      account.status = 'REAUTH_REQUIRED';
      return res.status(401).json({ error: 'Token refresh failed, reauth required' });
    }
  }

  res.json({
    email: account.email,
    accessToken: account.accessToken,
    expiresAt: account.expiresAt,
    status: account.status
  });
});

// ─────────────────────────────────────────────
// API: List all connected accounts
// GET /api/accounts
// ─────────────────────────────────────────────
app.get('/api/accounts', (req, res) => {
  const accounts = Object.values(tokenStore).map(({ email, displayName, photoUrl, googleSubjectId, expiresAt, status }) => ({
    email,
    displayName,
    photoUrl,
    googleSubjectId,
    expiresAt,
    status
  }));
  res.json({ accounts });
});

// ─────────────────────────────────────────────
// API: Remove account (disconnect)
// DELETE /api/accounts/:email
// ─────────────────────────────────────────────
app.delete('/api/accounts/:email', async (req, res) => {
  const { email } = req.params;
  const account = tokenStore[email];

  if (account?.accessToken) {
    // Optionally revoke token at Google
    try {
      await axios.post(`https://oauth2.googleapis.com/revoke?token=${account.accessToken}`);
    } catch (_) { /* ignore revoke errors */ }
  }

  delete tokenStore[email];
  res.json({ success: true });
});

// ─────────────────────────────────────────────
// Start server
// ─────────────────────────────────────────────
app.listen(PORT, () => {
  console.log(`MultiDrive OAuth Backend running on port ${PORT}`);
  console.log(`Auth start URL: ${BASE_URL}/auth/start`);
  console.log(`Callback URL:   ${REDIRECT_URI}`);
});

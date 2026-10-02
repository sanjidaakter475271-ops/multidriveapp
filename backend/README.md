# MultiDrive OAuth Backend — Setup Guide

## Architecture
```
[App] → Chrome Custom Tabs → [Backend /auth/start]
     → Google OAuth (response_type=code, access_type=offline)
     → [Backend /oauth/callback?code=...]
     → token exchange at oauth2.googleapis.com/token
     → multidrive://auth-done?account=email&status=ok
     → [App deep link handler → AccountsViewModel.syncNewAccountFromBackend]
```

## Prerequisites
1. A domain with HTTPS (Cloudflare Tunnel / DuckDNS + Let's Encrypt)
2. Node.js 18+
3. A Google Cloud project with:
   - Google Drive API enabled
   - OAuth consent screen configured (add itasst271@gmail.com as test user)
   - OAuth client type: **Web application**
   - Authorized redirect URI: `https://yourdomain.com/oauth/callback`

## Setup

```bash
cd backend
cp .env.example .env
# Edit .env with your real GOOGLE_CLIENT_ID, GOOGLE_CLIENT_SECRET, REDIRECT_URI, BASE_URL
npm install
npm start
```

## With Cloudflare Tunnel (free, no domain needed)
```bash
# Install cloudflared: https://developers.cloudflare.com/cloudflare-one/connections/connect-networks/downloads/
cloudflared tunnel --url http://localhost:3000
# Copy the generated *.trycloudflare.com URL
# Set it as BASE_URL and update REDIRECT_URI in .env
# Add the callback URL to Google Cloud Console → Authorized redirect URIs
```

## With DuckDNS + nginx + Let's Encrypt
```nginx
server {
    listen 443 ssl;
    server_name multidrive-auth.duckdns.org;

    ssl_certificate /etc/letsencrypt/live/multidrive-auth.duckdns.org/fullchain.pem;
    ssl_certificate_key /etc/letsencrypt/live/multidrive-auth.duckdns.org/privkey.pem;

    location / {
        proxy_pass http://localhost:3000;
        proxy_set_header Host $host;
        proxy_set_header X-Forwarded-Proto https;
        proxy_set_header X-Real-IP $remote_addr;
    }
}
```

## Google Cloud Console CSRF note
- Consent screen in "Testing" mode → refresh_token expires after 7 days
- Add test users under "OAuth consent screen" → "Test users"
- Publish to production when ready

## API Endpoints

| Method | Path | Description |
|--------|------|-------------|
| GET | /health | Health check |
| GET | /auth/start | Opens Google OAuth in browser |
| GET | /oauth/callback | Receives Google authorization code, exchanges it |
| GET | /api/token/:email | Returns valid access_token (refreshes if needed) |
| GET | /api/accounts | Lists all connected accounts |
| DELETE | /api/accounts/:email | Disconnects and revokes an account |

## Security Notes
- `GOOGLE_CLIENT_SECRET` is only in `.env` on your server — never in the Android app
- `state` parameter prevents CSRF attacks
- In production: encrypt refresh tokens at rest, use Redis for state store
- Add authentication to `/api/*` endpoints to prevent unauthorized token access

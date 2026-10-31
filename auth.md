# Google OAuth Authentication System

## Overview

This document describes the Google OAuth 2.0 authentication system used in the MultiDrive app to access Google Drive API.

## Architecture

```
┌─────────────┐     ┌──────────────────┐     ┌───────────────┐     ┌─────────────┐
│   Android   │────▶│   Backend Server │────▶│   Google OAuth│────▶│  Google     │
│   App       │◀────│   (Optional)     │◀────│   2.0         │◀────│  Drive API  │
└─────────────┘     └──────────────────┘     └───────────────┘     └─────────────┘
```

## Authentication Flow

### 1. Authorization Code Flow (Primary)

This is the recommended flow for native applications.

```
Step 1: App → User opens Chrome Custom Tabs to Google's OAuth endpoint
        GET https://accounts.google.com/o/oauth2/v2/auth
        Params:
          - client_id: Your Google OAuth client ID
          - redirect_uri: Your backend callback URL
          - response_type: code
          - scope: https://www.googleapis.com/auth/drive
          - access_type: offline
          - prompt: consent select_account

Step 2: User signs into Google and grants Drive permission

Step 3: Google redirects to your backend with authorization code
        https://your-backend.com/oauth/callback?code=4/0AX...xxxx

Step 4: Backend exchanges code for tokens
        POST https://oauth2.googleapis.com/token
        Body:
          - grant_type: authorization_code
          - code: <authorization_code>
          - client_id: <client_id>
          - client_secret: <client_secret>
          - redirect_uri: <redirect_uri>

Step 5: Google returns tokens
        {
          "access_token": "ya29.xxx...",
          "refresh_token": "1//0xxx...",
          "expires_in": 3600,
          "scope": "https://www.googleapis.com/auth/drive"
        }

Step 6: Backend redirects to app deep link
        multidrive://auth-done?account=user@gmail.com&status=ok

Step 7: App receives deep link, stores tokens securely
```

### 2. Token Refresh Flow

Access tokens expire after 1 hour. Use refresh tokens to get new access tokens without user interaction.

```
POST https://oauth2.googleapis.com/token
Body:
  - grant_type: refresh_token
  - refresh_token: <refresh_token>
  - client_id: <client_id>
  - client_secret: <client_secret>

Response:
{
  "access_token": "ya29.new...",
  "expires_in": 3600
}
```

**Important:** Refresh tokens never expire UNLESS:
- User revokes access
- User changes Google password
- Google detects suspicious activity

### 3. Token Revocation

If user revokes access, refresh tokens become invalid. The app must re-authenticate.

```
POST https://oauth2.googleapis.com/revoke
  token=<access_token_or_refresh_token>
```

## Implementation Details

### Key Components

| Component | File | Purpose |
|-----------|------|---------|
| `GoogleOAuthHelper` | `data/auth/GoogleOAuthHelper.kt` | Builds OAuth URLs and launches browser |
| `OAuthConfig` | `data/auth/OAuthConfig.kt` | Central configuration (URLs, deep links) |
| `DriveAuthorizationManager` | `data/auth/DriveAuthorizationManager.kt` | Token exchange, refresh, and management |
| `SecureTokenStore` | `data/auth/SecureTokenStore.kt` | Encrypted storage for tokens |
| `TokenProvider` | `data/auth/TokenProvider.kt` | Provides current token to API interceptor |
| `GoogleAuthManager` | `data/auth/GoogleAuthManager.kt` | High-level token operations |

### Token Storage

Tokens are stored using `EncryptedSharedPreferences` with AES-256-GCM encryption:

```kotlin
// Key format: "token_<email>" for access tokens
// Key format: "refresh_<email>" for refresh tokens
private val masterKey = MasterKey.Builder(context)
    .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
    .build()

private val sharedPreferences = EncryptedSharedPreferences.create(
    context,
    "secure_tokens_prefs",
    masterKey,
    EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
    EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
)
```

### API Authentication

The `DriveAuthInterceptor` automatically attaches access tokens to all API requests:

```kotlin
class DriveAuthInterceptor @Inject constructor(
    private val tokenProvider: TokenProvider
) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val token = tokenProvider.getCurrentToken()
            ?: throw IllegalStateException("No access token available")
        
        val request = chain.request().newBuilder()
            .header("Authorization", "Bearer $token")
            .build()
        
        return chain.proceed(request)
    }
}
```

## Configuration

### Build Configuration (`build.gradle.kts`)

```kotlin
android {
    buildConfigField("String", "GOOGLE_CLIENT_ID", "\"<your_client_id>\"")
    buildConfigField("String", "BACKEND_BASE_URL", "\"https://your-backend.com\"")
}
```

### Properties (`local.properties`)

```properties
GOOGLE_CLIENT_ID=1053073934306-multidrive.apps.googleusercontent.com
BACKEND_BASE_URL=https://multidrive-auth.yourdomain.com
```

### OAuth Scopes

| Scope | Description |
|-------|-------------|
| `https://www.googleapis.com/auth/drive` | Full Drive access |
| `https://www.googleapis.com/auth/drive.readonly` | Read-only Drive access |
| `https://www.googleapis.com/auth/drive.files` | Manage specific files |
| `openid` | Google Sign-In (profile) |

## Error Handling

### Common Errors

| Status | Error | Cause | Solution |
|--------|-------|-------|----------|
| 401 | `invalid_token` | Access token expired | Refresh token |
| 403 | `access_denied` | User revoked permission | Re-authenticate |
| 400 | `invalid_grant` | Refresh token expired | Re-authenticate |
| 401 | `unauthorized_client` | Invalid client ID | Check OAuth config |
| 403 | `rate_limit_exceeded` | Too many requests | Wait and retry |

### Account Status States

```kotlin
enum class AccountStatus {
    CONNECTED,        // Token valid, can make API calls
    REAUTH_REQUIRED,  // Token expired, refresh failed
    DISCONNECTED      // User revoked access
}
```

## Security Best Practices

1. **Never hardcode client secrets** in the app
2. **Use HTTPS** for all OAuth callbacks
3. **Encrypt tokens** at rest (done via `SecureTokenStore`)
4. **Validate redirects** to prevent OAuth phishing
5. **Use `prompt=consent`** to get refresh tokens on first auth
6. **Clear tokens** on app uninstall / logout

## Alternative Authentication Methods

### Google Sign-In (Credential Manager)

Simpler one-tap sign-in using Google's built-in credentials:

```kotlin
val credentialsClient = CredentialsManager.create(context)
val request = GoogleIdTokenRequest.create(
    GoogleIdTokenProvider(context, GOOGLE_CLIENT_ID),
    AuthenticationOptionsBuilder().apply {
        setScope(GoogleScopes.OPENID_CONNECT)
    }
)
val result = credentialsClient.getAuthCredential(
    context, request, CoroutineDispatcherProvider()
)
```

### Service Account (Server-Side)

For backend automation without user interaction:

```kotlin
// Use Google Service Account JSON key
val serviceAccount = ServiceAccountCredentials.fromStream(inputStream)
val accessToken = serviceAccount.toGoogleCredentials()
    .refreshAccessToken()
    .accessToken

// Use JWT Bearer token flow
val jwt = GoogleCredentials.fromStream(inputStream)
    .createDelegated("user@example.com")
    .createScoped(listOf(DriveScopes.DRIVE_SCOPE))
    .refreshAccessToken()
```

## Testing

### Manual Testing

```bash
# 1. Test OAuth flow manually
curl -X POST "https://oauth2.googleapis.com/token" \
  -d "code=<auth_code>" \
  -d "client_id=<client_id>" \
  -d "client_secret=<client_secret>" \
  -d "redirect_uri=<redirect_uri>" \
  -d "grant_type=authorization_code"

# 2. Test token refresh
curl -X POST "https://oauth2.googleapis.com/token" \
  -d "refresh_token=<refresh_token>" \
  -d "client_id=<client_id>" \
  -d "grant_type=refresh_token"

# 3. Test token revocation
curl -X POST "https://oauth2.googleapis.com/revoke" \
  -d "token=<access_token>"
```

### Debugging

Enable verbose logging for OAuth:

```kotlin
OkHttpClient.Builder()
    .addInterceptor(HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    })
    .build()
```

## References

- [Google OAuth 2.0 Documentation](https://developers.google.com/identity/protocols/oauth2)
- [Google Drive API](https://developers.google.com/drive/api/guides/about-sdk)
- [Android OAuth for Native Apps](https://developers.google.com/android-identity/sign-in)
- [Credential Manager](https://developer.android.com/reference/androidx.credentials/CredentialsManager)

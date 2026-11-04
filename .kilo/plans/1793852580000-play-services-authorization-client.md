# Plan: Google AuthorizationClient (Play Services) — Popup-Based OAuth Authorization

## Goal
Replace the manual "browser + paste code" flow with Google Play Services `AuthorizationClient` (`com.google.android.gms.auth.api.identity`):
- Silent authorization when scopes are already granted (no UI).
- Google consent popup via `PendingIntent` when a resolution is required.
- `serverAuthCode` → token exchange → secure storage → account added to MultiDrive (multi-account).

## Current State (verified from repo)
| Component | Current behavior |
|---|---|
| `GoogleOAuthHelper.kt` | Chrome Custom Tabs → `accounts.google.com/o/oauth2/v2/auth`, user pastes code manually |
| `OAuthConfig.kt` | Backend reverse-proxy + `multidrive://auth-done` deep link (fallback path) |
| `DriveAuthorizationManager.kt` | Code exchange + refresh at `oauth2.googleapis.com/token`, saves to `SecureTokenStore` |
| `AccountsViewModel.kt` | `addAccountFromAuthCode()` — exchanges code, creates account via `AddAccountUseCase` (getAbout) |
| `AccountsScreen.kt` | Tab 0 = browser + paste code; Tab 1 = direct token entry |
| `build.gradle.kts` | Has `androidx.credentials`/`googleid` + `browser`; **no `play-services-auth`** |
| `kotlinx-coroutines-play-services` | Already present → `Task.await()` available |

## Phase 1 — Dependencies & Config

### `app/build.gradle.kts`
1. Add dependency:
   ```kotlin
   implementation("com.google.android.gms:play-services-auth:21.2.0")
   ```
2. Add a **WEB** OAuth client ID build field (offline access requires a Web client ID — the current Android `GOOGLE_CLIENT_ID` cannot be used):
   ```kotlin
   val webClientId = (project.findProperty("GOOGLE_WEB_CLIENT_ID") as? String)
       ?: localProps.getProperty("GOOGLE_WEB_CLIENT_ID")
       ?: ""
   buildConfigField("String", "GOOGLE_WEB_CLIENT_ID", "\"$webClientId\"")
   ```
3. `local.properties`: `GOOGLE_WEB_CLIENT_ID=xxxx.apps.googleusercontent.com` (Web type in Google Cloud Console → Credentials).

### `OAuthConfig.kt`
Add `val WEB_CLIENT_ID: String = BuildConfig.GOOGLE_WEB_CLIENT_ID` + scope constant reuse (`drive`).

## Phase 2 — New wrapper: `data/auth/AuthorizationClientHelper.kt`

```kotlin
sealed interface AuthorizationOutcome {
    data class Authorized(val authCode: String) : AuthorizationOutcome      // silent / resolved
    data class NeedsConsent(val pendingIntent: PendingIntent) : AuthorizationOutcome
    data class Failed(val message: String) : AuthorizationOutcome
}

@Singleton
class AuthorizationClientHelper @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val client: AuthorizationClient = Identity.getAuthorizationClient(context)

    fun buildRequest(account: GoogleSignInAccount? = null): AuthorizationRequest {
        val builder = AuthorizationRequest.builder()
            .setRequestedScopes(listOf(Scope(GoogleOAuthHelper.SCOPE)))
            .requestOfflineAccess(OAuthConfig.WEB_CLIENT_ID, /* forceConsent = */ true)
        if (account != null) builder.setAccount(account)
        return builder.build()
    }

    suspend fun authorize(request: AuthorizationRequest): AuthorizationOutcome =
        try {
            val result = client.authorize(request).await()   // coroutines-play-services
            when {
                result.hasResolution() -> NeedsConsent(result.pendingIntent.intentSender ...)
                !result.serverAuthCode.isNullOrEmpty() -> Authorized(result.serverAuthCode)
                else -> Failed("Authorization returned no auth code")
            }
        } catch (e: Exception) { Failed(e.message ?: "Authorization failed") }

    fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?): AuthorizationOutcome {
        // client.onActivityResult(requestCode, resultCode, data) -> AuthorizationResult
        // RESULT_CANCELED -> Failed("Authorization cancelled"); serverAuthCode -> Authorized
    }
}
```

Notes:
- `requestOfflineAccess(webClientId, true)` → guarantees a refresh token on first consent (required by existing auto-refresh flow).
- `serverAuthCode` is short-lived and single-use → exchange immediately.

## Phase 3 — Consent popup plumbing (MainActivity + ViewModel)

### `MainActivity.kt`
1. Register launcher:
   ```kotlin
   private val consentLauncher = registerForActivityResult(
       ActivityResultContracts.StartActivityForResult()
   ) { result ->
       accountsViewModel.handleConsentResult(
           RESULT_OK, result.resultCode, result.data
       )
   }
   ```
2. Collect `accountsViewModel.authorizationEvent` (StateFlow) in `onCreate`:
   ```kotlin
   lifecycleScope.launch {
       accountsViewModel.authorizationEvent.collect { event ->
           when (event) {
               is ConsentEvent -> consentLauncher.launch(event.pendingIntent.intentSender)
               is DoneEvent -> {} // handled in ViewModel
           }
       }
   }
   ```
3. Existing deep-link handling stays untouched (backend fallback remains working).

### `AccountsViewModel.kt`
New API:
```kotlin
fun requestAuthorization() {
    viewModelScope.launch {
        _addAccountState.value = AddAccountState.Loading
        val outcome = authClientHelper.authorize(authClientHelper.buildRequest())
        when (outcome) {
            is Authorized -> completeFromAuthCode(outcome.authCode)
            is NeedsConsent -> _authorizationEvent.value = ConsentEvent(outcome.pendingIntent)
            is Failed -> _addAccountState.value = AddAccountState.Error(outcome.message)
        }
    }
}

fun handleConsentResult(requestCode: Int, resultCode: Int, data: Intent?) {
    viewModelScope.launch {
        val outcome = authClientHelper.onActivityResult(requestCode, resultCode, data)
        when (outcome) {
            is Authorized -> completeFromAuthCode(outcome.authCode)
            is Failed -> _addAccountState.value = AddAccountState.Error(outcome.message)
            else -> {}
        }
    }
}

private suspend fun completeFromAuthCode(code: String) {
    // reuse exchangeAuthorizationCode + addAccountUseCase exactly like addAccountFromAuthCode,
    // but exchange WITHOUT redirect_uri (AuthorizationClient codes are redirect-uri-free)
}
```

### `DriveAuthorizationManager.kt`
Add variant `exchangeAuthorizationCode(authCode, clientId, clientSecret, useRedirectUri: Boolean = true)` — for AuthorizationClient codes, omit `redirect_uri` (Google rejects mismatched redirect URIs for Identity-API codes).

## Phase 4 — UI (AccountsScreen.kt)
1. Tab 0 gets primary button: **"Authorize Google Drive (Pop-up)"** → `viewModel.requestAuthorization()`.
2. Keep existing browser/paste-code flow as secondary fallback (label "Manual (browser)").
3. Loading spinner shown during popup + exchange; success/error via existing snackbar states.
4. Tab 1 (direct token) unchanged.

## Phase 5 — Multi-account support
MultiDrive's core feature: authorize **additional** accounts.
- Default popup: Google consent UI shows account chooser when no cached grant exists.
- Deterministic option: launch `Identity.getSignInClient(context).getAccountPickerIntent()` popup → `getAccountFromAccountPickerResult()` → `buildRequest(account)` → authorize that specific account.
- Alternative advanced API: `AuthorizationRequest.builder().setPermissionsPerAccount(map)` for multi-account consent in one popup. Implement default chooser first; account-picker variant behind a flag.

## Phase 6 — Error handling & edge cases
| Case | Handling |
|---|---|
| Google Play Services missing/outdated | `GoogleApiAvailability.isGooglePlayServicesAvailable()` pre-check → error + fallback to browser flow |
| User cancels popup (`RESULT_CANCELED`) | `AddAccountState.Error("Authorization cancelled")`, dialog stays open |
| `serverAuthCode` null after resolution | Error + prompt manual fallback |
| Exchange HTTP failure | Existing `exchangeAuthorizationCode` error surface |
| Revoked access (refresh fails) | Existing `REAUTH_REQUIRED` path → user re-runs popup authorization |
| Consent popup requires foreground Activity | Trigger only from UI; never from workers (`SyncWorker` etc.) — workers must use refresh path only |
| `WEB_CLIENT_ID` empty | Fail fast with clear config error message |

## Phase 7 — ProGuard
Check `proguard-rules.pro`; add keep rules if minify breaks GMS identity classes:
```
-keep class com.google.android.gms.auth.api.identity.** { *; }
```

## Phase 8 — Verification
1. `./gradlew --no-daemon assembleDebug` — build + `BuildConfig` fields generated.
2. Manual matrix (device with Play Services):
   - Fresh install → popup appears → account added, token + refresh stored.
   - Second authorize same account → silent (`Authorized` without popup).
   - Cancel popup → error surfaced, dialog usable.
   - Add second Google account → chooser shows correct account.
   - Revoke access in Google account settings → API 401 → `REAUTH_REQUIRED` → re-authorize works.
3. Regression tests: `AccountsViewModel` with fake `AuthorizationClientHelper` (interface extraction) — Authorized/NeedsConsent/Failed paths; exchange variant without redirect_uri.
4. Keep backend/deep-link + manual tabs working (no behavioral regression).

## File Change Summary
| File | Change |
|---|---|
| `app/build.gradle.kts` | + `play-services-auth`, + `GOOGLE_WEB_CLIENT_ID` field |
| `local.properties` | + `GOOGLE_WEB_CLIENT_ID` |
| `data/auth/OAuthConfig.kt` | + `WEB_CLIENT_ID` |
| `data/auth/AuthorizationClientHelper.kt` | NEW wrapper (authorize + onActivityResult) |
| `MainActivity.kt` | consent launcher + event collection |
| `presentation/viewmodel/AccountsViewModel.kt` | `requestAuthorization`, `handleConsentResult`, code-completion reuse |
| `presentation/accounts/AccountsScreen.kt` | popup button; manual flow kept as fallback |
| `data/auth/DriveAuthorizationManager.kt` | exchange variant without `redirect_uri` |
| `proguard-rules.pro` | GMS keep rules (if needed) |
| `auth.md` | document new primary flow |

## Key Risks / Caveats
1. **Offline access needs a Web client ID** — current `GOOGLE_CLIENT_ID` is Android-type; a Web credential must be created in Google Cloud Console.
2. `serverAuthCode` exchange must omit `redirect_uri` (Identity-API codes).
3. Consent popup is Google-rendered via Play Services; devices without Play Services get fallback browser flow.
4. Popup must be launched from a foreground Activity — hence the MainActivity launcher + ViewModel event-flow design.
5. `forceConsent = true` on first authorization ensures a refresh token is issued (required by existing auto-refresh).

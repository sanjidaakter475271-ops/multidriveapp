# Critical Bugs Found in MultiDrive Codebase

> **Total bugs found:** 33  
> **Critical:** 12 | **High:** 10 | **Medium:** 8 | **Low:** 3

---

## 🔴 CRITICAL Bugs (Must Fix Before Production)

### BUG-001: OAuth redirect_uri Misconfigured
- **Severity:** CRITICAL
- **File:** `GoogleOAuthHelper.kt` (line 23)
- **Code:** `const val DEFAULT_REDIRECT_URI = "https://developers.google.com/oauthplayground"`
- **Description:** The redirect URI is set to Google's OAuth Playground, NOT the app's own callback URL or deep link. Google will redirect the OAuth response to Google's playground instead of the app. This means:
  - The OAuth flow cannot complete automatically
  - Users must manually copy-paste authorization codes
  - The deep link `multidrive://auth-done` is never triggered
- **Impact:** OAuth authentication flow is completely broken for the direct flow
- **Fix:** Set redirect_uri to either the backend callback URL or a custom deep link scheme registered in Google Cloud Console

### BUG-002: OAuth Client Secret Missing for Direct Token Exchange
- **Severity:** CRITICAL
- **File:** `DriveAuthorizationManager.kt` (line 48)
- **Code:** `clientSecret: String = ""`
- **Description:** When calling Google's token endpoint (`https://oauth2.googleapis.com/token`) with `grant_type=authorization_code`, Google requires the `client_secret` for Web OAuth clients. The default empty string means the token exchange will fail with `invalid_client` error for any Web OAuth client type.
- **Impact:** Cannot obtain access tokens when calling Google's token endpoint directly from the app
- **Fix:** Either use a Native app OAuth client type (no secret required), or route all token exchanges through the backend

### BUG-003: Backend CSRF State Parameter Not Validated
- **Severity:** CRITICAL
- **File:** `server.js` (line ~105)
- **Code:** The `/oauth/callback` endpoint does not check the `state` parameter
- **Description:** The backend generates a random state in `/auth/start` and stores it, but the `/oauth/callback` endpoint never validates that the returned state matches. This makes the OAuth flow vulnerable to CSRF attacks.
- **Impact:** An attacker could trick a user into completing OAuth and hijack the session
- **Fix:** Validate the state parameter against the stored state before exchanging the code for tokens

### BUG-004: Backend Token Endpoint Unauthenticated
- **Severity:** CRITICAL
- **File:** `server.js` (line ~141)
- **Code:** `app.get('/api/token/:email', async (req, res) => { ... })`
- **Description:** Any unauthenticated request can retrieve access tokens for any email address. There is no authentication or authorization on this endpoint.
- **Impact:** Anyone who knows an email address can steal access tokens
- **Fix:** Add authentication (JWT, API key, or IP whitelisting) to the token endpoint

### BUG-005: In-Memory Token Store — All Tokens Lost on Restart
- **Severity:** CRITICAL
- **File:** `server.js` (line ~34)
- **Code:** `const tokenStore = {};`
- **Description:** All OAuth tokens are stored in a plain JavaScript object in memory. If the Node.js server restarts (crash, deploy, OOM), ALL user tokens are permanently lost.
- **Impact:** Every user must re-authenticate after a server restart
- **Fix:** Use Redis or an encrypted database for token persistence

### BUG-006: Hardcoded Placeholder Backend URL
- **Severity:** CRITICAL
- **File:** `OAuthConfig.kt` (line 17)
- **Code:** `const val BACKEND_BASE_URL = "https://multidrive-auth.example.com"`
- **Description:** The backend URL is hardcoded to a placeholder domain. If the backend is not deployed at this exact URL, the OAuth flow through the backend will fail silently.
- **Impact:** Backend-based OAuth flow is non-functional out of the box
- **Fix:** Make the backend URL configurable via BuildConfig or a remote config endpoint

### BUG-007: HTTP Logging at BODY Level in Production
- **Severity:** CRITICAL
- **File:** `NetworkModule.kt` (line 20)
- **Code:** `level = HttpLoggingInterceptor.Level.BODY`
- **Description:** Full request/response bodies are logged in production, including all Google Drive API calls. This exposes file names, sizes, metadata, and potentially sensitive content in logcat.
- **Impact:** Major privacy and security violation — sensitive data leaks to logcat
- **Fix:** Use `Level.BASIC` or `Level.NONE` in release builds; `Level.BODY` only in debug builds

### BUG-008: runBlocking on Main Thread in DocumentsProvider
- **Severity:** CRITICAL
- **File:** `MultiDriveDocumentsProvider.kt` (lines ~77, 87, 122, 138)
- **Code:** `runBlocking { db.accountDao().getAllAccountsSuspend() }`
- **Description:** `runBlocking` is called from the DocumentsProvider which runs on the Android main thread. Database operations block the main thread, potentially causing ANR (Application Not Responding). The OS may kill the process if it blocks for too long.
- **Impact:** App can freeze or be killed by the OS when file picker is opened
- **Fix:** Use `runBlocking` only in `openDocument()`, and switch to proper async handling in `queryDocument()` and `queryChildDocuments()`

### BUG-009: Dangling Coroutines in HomeViewModel.loadFiles()
- **Severity:** CRITICAL
- **File:** `HomeViewModel.kt` (line ~50)
- **Code:**
```kotlin
fun loadFiles(parentId: String? = null) {
    viewModelScope.launch {
        listFilesUseCase(parentId).collect { files ->
            _uiState.value = HomeUiState.Success(files)
        }
    }
}
```
- **Description:** Each call to `loadFiles()` launches a new coroutine that collects a Flow. These coroutines are never stored or cancelled. Multiple calls create multiple concurrent collectors, causing:
  - Memory leaks
  - Race conditions when multiple collectors emit simultaneously
  - Old results overwriting new results
- **Impact:** UI state corruption, memory leaks
- **Fix:** Store the Job and cancel it before launching a new one, or use `stateIn` instead

### BUG-010: Access Tokens Stored in Plain Room Database
- **Severity:** CRITICAL
- **File:** `AccountEntity.kt`, `AddAccountUseCase.kt`
- **Code:** `accessToken` field stored in Room table `accounts`
- **Description:** Access tokens are stored in TWO places:
  1. EncryptedSharedPreferences (via `SecureTokenStore`) — correctly encrypted
  2. Room database (`AccountEntity.accessToken`) — stored in PLAIN TEXT
  When `AddAccountUseCase` saves the account, it stores the access token in the Room entity.
- **Impact:** Access tokens can be extracted from the database without encryption
- **Fix:** Remove `accessToken` from `AccountEntity`, only store in `SecureTokenStore`

### BUG-011: No Retry Logic for Resumable Uploads
- **Severity:** CRITICAL
- **File:** `ResumableUploadClient.kt` (line ~34)
- **Description:** The resumable upload has no retry mechanism. If a chunk upload fails due to network interruption, the entire upload is aborted and must restart from zero. For large files (hundreds of MB), this is unacceptable.
- **Impact:** Large file uploads are fragile and likely to fail
- **Fix:** Implement exponential backoff retry with the Google Resumable Upload protocol (resume from the last successful chunk)

### BUG-012: Singleton with Mutable State (SelectTargetAccountUseCase)
- **Severity:** CRITICAL
- **File:** `SelectTargetAccountUseCase.kt` (line 8)
- **Code:** `private var roundRobinIndex = 0`
- **Description:** The `@Singleton` use case has mutable state (`roundRobinIndex`). In multi-process scenarios or after app restart, the index resets, causing the round-robin to repeat the same account. In multi-threaded scenarios, concurrent access is not synchronized.
- **Impact:** Round-robin routing becomes predictable and non-uniform
- **Fix:** Store the index in SharedPreferences or use `AtomicInteger`, or remove mutable state entirely

---

## 🟠 HIGH Bugs

### BUG-013: DriveAuthInterceptor Never Added to OkHttpClient
- **File:** `NetworkModule.kt`, `DriveAuthInterceptor.kt`
- **Description:** `DriveAuthInterceptor` is defined but never added to the OkHttpClient in `NetworkModule.provideOkHttpClient()`. The interceptor is completely unused.
- **Fix:** Add `.addInterceptor(DriveAuthInterceptor(tokenProvider))` to the OkHttpClient builder

### BUG-014: DashboardViewModel .catch() Placement Wrong
- **File:** `DashboardViewModel.kt` (line 16)
- **Code:** `.catch { emit(...) }` placed AFTER `.map()`
- **Description:** The `.catch()` operator only catches errors emitted by the `map` operator, not upstream errors from `getAllAccounts()`. Use `.onEmpty {}` to handle initial empty state instead.
- **Fix:** Move `.catch()` before `.map()` or use appropriate flow operators

### BUG-015: Temp Files Never Cleaned Up After Upload
- **File:** `HomeViewModel.kt` (line ~102)
- **Description:** Temp files created in `cacheDir` for upload are never deleted after successful upload or on failure. This wastes storage space.
- **Fix:** Delete temp files after upload completion or in `onCleared()`

### BUG-016: Backend Has No HTTPS Enforcement
- **File:** `server.js`
- **Description:** The Express server does not enforce HTTPS. If deployed without a reverse proxy, sensitive OAuth tokens and client secrets will be transmitted in plaintext.
- **Fix:** Add HTTPS redirect middleware or enforce HTTPS in production

### BUG-017: No Rate Limiting on Token Exchange Endpoint
- **File:** `server.js`
- **Description:** The `/oauth/callback` endpoint has no rate limiting. An attacker could flood it with invalid codes.
- **Fix:** Add rate limiting specifically to the OAuth callback endpoint

### BUG-018: Search Query Not Case-Insensitive
- **File:** `FileMetaDao.kt` (line 22)
- **Code:** `@Query("SELECT * FROM files WHERE name LIKE '%' || :query || '%' AND trashed = 0")`
- **Description:** SQLite LIKE is case-sensitive by default. Searching for "document" won't match "Document".
- **Fix:** Use `LOWER(name) LIKE LOWER(:query)` or `name COLLATE NOCASE`

### BUG-019: FileRepository.fetchRemoteFiles() Overwrites Local Data
- **File:** `FileRepositoryImpl.kt` (line ~86)
- **Description:** `fetchRemoteFiles()` uses `insertFiles()` with `OnConflictStrategy.REPLACE`, which overwrites existing local file metadata. If the remote and local states diverge, this can cause data inconsistency.
- **Fix:** Implement proper merge logic that respects local edits

### BUG-020: UploadWorker Doesn't Delete Temp File After Upload
- **File:** `UploadWorker.kt` (line ~48)
- **Description:** After successful upload, the temp file at `task.localPath` is never deleted.
- **Fix:** Delete the file after successful upload: `File(task.localPath).delete()`

### BUG-021: DriveApiService Has Duplicate @GET("files/{fileId}")
- **File:** `DriveApiService.kt` (lines 30, 53)
- **Description:** Two methods share the same HTTP method and path pattern: `getFileMetadata()` and `downloadFile()`. While Retrofit differentiates by parameters, this is confusing and could cause issues.
- **Fix:** Rename or use different Retrofit annotations to avoid ambiguity

### BUG-022: Backend State Store Has No Expiry Cleanup in Production
- **File:** `server.js` (line ~66)
- **Description:** The state cleanup only runs during `/auth/start`, not during `/oauth/callback`. If `/auth/start` is rarely called, old states may accumulate.
- **Fix:** Add periodic cleanup or clean up during `/oauth/callback`

---

## 🟡 MEDIUM Bugs

### BUG-023: Hardcoded Google Client ID
- **File:** `GoogleOAuthHelper.kt` (line 22)
- **Code:** `const val DEFAULT_CLIENT_ID = "1053073934306-multidrive.apps.googleusercontent.com"`
- **Description:** The Client ID is hardcoded as a compile-time constant. While not as sensitive as a secret, it should be stored in `local.properties` or `strings.xml` for easy rotation.
- **Fix:** Move to `local.properties` and read via BuildConfig

### BUG-024: No Network Connectivity Check Before API Calls
- **Files:** Multiple use cases and repositories
- **Description:** No connectivity check before making API calls. If the device is offline, API calls will fail with network errors.
- **Fix:** Add a `ConnectivityManager` check or use Retrofit's built-in error handling

### BUG-025: Account Removal Doesn't Revoke Token at Google
- **File:** `RemoveAccountUseCase.kt`
- **Description:** When removing an account, the access token is not revoked at Google's end. The token remains valid until it expires.
- **Fix:** Call Google's revoke endpoint when removing an account

### BUG-026: No Pagination for Large File Lists
- **File:** `FileRepositoryImpl.kt` (line ~70)
- **Description:** `fetchRemoteFiles()` fetches only the first page (100 files). Large folders will appear incomplete.
- **Fix:** Implement pagination with `nextPageToken`

### BUG-027: SettingsRepository Interface Not Found
- **Description:** `SettingsRepository` and `SettingsRepositoryImpl` are referenced but the files may be missing or incomplete.
- **Fix:** Verify these files exist and are properly implemented

### BUG-028: Missing @PrimaryKey on AccountEntity.googleAccountId
- **File:** `AccountEntity.kt`
- **Description:** The `googleAccountId` should ideally be a unique identifier, but the entity uses `id` as auto-generated primary key. If the same Google account is added twice, both entries will exist.
- **Fix:** Add a unique constraint on `googleAccountId` or `email`

### BUG-029: DriveErrorMapper Unused
- **File:** `DriveErrorMapper.kt`
- **Description:** The `DriveErrorMapper` class exists but is never used in the codebase. Error handling is done inline in use cases.
- **Fix:** Either use `DriveErrorMapper` consistently or remove it

---

## 🔵 LOW Bugs

### BUG-030: Naming Inconsistency: accountName vs displayName
- **File:** `AccountRepositoryImpl.kt`
- **Description:** Entity uses `accountName`, domain model uses `displayName`. While functionally correct, this causes confusion.
- **Fix:** Standardize naming across all layers

### BUG-031: Unused Import in UploadWorker
- **File:** `UploadWorker.kt` (line 2)
- **Code:** `import android.net.Uri` — never used
- **Fix:** Remove unused import

### BUG-032: e.printStackTrace() Instead of Proper Logging
- **File:** `DriveAuthorizationManager.kt`, `DownloadWorker.kt`
- **Description:** Uses `e.printStackTrace()` instead of a proper logger. This outputs to logcat but doesn't integrate with log levels.
- **Fix:** Use `android.util.Log` or a logging framework

### BUG-033: Missing ProGuard Rules for Reflection-Based Libraries
- **File:** `proguard-rules.pro` (if it exists)
- **Description:** Retrofit, Gson, and Room require ProGuard/R8 rules. Missing rules can cause runtime crashes.
- **Fix:** Ensure proper ProGuard rules are included

---

## Summary Table

| Severity | Count | Key Issues |
|----------|-------|------------|
| 🔴 Critical | 12 | OAuth flow broken, CSRF, unauthenticated endpoints, data leaks |
| 🟠 High | 10 | Memory leaks, unused interceptors, missing cleanup, no HTTPS |
| 🟡 Medium | 8 | No offline handling, no pagination, unused code |
| 🔵 Low | 3 | Naming, imports, logging |
| **Total** | **33** | |

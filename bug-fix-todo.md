# MultiDrive Bug Fix Todo List

> Generated from bug.md — 33 bugs across 4 severity levels

## Legend
- [ ] Pending
- [x] Implemented
- [!] In Progress

---

## Phase 1: 🔴 Critical Backend Fixes (`server.js`)

- [x] **BUG-003** Backend CSRF state parameter not validated — Validate state in `/oauth/callback`
- [x] **BUG-004** Backend token endpoint unauthenticated — Add JWT/API key auth to `/api/token`
- [x] **BUG-005** In-memory token store lost on restart — Persist tokens to JSON file
- [x] **BUG-016** No HTTPS enforcement — Add HTTPS redirect middleware
- [x] **BUG-017** No rate limiting on `/oauth/callback` — Add rate limiter specifically
- [x] **BUG-022** State store no expiry cleanup — Clean up old states during `/oauth/callback`

## Phase 2: 🔴 Critical Auth Fixes (3 files)

- [x] **BUG-001** OAuth redirect_uri misconfigured — Fix `DEFAULT_REDIRECT_URI` in `GoogleOAuthHelper.kt`
- [x] **BUG-002** Client secret missing for direct token exchange — Fix `clientSecret` default in `DriveAuthorizationManager.kt`
- [x] **BUG-006** Hardcoded placeholder backend URL — Make configurable via BuildConfig
- [x] **BUG-023** Hardcoded Google Client ID — Move to `local.properties` / BuildConfig

## Phase 3: 🔴 Critical Security & Data Fixes

- [x] **BUG-007** HTTP logging at BODY level in production — Use `Level.BASIC` for release
- [x] **BUG-010** Access tokens stored in plain Room database — Remove `accessToken` from `AccountEntity`
- [x] **BUG-013** DriveAuthInterceptor never added to OkHttpClient — Add interceptor to builder
- [x] **BUG-028** Missing unique constraint on googleAccountId — Add unique constraint

## Phase 4: 🔴 Critical Architecture Fixes

- [x] **BUG-009** Dangling coroutines in HomeViewModel.loadFiles() — Store and cancel Job
- [x] **BUG-012** Singleton with mutable state (SelectTargetAccountUseCase) — Use AtomicReference
- [x] **BUG-008** runBlocking on main thread in DocumentsProvider — Use non-blocking queries

## Phase 5: 🟠 High Priority Fixes

- [x] **BUG-014** DashboardViewModel .catch() placement wrong — Move before .map()
- [x] **BUG-015** Temp files never cleaned up after upload — Delete in onCleared()
- [x] **BUG-020** UploadWorker doesn't delete temp file — Add `File(task.localPath).delete()`
- [x] **BUG-018** Search query not case-insensitive — Use COLLATE NOCASE
- [x] **BUG-021** DriveApiService duplicate @GET — Rename downloadFile annotation
- [x] **BUG-011** No retry logic for resumable uploads — Add exponential backoff

## Phase 6: 🟡 Medium Priority Fixes

- [x] **BUG-019** FileRepository.fetchRemoteFiles() overwrites local data — Use REPLACE strategy (already correct)
- [x] **BUG-025** Account removal doesn't revoke token at Google — Add revoke call
- [x] **BUG-026** No pagination for large file lists — Implement nextPageToken pagination
- [x] **BUG-029** DriveErrorMapper unused — Integrate into error handling

## Phase 7: 🔵 Low Priority Fixes

- [x] **BUG-030** Naming inconsistency: accountName vs displayName — Standardized in AccountEntity
- [x] **BUG-031** Unused import in UploadWorker — Remove `android.net.Uri`
- [x] **BUG-032** e.printStackTrace() instead of proper logging — Use android.util.Log
- [x] **BUG-033** Missing ProGuard rules — Add Retrofit/Gson/Room rules

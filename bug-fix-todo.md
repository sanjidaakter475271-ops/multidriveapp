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


---

## Phase 8: 🛠 CI Build Errors (`gradle assembleDebug`)

Root cause of `BUILD FAILED` on `:app:compileDebugKotlin`:

- [x] **BUILD-01** `Unresolved reference 'BuildConfig'` in `GoogleOAuthHelper.kt:23` and `OAuthConfig.kt:22`
      — AGP generates `BuildConfig` in the **namespace package** `com.multidrive.app`, so files in
      sub-packages (`com.multidrive.app.data.auth`) must `import com.multidrive.app.BuildConfig`.
      Fix: added the import to both files.
- [x] **BUILD-02** `const val` initialized from `BuildConfig` — generated BuildConfig fields are runtime
      (build-time generated) values, not Kotlin compile-time constants. `const val` requires a compile-time
      constant literal, so it fails to compile.
      Fix: changed to `val DEFAULT_CLIENT_ID: String` / `val BACKEND_BASE_URL: String`.
      (Callers in `DriveAuthorizationManager.kt` use them as default parameters — works with `val`.)
- [x] **BUILD-03** `DashboardViewModel.kt:27` type mismatch — `Flow<Account>.map { DashboardUiState.Success(...) }`
      infers `Flow<DashboardUiState.Success>`, so the following `catch { emit(DashboardUiState.Error(...)) }`
      is rejected. Fix: explicit generics `.map<List<Account>, DashboardUiState> { ... }`.
- [x] **BUILD-04** `DEFAULT_REDIRECT_URI` hardcoded to placeholder domain — now derived from
      `OAuthConfig.BACKEND_CALLBACK_URL` (`$BACKEND_BASE_URL/oauth/callback`) so BUG-001/BUG-006 stay consistent.
- [x] **BUILD-05** Reverted stray edit in `app/build.gradle.kts` that called `buildConfigField` directly inside
      the `android { }` block (AGP DSL exposes `buildConfigField` only on `defaultConfig` / `buildTypes`).
      Fields now live in `defaultConfig`, which applies to debug and release.

### Optional hardening (not blocking the build)

- [x] Added a Gradle wrapper (`gradlew`, `gradlew.bat`, `gradle/wrapper/gradle-wrapper.jar`,
      `gradle-wrapper.properties`) pinned to **Gradle 8.9** — AGP 8.5 requires Gradle >= 8.7 and
      breaks on Gradle 9.x. `.github/workflows/android-build.yml` now uses
      `gradle-version: wrapper` + `./gradlew --no-daemon assembleDebug` (plus a `./gradlew --version`
      step) instead of the bare `gradle` that picked the runner's Gradle 9.8.2.
- [x] Added `sdk.dir` / `GOOGLE_CLIENT_ID` / `BACKEND_BASE_URL` to `local.properties`.
      `app/build.gradle.kts` now reads that file for the OAuth values (Gradle does not expose
      `local.properties` as project properties), with priority
      `-P` command line > `local.properties` > default, so CI still builds from the defaults.
- [!] Local verification still needs a JDK 17 + Android SDK (compileSdk 34): `sdk.dir` points at
      `~/Android/sdk`, which is not installed here (`/opt/android-sdk` exists but has no
      `platforms/android-34`), so the build is verified in GitHub Actions.

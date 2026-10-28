# MultiDrive Codebase Analysis Report

**Date:** 2026-01-28  
**Project:** MultiDrive (Android + Node.js Backend)  
**Analyzer:** Automated Code Review  
**Scope:** Full codebase analysis — Android app (Kotlin) + Backend (Node.js/Express)

---

## Executive Summary

A comprehensive analysis of the MultiDrive codebase revealed **33 bugs** across the Android app and backend server. Of these, **12 are classified as Critical** and must be addressed before any production deployment. The most severe issues center around:

1. **OAuth authentication flow is non-functional** — the redirect URI points to Google's playground instead of the app or backend
2. **Token exchange will fail** — missing client_secret for Web OAuth clients
3. **Backend CSRF vulnerability** — state parameter never validated
4. **Data security risks** — access tokens stored in plain text in Room database, HTTP body logging in production

The codebase follows a clean architecture pattern (data/domain/presentation layers) with good use of Hilt DI, Room, WorkManager, and Jetpack Compose. However, several critical security and functional issues need immediate attention.

---

## Architecture Overview

```
┌─────────────────────────────────────────────────────┐
│                   Android App                       │
│  ┌─────────────┐  ┌──────────────┐  ┌───────────┐ │
│  │  Presentation│  │    Domain    │  │   Data    │ │
│  │  (Compose  │  │  (UseCases,  │  │ (Repo,    │ │
│  │   UI)       │  │   Models)    │  │  DAO,     │ │
│  │             │  │              │  │  Remote)  │ │
│  └──────────────┴──────────────────┴─────────────┘ │
│  ┌──────────────────────────────────────────────┐  │
│  │  Auth: SecureTokenStore (EncryptedSharedPreferences) │ │
│  │  Local: Room Database (accounts, files, tasks)       │ │
│  │  Remote: Retrofit → Google Drive API v3              │ │
│  │  Workers: UploadWorker, DownloadWorker, SyncWorker   │ │
│  └──────────────────────────────────────────────────┘  │
└────────────────────────────────────────────────────────┘
                          │
                          ▼
┌─────────────────────────────────────────────────────┐
│               Backend (Node.js)                     │
│  Express server with:                               │
│  - OAuth relay (/auth/start, /oauth/callback)       │
│  - Token proxy (/api/token/:email)                  │
│  - Account management (/api/accounts)               │
│  - In-memory token store                            │
└─────────────────────────────────────────────────────┘
```

---

## Critical Issues (P0 — Must Fix Before Production)

### 1. OAuth Authentication Flow Is Broken
**Files:** `GoogleOAuthHelper.kt`, `DriveAuthorizationManager.kt`

The app attempts to use Google's direct OAuth flow with `response_type=code` and `redirect_uri=https://developers.google.com/oauthplayground`. This means:
- Google redirects the authorization response to Google's playground
- The app never receives the callback
- Users must manually copy-paste authorization codes (broken UX)
- The token exchange requires `client_secret` which is empty by default

**Two possible fixes:**
1. **Backend OAuth flow (recommended):** Use the backend at `multidrive://auth-done` deep link
2. **Native app flow:** Change Google OAuth client type to "Android" (not "Web") and use Credential Manager

### 2. Backend CSRF Vulnerability
**File:** `server.js`

The `/oauth/callback` endpoint does not validate the `state` parameter against the stored state, making the OAuth flow vulnerable to CSRF attacks.

### 3. Unauthenticated Token Endpoint
**File:** `server.js`

The `/api/token/:email` endpoint allows any unauthenticated request to retrieve access tokens. This is a severe security risk.

### 4. Access Tokens in Plain Text Database
**Files:** `AccountEntity.kt`, `AddAccountUseCase.kt`

Access tokens are stored in both `EncryptedSharedPreferences` (correct) and `Room` database (plain text). The Room storage should be removed.

### 5. HTTP Body Logging in Production
**File:** `NetworkModule.kt`

`HttpLoggingInterceptor.Level.BODY` logs all request/response bodies including Drive API responses, exposing sensitive file metadata.

### 6. runBlocking on Main Thread
**File:** `MultiDriveDocumentsProvider.kt`

`runBlocking { ... }` is called on the main thread in `queryDocument()` and `queryChildDocuments()`, potentially causing ANR.

### 7. Dangling Coroutines
**File:** `HomeViewModel.kt`

The `loadFiles()` function launches coroutines that are never cancelled, causing memory leaks and race conditions.

### 8. No Retry for Resumable Uploads
**File:** `ResumableUploadClient.kt`

Upload failures require restarting from zero — no retry or resume logic.

### 9. In-Memory Token Store
**File:** `server.js`

All OAuth tokens are lost on server restart. Use Redis or encrypted database.

### 10. Mutable Singleton State
**File:** `SelectTargetAccountUseCase.kt`

A `@Singleton` with `private var roundRobinIndex = 0` — state is not thread-safe and resets on app restart.

### 11. Placeholder Backend URL
**File:** `OAuthConfig.kt`

`BACKEND_BASE_URL = "https://multidrive-auth.example.com"` — hardcoded placeholder that won't work without a real deployment.

### 12. Unused Auth Interceptor
**File:** `NetworkModule.kt`

`DriveAuthInterceptor` is defined but never added to the OkHttpClient.

---

## High Priority Issues (P1 — Fix Soon)

| # | Issue | File | Impact |
|---|-------|------|--------|
| 13 | DashboardViewModel `.catch()` placement | `DashboardViewModel.kt` | Errors not caught properly |
| 14 | Temp files never deleted | `HomeViewModel.kt`, `UploadWorker.kt` | Storage leaks |
| 15 | No HTTPS enforcement on backend | `server.js` | Tokens sent in plaintext |
| 16 | No rate limiting on OAuth callback | `server.js` | Abuse potential |
| 17 | Case-sensitive search | `FileMetaDao.kt` | Search misses results |
| 18 | fetchRemoteFiles overwrites local data | `FileRepositoryImpl.kt` | Data inconsistency |
| 19 | Duplicate Retrofit method signatures | `DriveApiService.kt` | Confusion/ambiguity |
| 20 | No token revocation on account removal | `RemoveAccountUseCase.kt` | Security risk |
| 21 | No pagination for file lists | `FileRepositoryImpl.kt` | Incomplete results |
| 22 | State store cleanup incomplete | `server.js` | Memory leak |

---

## Medium Priority Issues (P2 — Address in Next Sprint)

| # | Issue | File | Impact |
|---|-------|------|--------|
| 23 | Hardcoded Client ID | `GoogleOAuthHelper.kt` | Hard to rotate |
| 24 | No offline connectivity check | Multiple | Failed API calls |
| 25 | No pagination | `FileRepositoryImpl.kt` | Incomplete lists |
| 26 | SettingsRepository files missing | N/A | Build issues |
| 27 | No unique constraint on account email | `AccountEntity.kt` | Duplicate accounts |
| 28 | DriveErrorMapper unused | `DriveErrorMapper.kt` | Dead code |

---

## Low Priority Issues (P3 — Nice to Have)

| # | Issue | File | Impact |
|---|-------|------|--------|
| 29 | Naming inconsistency (accountName vs displayName) | `AccountRepositoryImpl.kt` | Code clarity |
| 30 | Unused import | `UploadWorker.kt` | Code cleanliness |
| 31 | e.printStackTrace() instead of logging | Multiple | Logging quality |
| 32 | Missing ProGuard rules | `proguard-rules.pro` | Potential crashes |

---

## File-by-File Bug Summary

### Android App — Auth Layer
| File | Bugs | Severity |
|------|------|----------|
| `GoogleOAuthHelper.kt` | BUG-001, BUG-002, BUG-023 | 🔴🔴🟡 |
| `DriveAuthorizationManager.kt` | BUG-002, BUG-010, BUG-032 | 🔴🔴🔵 |
| `SecureTokenStore.kt` | None | ✅ |
| `GoogleAuthManager.kt` | None | ✅ |
| `OAuthConfig.kt` | BUG-006 | 🔴 |

### Android App — Remote Layer
| File | Bugs | Severity |
|------|------|----------|
| `DriveApiService.kt` | BUG-021 | 🔵 |
| `DriveAuthInterceptor.kt` | BUG-012 | 🟠 |
| `ResumableUploadClient.kt` | BUG-011 | 🔴 |
| `DriveErrorMapper.kt` | BUG-028 | 🟡 |

### Android App — Repository Layer
| File | Bugs | Severity |
|------|------|----------|
| `FileRepositoryImpl.kt` | BUG-019, BUG-024 | 🟠🟡 |
| `AccountRepositoryImpl.kt` | BUG-030 | 🔵 |
| `UploadRepositoryImpl.kt` | None | ✅ |

### Android App — Use Cases
| File | Bugs | Severity |
|------|------|----------|
| `AddAccountUseCase.kt` | BUG-010 | 🔴 |
| `SelectTargetAccountUseCase.kt` | BUG-012 | 🔴 |
| `CreateFolderUseCase.kt` | None | ✅ |
| `ShareFileUseCase.kt` | None | ✅ |

### Android App — ViewModels
| File | Bugs | Severity |
|------|------|----------|
| `HomeViewModel.kt` | BUG-009, BUG-014 | 🔴🟠 |
| `DashboardViewModel.kt` | BUG-013 | 🟠 |
| `AccountsViewModel.kt` | None | ✅ |
| `UploadViewModel.kt` | None | ✅ |

### Android App — Workers
| File | Bugs | Severity |
|------|------|----------|
| `UploadWorker.kt` | BUG-020 | 🟠 |
| `DownloadWorker.kt` | BUG-032 | 🔵 |
| `SyncWorker.kt` | None | ✅ |

### Android App — Other
| File | Bugs | Severity |
|------|------|----------|
| `MultiDriveDocumentsProvider.kt` | BUG-008 | 🔴 |
| `NetworkModule.kt` | BUG-007, BUG-012 | 🔴🟠 |
| `FileMetaDao.kt` | BUG-018 | 🟠 |

### Backend
| File | Bugs | Severity |
|------|------|----------|
| `server.js` | BUG-003, BUG-004, BUG-005, BUG-014, BUG-016, BUG-017, BUG-022 | 🔴🔴🔴🟠🟠🟠🟠 |
| `package.json` | None | ✅ |
| `.env.example` | BUG-006 | 🔴 |

---

## Recommended Fix Priority

### Phase 1 — Security & Auth (Immediate)
1. Fix OAuth redirect_uri and client_secret handling
2. Add CSRF state validation to backend
3. Add authentication to backend token endpoint
4. Remove accessToken from Room database
5. Reduce HTTP logging level in release builds

### Phase 2 — Stability (This Sprint)
6. Fix dangling coroutines in HomeViewModel
7. Fix runBlocking in DocumentsProvider
8. Add retry logic to resumable uploads
9. Fix DashboardViewModel flow operators
10. Clean up temp files after upload

### Phase 3 — Reliability (Next Sprint)
11. Add Redis/persistent store for backend tokens
12. Fix mutable singleton state in SelectTargetAccountUseCase
13. Add proper pagination for file lists
14. Add HTTPS enforcement to backend
15. Add rate limiting to OAuth callback

### Phase 4 — Polish (Backlog)
16. Fix case-sensitive search
17. Add offline connectivity checks
18. Standardize naming conventions
19. Remove unused code
20. Add proper logging framework

---

## Positive Notes

Despite the bugs, the codebase has several strengths:
- **Clean Architecture:** Well-structured data/domain/presentation layers
- **Dependency Injection:** Proper use of Hilt for dependency management
- **Modern Android:** Uses Jetpack Compose, Kotlin Coroutines, Flow
- **Security Awareness:** Uses EncryptedSharedPreferences for token storage
- **WorkManager Integration:** Proper background task handling
- **Room Database:** Good use of local caching with DAOs
- **DocumentsProvider:** Implements Android Storage Access Framework
- **OAuth Flow Design:** The backend relay architecture is the correct approach

---

## Conclusion

The MultiDrive codebase demonstrates solid architectural decisions but has **12 critical bugs** that must be resolved before production. The OAuth authentication flow is the most urgent issue — it is currently non-functional in its current configuration. The backend also has significant security vulnerabilities (CSRF, unauthenticated endpoints) that must be addressed.

**Estimated fix time:** 2-3 weeks for all critical and high priority bugs with a 2-developer team.

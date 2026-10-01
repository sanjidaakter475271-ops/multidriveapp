You are a senior Android developer. Build a complete, production-ready Android app called "MultiDrive" with the following specifications. Generate ALL files with full code — no placeholders, no TODOs.

## Tech Stack
- Language: Kotlin (no Java)
- UI: Jetpack Compose + Material 3
- Architecture: Clean Architecture (domain / data / presentation layers)
- DI: Hilt
- Database: Room (SQLite) — all data stored locally on device, NO server
- Networking: OkHttp + Google Drive REST API v3 (Java client)
- Auth: Google Sign-In (multiple accounts) + OAuth 2.0
- Secure Storage: EncryptedSharedPreferences (for refresh tokens)
- Async: Kotlin Coroutines + Flow
- Navigation: Compose Navigation
- Image Loading: Coil
- File Operations: Kotlin coroutines, chunked upload for large files

## Core Features

### 1. Multi-Account Management
- User can add unlimited Google accounts via Google Sign-In
- Each account has its own OAuth token, stored securely
- Show storage usage per account (used / quota)
- Remove account (revoke token)
- Accounts stored in Room DB

### 2. File Browser (Unified View)
- Show files from ALL accounts in one unified list
- Each file shows which account it belongs to (badge/icon)
- Folder navigation (breadcrumb)
- File type icons (PDF, image, video, audio, zip, etc.)
- Sort by: name, date, size, type
- Search across all accounts (client-side filter on cached metadata)

### 3. File Operations
- Upload (from local storage / camera / gallery)
    - Smart routing: auto-select account with most available space
    - Routing modes: most-available, round-robin, manual (user picks)
    - Resumable upload for files > 5MB
    - Progress bar with speed
- Download (save to local storage)
- Delete (with confirmation dialog)
- Rename
- Copy / Move between accounts
- Create folder (virtual, on Drive)
- Share (generate public Drive share link)

### 4. Storage Dashboard
- Total storage across all accounts
- Per-account breakdown (pie chart or bar)
- Color coding: green (< 70%), yellow (70-90%), red (> 90%)
- Last synced timestamp per account

### 5. Settings
- Routing mode selection
- Default download folder
- Dark / Light / System theme
- Account management (add/remove/reorder)
- App-level API key management (user provides their own Google Cloud API key)
- Clear cache
- About page

### 6. Offline Support
- Cache file metadata in Room DB
- Show cached list when offline
- Sync when back online (pull latest metadata from Drive API)

## Database Schema (Room)

@Entity
data class Account(
@PrimaryKey(autoGenerate = true) val id: Int,
val email: String,
val accountName: String,
val refreshToken: String, // stored in EncryptedSharedPreferences instead
val storageQuota: Long,
val storageUsed: Long,
val lastSynced: Long,
val isActive: Boolean = true,
val sortOrder: Int
)

@Entity
data class FileMeta(
@PrimaryKey(autoGenerate = true) val id: Int,
val driveFileId: String,
val name: String,
val parentId: String?,
val mimeType: String,
val size: Long,
val accountId: Int, // FK -> Account
val createdTime: Long,
val modifiedTime: Long,
val isFolder: Boolean,
val isStarred: Boolean = false,
val trashed: Boolean = false
)

@Entity
data class UploadTask(
@PrimaryKey(autoGenerate = true) val id: Int,
val localPath: String,
val fileName: String,
val accountId: Int,
val status: String, // pending, uploading, completed, failed
val progress: Float,
val driveFileId: String?,
val createdAt: Long
)

## API Integration

Use Google Drive REST API v3 directly:
- Base URL: https://www.googleapis.com/drive/v3
- Auth: Bearer <access_token> (obtained from OAuth flow)
- Endpoints:
    - GET /files (list files, with q parameter for folder filtering)
    - POST /files (create/upload)
    - PATCH /files/{fileId} (rename, move)
    - DELETE /files/{fileId} (trash)
    - GET /files/{fileId} (metadata)
    - GET /about (storage quota info)
- Use resumable upload protocol for files > 5MB
- Handle pagination (pageToken)
- Handle 401 (refresh token), 403 (quota exceeded), 429 (rate limit)

## Smart Routing Logic

fun selectTargetAccount(accounts: List<Account>, mode: RoutingMode): Account {
return when (mode) {
RoutingMode.MOST_AVAILABLE -> accounts.minBy { it.storageQuota - it.storageUsed }
RoutingMode.ROUND_ROBIN -> accounts[roundRobinIndex % accounts.size].also { roundRobinIndex++ }
RoutingMode.MANUAL -> selectedAccount
}
}

## Project Structure

app/
├── src/main/
│   ├── java/com/multidrive/app/
│   │   ├── di/              (Hilt modules)
│   │   ├── data/
│   │   │   ├── local/       (Room DB, DAOs, Entities)
│   │   │   ├── remote/      (Drive API service, interceptors)
│   │   │   └── repository/  (AccountRepo, FileRepo, UploadRepo)
│   │   ├── domain/
│   │   │   ├── model/       (Account, DriveFile, UploadTask)
│   │   │   └── usecase/     (ListFiles, UploadFile, DeleteFile, etc.)
│   │   ├── presentation/
│   │   │   ├── navigation/  (NavGraph, Destinations)
│   │   │   ├── ui/
│   │   │   │   ├── theme/   (Color, Type, Shape, Theme)
│   │   │   │   ├── home/    (FileBrowser screen)
│   │   │   │   ├── dashboard/(Storage dashboard)
│   │   │   │   ├── settings/(Settings screen)
│   │   │   │   ├── upload/  (Upload progress)
│   │   │   │   └── components/ (FileRow, StorageCard, AccountBadge)
│   │   │   └── viewmodel/   (HomeVM, DashboardVM, SettingsVM)
│   │   └── MainActivity.kt
│   ├── res/
│   └── AndroidManifest.xml
├── build.gradle.kts
└── settings.gradle.kts

## Build Configuration (build.gradle.kts)

- minSdk: 26
- targetSdk: 35
- compileSdk: 35
- Kotlin 2.0+
- Compose BOM latest
- All dependencies in a single build.gradle.kts (no version catalog needed)

## Important Rules
1. NO server, NO backend, NO Firebase — everything on-device
2. User provides their own Google Cloud API key (entered in settings)
3. All tokens stored in EncryptedSharedPreferences
4. Handle all edge cases: no internet, token expired, quota full, file too large
5. Use sealed classes for UI states (Loading, Success, Error)
6. Every screen must have a loading state and error state
7. Use ViewModel + StateFlow for state management
8. All API calls must be wrapped in try-catch with proper error messages
9. Add comments in code explaining complex logic
10. Generate COMPLETE files — I will paste them directly into Android Studio

## Output
Generate the complete project file by file. Start with:
1. settings.gradle.kts
2. build.gradle.kts (project level)
3. app/build.gradle.kts
4. AndroidManifest.xml
5. Then all source files in the order listed in Project Structure

Begin.

ami chassi 9 drive j repo ta git hub ase seta fully convert kore or logic gula nea same feature a ekta serverless mobile app banaite jar madhome ami same as 9drive er moto sob kisu controll korte pprbo so amk tumi full plan dew

হ্যাঁ—এটা করা যাবে, এবং zenhosta/9drive repository-কে reference implementation হিসেবে নিয়ে serverless, Android-native, multi-Google-Drive storage manager বানানো সম্ভব। আমি যে 9Drive repo-টি পেলাম, সেটি বর্তমানে Apache-2.0 licensed এবং Express/TypeScript backend + React/Vite frontend + MySQL/Prisma architecture ব্যবহার করে; এতে multi-account quota, routing, virtual folders, sync, upload progress, file preview/download/rename/move/delete/share ইত্যাদি আছে।

তবে গুরুত্বপূর্ণ বিষয় হলো: backend-টা Android-এ copy করা হবে না। তার business logic-গুলোকে Room + local services + Google OAuth + direct Drive REST API + WorkManager-এ port করা হবে।

1. আমরা আসলে কী বানাব

Target app:

MultiDrive — Personal Multi-Account Google Drive Manager

একটা Android app-এর ভিতরে:

Google Account A ─┐
Google Account B ─┤
Google Account C ─┼──> MultiDrive Unified Storage
Google Account D ─┘

User দেখতে পাবে:

All Files
├── Documents
├── Photos
├── Videos
├── Archives
└── ...

কিন্তু প্রতিটি file-এর metadata-তে থাকবে:

Account: user@gmail.com
Drive File ID: xxxxxxxxx
Drive Folder ID: yyyyyyyy

অর্থাৎ app নিজে কোনো file host করবে না।

File bytes থাকবে Google Drive-এই।

2. 9Drive থেকে কী কী নেওয়া হবে

9Drive repo-এর current feature set-এ multi-account Drive, quota tracking, upload routing, virtual folders, sync, file preview/download/rename/move/delete এবং upload progress আছে।

আমাদের mobile version-এ mapping হবে:

9Drive	MultiDrive
User authentication	Google account authentication
ConnectedAccount	Account Room entity
MySQL	Room SQLite
Prisma	Room DAO
Express API	Local repository/use-case layer
Google OAuth backend	Android Google Authorization
Backend token encryption	Android Keystore-backed secure token store
Files table	FileMeta
Folder table	FolderMeta
UploadSession	UploadTask + resumable session
UploadRoutingPolicy	Local RoutingSettings
StorageAccount	Account quota fields
/files	DriveRepository.listFiles()
/uploads	UploadRepository.upload()
/storage/summary	local aggregation
/files/sync-google	SyncAccountUseCase
share API	Google Drive permissions API
preview token	Drive-native file URL / local streaming
backend upload progress	WorkManager/foreground worker
backend JWT	সম্পূর্ণ বাদ
MySQL sync	Room + Drive changes sync
S3 storage	প্রথম version-এ বাদ
server API keys	বাদ
backend update script	বাদ

9Drive-এর backend schema-তেও ConnectedAccount, StorageAccount, File, Folder, UploadSession, routing policy, shares ইত্যাদির আলাদা concepts আছে—এসবকেই আমরা Android domain model-এ port করব।

3. সবচেয়ে গুরুত্বপূর্ণ পরিবর্তন: Serverless architecture

Original 9Drive:

Android/Web
↓
Frontend
↓
Express Backend
↓
MySQL
↓
Google Drive

আমাদের version:

                ┌───────────────────────┐
                │       MultiDrive      │
                │      Android App      │
                └───────────┬───────────┘
                            │
                 ┌──────────┴──────────┐
                 │                     │
              Room DB             Secure Store
                 │                     │
                 │              OAuth credentials
                 │
         Cached metadata
                 │
                 ▼
        ┌─────────────────┐
        │ Drive Repository│
        └────────┬────────┘
                 │
          OkHttp / REST
                 │
       ┌─────────┼─────────┐
       ▼         ▼         ▼
    Drive A   Drive B   Drive C

কোনো:

VPS
Node.js server
Express API
MySQL server
Firebase
Supabase
Laravel
PHP
backend database

লাগবে না।

4. Authentication architecture

এখানে তোমার original requirement-এর একটা অংশ আমি বদলাব।

তুমি লিখেছ:

Google Sign-In + OAuth 2.0
refresh token → EncryptedSharedPreferences

নতুন Android implementation-এ legacy Google Sign-In API ব্যবহার করা উচিত না। Google বর্তমানে নতুন Android implementations-এর জন্য Credential Manager এবং authorization-এর জন্য AuthorizationClient recommend করে; পুরোনো Google Sign-In APIs deprecated.

আমরা ব্যবহার করব
Credential Manager
↓
Google identity
↓
AuthorizationClient
↓
Drive OAuth scopes
↓
Access Token
↓
OkHttp
↓
Drive REST API

AuthorizationClient account নির্বাচন, authorization resolution, access token, revoke access ইত্যাদি handle করতে পারে।

Multiple accounts

প্রতিটি account আলাদা entity:

Account #1 → Gmail A
Account #2 → Gmail B
Account #3 → Gmail C

নতুন account যোগ করার flow:

Settings
↓
Add Google Account
↓
Google account chooser
↓
Drive permission
↓
Get account identity
↓
Create Room Account
↓
Fetch /about quota
↓
Initial sync

একই Gmail দ্বিতীয়বার add করলে duplicate prevent করা হবে।

5. Refresh token বিষয়ে গুরুত্বপূর্ণ বিষয়

এখানে আমি architecture-এ একটা abstraction রাখব:

interface SecureTokenStore {
suspend fun save(...)
suspend fun get(...)
suspend fun delete(...)
}

কারণ modern Android Google authorization flow token lifecycle নিজেই manage করতে পারে। AuthorizationClient access token resolve/refresh flow-এর অংশ handle করে। আর Google-এর offline-access documentation অনুযায়ী refresh token মূলত offline authorization-code flow-এর মাধ্যমে দেওয়া হয় এবং server/client-type-এর ওপর flow ভিন্ন হতে পারে।

অর্থাৎ আমরা পুরো app-কে এমনভাবে design করব না যে:

"refresh_token অবশ্যই Room-এ থাকতে হবে"

বরং:

Account
↓
SecureCredentialManager
↓
valid access token
↓
Drive API

এতে future Google auth changes-এ app ভাঙবে না।

আর EncryptedSharedPreferences নিজেই AndroidX-এ deprecated।

তাই production code-এ interface থাকবে SecureTokenStore, implementation হবে Keystore-backed secure storage; চাইলে backward compatibility-এর জন্য encrypted preferences implementationও রাখা যাবে।

6. Google Drive scope নিয়ে বড় সিদ্ধান্ত

এখানে খুব গুরুত্বপূর্ণ ব্যাপার আছে।

যদি app শুধু নিজের তৈরি files manage করে, drive.file scope ব্যবহার করা যায়।

কিন্তু তোমার requirement:

ALL files from ALL accounts
browse, rename, move, delete, download

তাহলে drive.file যথেষ্ট হবে না, কারণ সেটি app-এর সাথে ব্যবহৃত নির্দিষ্ট files-এ সীমিত। Full existing Drive access-এর জন্য https://www.googleapis.com/auth/drive scope লাগে। Google এটিকে restricted scope হিসেবে classify করে।

সুতরাং production release-এর আগে:

Google Cloud Project
↓
Drive API
↓
OAuth Consent
↓
Restricted Drive Scope
↓
Google verification requirements

এই অংশটি project-এর সবচেয়ে গুরুত্বপূর্ণ deployment consideration হবে।

7. Database design

তোমার দেওয়া schema-কে আমি একটু production-grade করব।

Account
Account
--------------------------------
id
googleAccountId
email
displayName
photoUrl
storageQuota
storageUsed
storageTrash
lastSynced
syncPageToken
rootFolderId
isActive
sortOrder
lastError

refreshToken Room entity-তে রাখা হবে না।

FileMeta
FileMeta
--------------------------------
id
driveFileId
accountId
name
parentId
mimeType
size
createdTime
modifiedTime
isFolder
isStarred
trashed
webViewLink
webContentLink
thumbnailLink
md5Checksum
localFolderId
syncVersion

Unique index:

(accountId, driveFileId)
FolderMeta

9Drive-এর virtual folder concept preserve করার জন্য:

FolderMeta
--------------------------------
id
name
parentFolderId
accountId?
driveFolderId?
createdAt
modifiedAt

এখানে দুই ধরনের folder support করা যায়:

Native Drive folder
Drive A
└── MultiDrive
└── Movies
Unified virtual folder
MultiDrive
└── Movies
├── Drive A → movie1.mp4
├── Drive B → movie2.mp4
└── Drive C → movie3.mp4

আমি দ্বিতীয় model-টাও রাখব, কারণ এটিই 9Drive-এর unified-storage idea-কে Android-এ ভালোভাবে reproduce করবে।

8. UploadTask
   UploadTask
--------------------------------
id
localPath
fileName
mimeType
size
accountId
parentDriveFolderId
status
progress
uploadedBytes
speedBytesPerSecond
driveFileId
resumableSessionUri
retryCount
lastError
createdAt
updatedAt

এটা অত্যন্ত গুরুত্বপূর্ণ।

কারণ process kill হলেও upload state থাকবে।

9. Smart routing logic

তোমার দেওয়া example:

accounts.minBy {
it.storageQuota - it.storageUsed
}

এটা actually সবচেয়ে কম available space-এর account নির্বাচন করবে।

MOST_AVAILABLE-এর জন্য হবে:

accounts.maxByOrNull {
it.storageQuota - it.storageUsed
}

আর production implementation-এ শুধু quota দেখলেই হবে না।

আমরা হিসাব করব:

available =
quota
- used
- pendingUploadReservations

অর্থাৎ:

Drive A
Quota:       15 GB
Used:        10 GB
Uploading:    3 GB
Available:   2 GB

Drive B
Quota:       15 GB
Used:         5 GB
Uploading:    1 GB
Available:   9 GB

→ Drive B

এটা concurrent upload-এর race condition কমাবে।

10. Routing modes

আমি 9Drive-inspired চারটা mode রাখব:

MOST_AVAILABLE
Highest free space
ROUND_ROBIN
A → B → C → A → B → C

index Room/DataStore-এ persist হবে।

MANUAL

Upload-এর আগে:

Choose account
PRIORITY

9Drive-এর current routing system-এ priority-order-ও আছে।

1. Drive C
2. Drive A
3. Drive B

যেটিতে file fit করবে সেটি আগে ব্যবহার।

11. Upload architecture

Google Drive বর্তমানে simple, multipart এবং resumable upload support করে; Google-এর guidance অনুযায়ী large files এবং mobile/network-interruption scenarios-এ resumable upload উপযুক্ত।

আমাদের flow:

User selects file
↓
Get local URI
↓
Query MIME + size
↓
Select target account
↓
Check available quota
↓
Create UploadTask
↓
Initiate resumable session
↓
Save session URL
↓
Upload chunks
↓
Update progress
↓
Persist uploaded bytes
↓
Complete
↓
Save Drive file metadata
↓
Update quota
↓
Refresh Room
Chunking

আমি configurable chunk size রাখব:

8 MB / 16 MB / 32 MB

production default হিসেবে moderate chunk size।

12. Interrupted upload

ধরো:

1.8 GB file
uploaded = 734 MB

তারপর:

Wi-Fi disconnected

App বন্ধ হয়ে গেলেও:

UploadTask.status = UPLOADING
uploadedBytes = 734MB
resumableSessionUri = ...

পরেরবার:

WorkManager
↓
check session
↓
HTTP PUT Content-Range
↓
continue

Google-এর resumable protocol interruption হলে upload status query করে Range header অনুযায়ী resume করা যায়; session expire হলে restart করতে হবে।

13. Download architecture

Download flow:

File click
↓
AccountResolver
↓
valid access token
↓
GET files/{id}?alt=media
↓
stream response
↓
SAF output URI
↓
buffered write
↓
progress

Default download directory user একবার নির্বাচন করবে:

Settings
→ Default Download Folder
→ SAF folder picker
→ persist URI permission

তারপর সব download সেখানে যাবে।

14. Camera + Gallery
    Gallery

Android Photo Picker / SAF:

Photos
Videos
Files
Camera
CameraX
↓
temporary file
↓
UploadTask
↓
Google Drive

Camera file আগে Drive-এর memory-তে রাখা হবে না।

Local temporary file থেকে stream হবে।

15. Copy / Move

এটা খুব carefully implement করতে হবে।

Same account

Native Drive move:

old parent
↓
removeParents
↓
addParents
Different account

এটা সরাসরি normal move নয়।

আমাদের:

Source Account
↓
Download / Drive copy strategy
↓
Target Account
↓
Upload
↓
Optional source delete

Large binary files-এর জন্য resumable transfer থাকবে।

সুতরাং UI:

Move to...
├── Folder A
├── Folder B
└── Other Google Account

Cross-account move-কে একটি background task হিসেবে চালাব।

16. Share

9Drive backend share-token system ছিল, কিন্তু serverless app-এ সেটা দরকার নেই।

আমরা Google Drive-এর native permissions API ব্যবহার করব। Drive-এ user, group, domain, anyone permission type এবং reader, commenter, writer ইত্যাদি role আছে।

UI:

Share
├── Restricted
├── Anyone with link → Viewer
├── Anyone with link → Editor
├── Add person/email
└── Copy link

Public share তৈরি হবে:

POST /drive/v3/files/{fileId}/permissions

type = anyone
role = reader

Drive policy/organization restrictions থাকলে public sharing blocked হতে পারে।

17. File preview
    Image

Coil:

jpg
png
webp
gif
Video

Native Android player / Media3.

Audio

Media3.

PDF

Embedded PDF viewer strategy।

Microsoft Office

Drive viewer / external compatible viewer।

Google Docs/Sheets/Slides

Drive web viewer/export mechanism।

Unified PreviewScreen থাকবে:

PreviewContent
├── ImagePreview
├── PdfPreview
├── VideoPreview
├── AudioPreview
├── TextPreview
└── UnsupportedPreview
18. Unified File Browser

Home screen:

┌───────────────────────────────┐
│ MultiDrive                🔍   │
├───────────────────────────────┤
│ All Accounts ▼                │
├───────────────────────────────┤
│ Home / Movies / 2026          │
├───────────────────────────────┤
│ 📁 Movies                     │
│                                │
│ 🎬 movie1.mp4   [Gmail A]     │
│ 📄 contract.pdf [Gmail B]     │
│ 🖼 photo.jpg    [Gmail C]     │
│ 🗜 archive.zip  [Gmail A]     │
└───────────────────────────────┘

Search হবে Room cached metadata-এর ওপর।

Flow<FileMeta>
↓
combine(accounts)
↓
filter(search)
↓
sort
↓
LazyColumn
19. Breadcrumb

উদাহরণ:

Home
> Documents
> Work
> 2026

Back করলে:

2026
↓
Work
↓
Documents
↓
Home

Navigation state Compose Navigation + ViewModel-এর মধ্যে থাকবে।

20. Sorting

Supported:

Name ↑
Name ↓

Date newest
Date oldest

Size large → small
Size small → large

Type A → Z

Filter:

All
Images
Videos
Audio
Documents
PDF
Archives
Folders
Starred
21. Offline-first architecture

এটা app-এর সবচেয়ে শক্তিশালী অংশ হবে।

UI কখনো সরাসরি network থেকে file list নেবে না।

সবসময়:

Drive
↓
Remote DTO
↓
Repository
↓
Room
↓
Flow
↓
Compose UI

Network না থাকলে:

Room cached data
↓
UI
↓
Offline badge
22. Incremental sync

এখানে আমি শুধু বারবার files.list চালাতে চাই না।

Google Drive-এর changes API ব্যবহার করব।

প্রথম sync:

getStartPageToken
↓
full sync
↓
save page token

পরের sync:

saved page token
↓
changes.list
↓
added / modified / deleted
↓
update Room
↓
save newStartPageToken

Google নিজেই changes collection-কে file changes track করার efficient mechanism হিসেবে document করে। getStartPageToken() থেকে token নিয়ে changes.list() করতে হয়।

এতে হাজার হাজার file থাকলেও প্রতিবার full database rebuild করতে হবে না।

23. Sync architecture
    WorkManager
    │
    ├── Account A Sync
    ├── Account B Sync
    ├── Account C Sync
    └── Account D Sync

Network constraint:

NetworkType.CONNECTED

Optional user setting:

Sync only on Wi-Fi
24. Upload/Download background execution

Long-running transfer-এর জন্য foreground Worker ব্যবহার করব।

উদাহরণ:

Uploading:
example.iso

72%
↑
8.4 MB/s

Notification:

MultiDrive
Uploading example.iso
72%

Android 13+ এ notification runtime permission এবং foreground-service notifications-এর behaviour আলাদাভাবে handle করতে হয়।

25. Error handling architecture

একটা central error model থাকবে।

sealed class DriveError {
data object NetworkUnavailable : DriveError()
data object Unauthorized : DriveError()
data object QuotaExceeded : DriveError()
data object PermissionDenied : DriveError()
data object FileNotFound : DriveError()
data object RateLimited : DriveError()
data object ServerUnavailable : DriveError()
data class Unknown(val message: String) : DriveError()
}

HTTP mapping:

401 → re-authorize / refresh
403 → permission/quota analysis
404 → file missing
429 → exponential backoff
500 → retry
502 → retry
503 → retry
504 → retry

Google specifically documents 401 handling through token refresh/re-authorization, and 429/5xx through exponential backoff.

26. Retry policy

উদাহরণ:

Retry #1 → 1 sec
Retry #2 → 2 sec
Retry #3 → 4 sec
Retry #4 → 8 sec
Retry #5 → 16 sec

তারপর:

FAILED

User:

Retry

চাপতে পারবে।

27. Quota handling

প্রতিটি account-এর জন্য:

Used
Quota
Available
Percent

উদাহরণ:

Google A
7.2 / 15 GB
48%

Google B
13.9 / 15 GB
93%

Google C
3.4 / 15 GB
23%

Color logic:

< 70%  → Green
70–90% → Yellow
> 90%  → Red

Dashboard:

Total Storage
----------------
31.8 GB / 45 GB
70.7%

Google A ███████░░░
Google B ██████████
Google C ███░░░░░░░
28. Dashboard

Screen:

Dashboard

Total
31.8 GB / 45 GB

Accounts
────────────────

Gmail A
15 GB
7.2 GB used

Gmail B
15 GB
13.9 GB used

Gmail C
15 GB
3.4 GB used

Last sync:
Today, 12:02 PM

চার্ট implementation:

Canvas

অথবা Compose chart library।

আমি unnecessary heavy chart dependency না দিয়ে simple Compose Canvas chart ব্যবহার করতে পারি।

29. Settings
    Settings
    │
    ├── Accounts
    │   ├── Add account
    │   ├── Reorder
    │   ├── Remove
    │   └── Re-authenticate
    │
    ├── Upload Routing
    │   ├── Most Available
    │   ├── Round Robin
    │   ├── Priority
    │   └── Manual
    │
    ├── Download Folder
    │
    ├── Appearance
    │   ├── System
    │   ├── Light
    │   └── Dark
    │
    ├── Sync
    │   ├── Auto Sync
    │   ├── Wi-Fi only
    │   └── Sync interval
    │
    ├── Google Cloud
    │   └── API key/config
    │
    ├── Cache
    │   └── Clear cache
    │
    └── About
30. API key বিষয়ে আমার recommendation

তোমার specification-এ আছে:

User provides their own Google Cloud API key.

এটা রাখা সম্ভব, কিন্তু একটা distinction জরুরি:

Google API key OAuth credential-এর replacement নয়।

Drive user data access-এর জন্য OAuth scopes/authorization লাগবেই। Google Drive API OAuth scopes আলাদা authorization layer হিসেবে কাজ করে।

সুতরাং:

API Key
+
OAuth access

কে আলাদা configuration হিসেবে ধরব।

API key:

local configuration

OAuth credentials:

secure credentials
31. 9Drive-এর যেসব feature আমরা intentionally বাদ দেব

Serverless করার কারণে কিছু original feature সরাতে হবে।

বাদ
Email/password account system

কারণ app-level backend user account লাগছে না।

বাদ
JWT
বাদ
MySQL
বাদ
Server-side API key upload endpoint
বাদ
S3 credentials/backend gateway

9Drive বর্তমানে Google Drive-এর পাশাপাশি S3-compatible providers-ও support করে, কিন্তু direct serverless Android design-এ secret exposure এবং provider-specific credentials management অনেক বেশি complex হয়ে যাবে।

বাদ
Server-hosted preview token
বাদ
Server-side public share proxy

এর বদলে native Drive sharing।

32. Project architecture

আমি final Android project-টা মোটামুটি এইভাবে রাখব:

app/
└── src/main/java/com/multidrive/app/

    ├── MainActivity.kt

    ├── di/
    │   ├── AppModule.kt
    │   ├── DatabaseModule.kt
    │   ├── NetworkModule.kt
    │   ├── DriveModule.kt
    │   └── SecurityModule.kt

    ├── data/
    │
    │   ├── local/
    │   │   ├── MultiDriveDatabase.kt
    │   │   ├── dao/
    │   │   │   ├── AccountDao.kt
    │   │   │   ├── FileMetaDao.kt
    │   │   │   ├── FolderDao.kt
    │   │   │   ├── UploadTaskDao.kt
    │   │   │   └── SettingsDao.kt
    │   │   │
    │   │   └── entity/
    │   │       ├── AccountEntity.kt
    │   │       ├── FileMetaEntity.kt
    │   │       ├── FolderEntity.kt
    │   │       ├── UploadTaskEntity.kt
    │   │       └── AppSettingEntity.kt
    │   │
    │   ├── remote/
    │   │   ├── DriveApi.kt
    │   │   ├── DriveAuthInterceptor.kt
    │   │   ├── DriveAuthenticator.kt
    │   │   ├── DriveModels.kt
    │   │   ├── ResumableUploadClient.kt
    │   │   └── DriveErrorMapper.kt
    │   │
    │   ├── auth/
    │   │   ├── GoogleAuthManager.kt
    │   │   ├── DriveAuthorizationManager.kt
    │   │   └── SecureTokenStore.kt
    │   │
    │   ├── worker/
    │   │   ├── SyncWorker.kt
    │   │   ├── UploadWorker.kt
    │   │   └── DownloadWorker.kt
    │   │
    │   └── repository/
    │       ├── AccountRepositoryImpl.kt
    │       ├── FileRepositoryImpl.kt
    │       ├── FolderRepositoryImpl.kt
    │       ├── UploadRepositoryImpl.kt
    │       └── SettingsRepositoryImpl.kt
    │
    ├── domain/
    │   ├── model/
    │   ├── repository/
    │   └── usecase/
    │       ├── AddAccountUseCase.kt
    │       ├── RemoveAccountUseCase.kt
    │       ├── RefreshQuotaUseCase.kt
    │       ├── ListUnifiedFilesUseCase.kt
    │       ├── SearchFilesUseCase.kt
    │       ├── SyncAccountUseCase.kt
    │       ├── UploadFileUseCase.kt
    │       ├── DownloadFileUseCase.kt
    │       ├── DeleteFileUseCase.kt
    │       ├── RenameFileUseCase.kt
    │       ├── MoveFileUseCase.kt
    │       ├── CopyFileUseCase.kt
    │       ├── CreateFolderUseCase.kt
    │       ├── ShareFileUseCase.kt
    │       └── SelectTargetAccountUseCase.kt
    │
    └── presentation/
        ├── navigation/
        │   ├── AppNavGraph.kt
        │   └── Destinations.kt
        │
        ├── theme/
        │   ├── Color.kt
        │   ├── Theme.kt
        │   ├── Type.kt
        │   └── Shape.kt
        │
        ├── components/
        │   ├── FileRow.kt
        │   ├── AccountBadge.kt
        │   ├── StorageCard.kt
        │   ├── UploadProgressCard.kt
        │   ├── EmptyState.kt
        │   ├── ErrorState.kt
        │   └── LoadingState.kt
        │
        ├── home/
        ├── dashboard/
        ├── settings/
        ├── accounts/
        ├── upload/
        ├── downloads/
        ├── preview/
        └── viewmodel/
33. UI State architecture

প্রতিটি screen-এ থাকবে:

sealed interface UiState<out T> {
data object Loading : UiState<Nothing>

    data class Success<T>(
        val data: T
    ) : UiState<T>

    data class Error(
        val message: String
    ) : UiState<Nothing>
}

আর screen-specific state:

Loading
Success
Empty
Error
Offline
34. ViewModel architecture

উদাহরণ:

HomeViewModel
↓
StateFlow<HomeUiState>

DashboardViewModel
↓
StateFlow<DashboardUiState>

SettingsViewModel
↓
StateFlow<SettingsUiState>

AccountsViewModel
↓
StateFlow<AccountsUiState>

UploadViewModel
↓
Flow<List<UploadTask>>
35. Repository rule

UI কখনো:

OkHttp
Room
Google auth

directly call করবে না।

শুধু:

ViewModel
↓
UseCase
↓
Repository
36. Network layer

আমি endpoint abstraction এভাবে রাখব:

GET
/drive/v3/files

GET
/drive/v3/about

GET
/drive/v3/files/{id}

POST
/drive/v3/files

PATCH
/drive/v3/files/{id}

DELETE
/drive/v3/files/{id}

POST
/drive/v3/files/{id}/permissions

GET
/drive/v3/changes/startPageToken

GET
/drive/v3/changes

আর upload:

POST
/upload/drive/v3/files?uploadType=resumable

PUT
<session-url>

Google-এর REST API reference-এ এসব resources/methods সরাসরি available।

37. Account isolation

এটা security-critical।

প্রতিটি request:

AccountId
↓
Credential
↓
AccessToken
↓
Drive API

কখনো এমন হবে না:

Current selected account
↓
global access token

কারণ তখন account mix-up হওয়ার সম্ভাবনা থাকবে।

বরং:

driveClient.forAccount(accountId)

concept থাকবে।

38. Unified list algorithm
    Room
    ├── Account A files
    ├── Account B files
    ├── Account C files
    └── Account D files

        ↓

merge

        ↓

attach account badge

        ↓

remove duplicate cache records

        ↓

sort/filter/search

        ↓

Compose LazyColumn
39. Folder strategy

আমি recommend করছি প্রতিটি connected Drive account-এ একটি hidden root folder তৈরি করতে:

MultiDrive/

তারপর:

MultiDrive/
Documents/
Photos/
Videos/
Archives/

9Drive-ও uploads-কে dedicated 9drive folder-এর নিচে রাখে।

আমাদের version:

Google A
└── MultiDrive

Google B
└── MultiDrive

Google C
└── MultiDrive

এতে app-এর managed files আলাদা থাকবে।

40. কিন্তু existing Drive files?

User যদি বলে:

Show everything from my Drive

তাহলে optional mode:

Settings
→ Browse Entire Drive

এতে full Drive scope ব্যবহৃত হবে।

অথবা safer default:

Managed by MultiDrive

এবং আলাদা:

Browse Full Drive

এই separation privacy এবং OAuth review-এর দিক থেকে useful।

41. Recommended app modes

আমি actually app-এ দুইটা browsing mode রাখব:

Unified
All accounts
Account
Gmail A
Gmail B
Gmail C

এটা debugging এবং user control দুটোতেই সাহায্য করবে।

42. Account remove flow

User:

Remove Google Account?

বললে:

Revoke Google access
↓
Delete secure credentials
↓
Delete account's cached metadata
↓
Delete pending tasks
↓
Delete Room account

কিন্তু Drive-এর real files delete করা হবে না।

Google-এর guidance অনুযায়ী disconnect/revoke access এবং app-obtained data deletion support করা উচিত।

43. Cache clear

Settings:

Clear Cache

দুই ধরনের operation:

Soft cache
thumbnail cache
temporary upload/download files
Metadata cache
Room FileMeta

এটার জন্য confirmation:

Clear cached metadata?

Your Drive files will NOT be deleted.
44. Conflict handling

ধরো:

Room says:
report.pdf

কিন্তু Drive-এ:

deleted externally

Sync করবে:

FileMeta.trashed = true

এবং UI:

File unavailable

Upload conflict:

report.pdf already exists

options:

Replace
Keep both
Rename automatically
Cancel
45. Large file protection

Upload-এর আগে:

file size
↓
available quota
↓
reservation
↓
upload

যদি:

file = 12 GB
available = 5 GB

তাহলে immediately:

Not enough storage

routing system অন্য account try করবে।

46. Multiple simultaneous uploads

আমি unlimited parallel upload দেব না।

MAX_CONCURRENT_UPLOADS = 2

এর কারণ:

bandwidth
battery
429 rate limit
RAM

Upload queue:

Pending
↓
Uploading
↓
Completed / Failed
47. Local filesystem security

Temporary upload/download files:

cacheDir

এবং task শেষ হলে cleanup।

Permanent user files:

SAF URI

অ্যাপের private storage-এ অযথা copy রাখা হবে না।

48. Security layer

Security checklist:

✓ No client secret in source
✓ No access token in Room
✓ Keystore-backed secure credential store
✓ TLS only
✓ Certificate hostname verification
✓ No tokens in logs
✓ No Drive file content logs
✓ Obfuscated release build
✓ Backup exclusions for credential material
✓ URI permission validation
✓ Account isolation
49. Logging

Release build-এ:

Log.d(...)

sensitive data করবে না।

উদাহরণ:

খারাপ:

accessToken=ya29...

ভালো:

Drive request failed: 401 accountId=12
50. Google Cloud setup

Development-এর জন্য:

Google Cloud Project
↓
Drive API Enabled
↓
OAuth consent screen
↓
Android OAuth client
↓
SHA-1/SHA-256 config
↓
Credential Manager
↓
Drive authorization

তোমার app যেহেতু full Drive scope ব্যবহার করবে, verification planning শুরু থেকেই করতে হবে। Google documentation অনুযায়ী drive restricted scope এবং public application-এর ক্ষেত্রে verification/security-assessment requirements প্রযোজ্য হতে পারে।

51. 9Drive source reuse strategy

আমি source-টা তিন ভাগে দেখব।

Directly reusable concept
Routing algorithm
Account management model
Quota calculation
Upload state machine
Folder abstraction
File abstraction
Sync strategy
UI feature map
Port করতে হবে
TypeScript → Kotlin
Prisma → Room
Express service → Repository
HTTP controllers → UseCases
MySQL transactions → Room transactions
JWT → OAuth
Completely remove
Backend server
User session
JWT
MySQL
S3 gateway
server-side token exchange
server preview token
server API upload endpoint

9Drive-এর current repository structure এবং API flow থেকেও এই separation পরিষ্কার—backend-এ user/session/provider/account/file/folder/upload-session আলাদা persistence layer হিসেবে রয়েছে।

52. Development phases

আমি এটা একবারে 200টা file লিখে blind generate করব না। Production-grade result পেতে এই sequence:

Phase 1 — Foundation
Gradle
Manifest
Hilt
Compose
Theme
Navigation
Room
DataStore
OkHttp
WorkManager
Phase 2 — Google Authentication
Credential Manager
AuthorizationClient
Account add/remove
Multiple account support
Secure credentials
Phase 3 — Drive Core
files.list
about.get
files.get
files.create
files.update
files.delete
permissions
Phase 4 — Offline Database
Account
FileMeta
Folder
UploadTask
Sync state
Phase 5 — Sync Engine
initial sync
incremental changes
pagination
offline recovery
Phase 6 — Browser
Unified list
folders
breadcrumb
search
sort
filter
account badges
Phase 7 — Upload
file picker
camera
gallery
routing
resumable upload
queue
progress
speed
retry
Phase 8 — File Operations
rename
delete
copy
move
share
download
preview
Phase 9 — Dashboard
quota
charts
storage status
last sync
Phase 10 — Settings
routing
theme
download folder
accounts
API key
cache
about
Phase 11 — Hardening
401
403
404
429
5xx
offline
process death
low storage
duplicate uploads
quota exhaustion
Phase 12 — Release
R8
ProGuard
baseline profile
release signing
privacy
OAuth verification
Play Store
53. Testing plan

Production-ready বললে testing খুব গুরুত্বপূর্ণ।

Unit tests
selectTargetAccount()
quota calculation
routing
sorting
search
mime detection
retry policy
folder mapping
Repository tests
401
403
404
429
503
pagination
sync changes
Room tests
insert
update
delete
upsert
transaction
migration
Instrumentation
OAuth
account add/remove
SAF
camera
background worker
notification
Upload tests
10 MB
100 MB
1 GB
5 GB+
network interruption
process restart
quota failure
duplicate name
54. UI screens final list

আমি final app-এ roughly এই screens দেব:

Splash
↓
Account Setup / Home

Home
├── Unified Files
├── Search
├── Folder View
└── File Preview

Dashboard

Accounts
├── Add Account
├── Account Detail
└── Remove Account

Upload Center
├── Queue
├── Progress
└── Errors

Download Center

Settings
├── Routing
├── Theme
├── Download Folder
├── Sync
├── Accounts
├── API Key
├── Cache
└── About
55. Bottom navigation

আমি ব্যবহার করব:

┌────────────────────────────────┐
│ Files    Dashboard    Settings │
└────────────────────────────────┘

আর upload/download progress floating panel:

Uploading 2 files
72%

9Drive-এর bottom-right upload-progress concept-টাও এইভাবে native Android UI-তে port হবে।

56. Final dependency strategy

Main dependencies হবে:

Kotlin
Jetpack Compose
Material 3
Hilt
Room
Coroutines
Flow
Navigation Compose
OkHttp
Kotlin Serialization / Gson
Coil
WorkManager
DataStore
AndroidX Security / Keystore
Google Identity / Credential Manager
Google Play Services
Media3
CameraX

Google Drive Java client library mandatory করব না।

কারণ তোমার requirements-এর জন্য:

OkHttp
+
Drive REST v3

অনেক বেশি controllable, বিশেষ করে:

resumable upload
progress
chunking
retry
HTTP error mapping
multi-account token switching

চাইলে Google Drive Java client library-কে আলাদা integration layer হিসেবেও রাখা যায়; Google-এর own Drive documentation-এ Java client usage এবং changes API usage দেখানো আছে।

57. Final architecture এক লাইনে

পুরো system হবে:

Credential Manager
↓
Google Authorization
↓
Secure Account Credential
↓
Account-specific Drive Client
↓
Repository
↓
Room Cache
↓
Use Cases
↓
ViewModel + StateFlow
↓
Jetpack Compose

আর background:

WorkManager
├── Sync
├── Upload
└── Download
58. সবচেয়ে গুরুত্বপূর্ণ production decisions

আমি implementation শুরু করলে এই ৮টি decision lock করব:

1. No backend
2. No MySQL
3. No JWT
4. Native Google Authorization
5. Room is local metadata source
6. Google Drive stores actual files
7. WorkManager handles durable background work
8. Direct Drive REST/OkHttp handles network
59. 9Drive বনাম MultiDrive — final result

শেষে user experience হবে প্রায়:

                    MultiDrive

      ┌───────────┬───────────┬───────────┐
      │ Drive A   │ Drive B   │ Drive C   │
      │ 15 GB     │ 15 GB     │ 15 GB     │
      └─────┬─────┴─────┬─────┴─────┬─────┘
            │           │           │
            └───────────┼───────────┘
                        │
                 Unified Storage
                        │
       ┌────────────────┼────────────────┐
       │                │                │
    Upload           Browse           Download
       │                │                │
Smart route      Unified list      Local folder
│
Most available
Round robin
Priority
Manual

এতে 9Drive-এর core idea থাকবে, কিন্তু backend dependency থাকবে না।

একটা কথা বিশেষভাবে মনে রাখা দরকার: "serverless" মানে app-এর কোনো external dependency নেই—এটা নয়। Google Drive API, Google OAuth এবং Google Cloud project থাকবে; শুধু আমাদের নিজস্ব backend/database থাকবে না।

আর source reuse-এর ক্ষেত্রে বর্তমান 9Drive repository Apache-2.0 licensed; source ব্যবহার করলে license/copyright notices এবং license conditions preserve করতে হবে।

আমার recommended next step

এরপর আমি এই plan অনুযায়ী একটা সম্পূর্ণ Android Studio project structure freeze করে, তারপর settings.gradle.kts → root build.gradle.kts → app/build.gradle.kts → Manifest → Hilt → Room → OAuth → Drive REST → repositories → use cases → ViewModels → Compose screens → WorkManager → upload/download/sync—এই exact order-এ file-by-file complete source বানাব।
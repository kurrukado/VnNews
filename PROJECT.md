# Project: VnNews Android Jetpack Compose Application

## Architecture
- **Architecture Pattern**: MVVM (Model-View-ViewModel) with Unidirectional Data Flow (UDF) & StateFlow.
- **UI Framework**: Android Jetpack Compose (Compose BOM 2026.02.01), Material 3, Navigation Compose.
- **Networking**: Retrofit 2.11.0 + OkHttp 4.12.0 + GsonConverter consuming `https://api.rss2json.com/v1/api.json` for 5 VnExpress RSS feeds.
- **Image Loading**: Coil Compose 2.7.0 (`AsyncImage`) with disk/memory caching and fallback URL extraction.
- **Local Persistence**: Room Database 2.7.2 with KSP, Room DAO, Flow queries, and TypeConverters.
- **Preferences**: Jetpack DataStore Preferences 1.1.2 for dark theme toggle and selected news category state.
- **Background Tasks**: WorkManager 2.10.0 (`CoroutineWorker`) for 12-hour periodic background checks & NotificationCompat notifications.
- **Web Browsing**: Embedded WebView wrapped in `AndroidView` with loading indicator and hardware back handling.

## Code Layout
- `app/src/main/java/com/example/vnnews/`:
  - `MainActivity.kt`: Root activity, runtime permission handler, theme wrapper, NavHost entry.
  - `NewsApplication.kt`: Custom Application class, manual dependency injection container initialization, WorkManager periodic work enqueueing.
  - `data/`:
    - `AppContainer.kt`: Dependency injection container providing repository singletons.
    - `local/`:
      - `AppDatabase.kt`: Room database definition, versioning, fallback migration, TypeConverters.
      - `ArticleDao.kt`: Room DAO interface for bookmarking operations with Coroutines and Flow.
      - `ArticleEntity.kt`: Room table entity for bookmarked articles.
      - `Converters.kt`: Room TypeConverters for timestamps / dates.
      - `UserPreferencesRepository.kt`: DataStore preferences for theme and category persistence.
    - `remote/`:
      - `NewsApiService.kt`: Retrofit service for rss2json endpoint.
      - `NewsResponseDto.kt`: DTO models (RssFeedResponse, RssItemDto, EnclosureDto).
    - `repository/`:
      - `NewsRepository.kt`: Repository interface & `NetworkNewsRepository` implementation.
  - `ui/`:
    - `home/`: `HomeScreen.kt`, `HomeViewModel.kt` (Category tabs, article cards, swipe/click refresh).
    - `bookmarks/`: `BookmarksScreen.kt`, `BookmarksViewModel.kt` (Saved offline articles, swipe-to-delete/unbookmark).
    - `detail/`: `DetailScreen.kt`, `ArticleWebView.kt` (WebView embedded in AndroidView, progress bar, BackHandler).
    - `navigation/`: `NewsDestinations.kt`, `NewsNavGraph.kt` (Bottom bar navigation, routes, argument passing).
    - `theme/`: `Color.kt`, `Theme.kt`, `Type.kt` (Material 3 dynamic / dark theme styling).
  - `worker/`:
    - `DailyNewsNotificationWorker.kt`: Periodic CoroutineWorker fetching top news and posting notification.
- `app/src/test/java/com/example/vnnews/`:
  - Unit tests for ViewModels, Repositories, DataStore, and Room DAO logic.

## Feature Inventory
| # | Feature | Description | Milestone | Source |
|---|---------|-------------|-----------|--------|
| 1 | MVVM & StateFlow Architecture | ViewModel exposing sealed UI State (Loading, Success, Error) as StateFlow to Compose UI | M1 | ORIGINAL_REQUEST R1 (Unit 4) |
| 2 | Navigation Compose | NavHost with BottomNavigationBar for Home, Bookmarks, and Detail routes with single-top & state restoration | M1 | ORIGINAL_REQUEST R1 (Unit 4) |
| 3 | Safe Navigation Argument Passing | Single URL encoding/decoding for detail screen navigation without double-decoding bugs | M1 | Survey Explorer 2 |
| 4 | Retrofit 2 + rss2json API | HTTP client querying rss2json with 5 VnExpress categories (Tin mới nhất, Số hóa, Kinh doanh, Thể thao, Giải trí) | M1 | ORIGINAL_REQUEST R1 (Unit 5) |
| 5 | Coil Image Loading | AsyncImage thumbnail loading with crossfade and multi-source fallback (thumbnail, enclosure, regex) | M1 | ORIGINAL_REQUEST R1 (Unit 5) |
| 6 | Room Offline Bookmarking | Room Database (ArticleEntity, ArticleDao) for bookmarking articles with Flow streams | M1 | ORIGINAL_REQUEST R1 (Unit 6) |
| 7 | Room TypeConverters | Room TypeConverter for Date / Long timestamp conversions | M1 | ORIGINAL_REQUEST R1 (Unit 6) |
| 8 | DataStore Preferences | UserPreferencesRepository integrated with UI for persisting dark theme and selected category | M1 | ORIGINAL_REQUEST R1 (Unit 6) |
| 9 | WorkManager Periodic Notifications | 12-hour periodic CoroutineWorker checking latest news and posting notification channel alert | M1 | ORIGINAL_REQUEST R1 (Unit 7) |
| 10 | Android 13+ Notification Permission | Runtime permission request for POST_NOTIFICATIONS in MainActivity | M1 | Survey Explorer 2 |
| 11 | Embedded WebView Detail Screen | AndroidView wrapping WebView with JavaScript, DOM storage, loading indicator, and BackHandler | M1 | ORIGINAL_REQUEST R1 (Unit 8) |
| 12 | Comprehensive Unit Test Suite | Unit tests verifying HomeViewModel, BookmarksViewModel, NewsRepository, and DataStore logic | M1 / E2E | Verification standard |
| 13 | Local APK Build Generation | `./gradlew assembleDebug` exits with 0 and produces `app/build/outputs/apk/debug/app-debug.apk` | M2 | ORIGINAL_REQUEST R2 |
| 14 | Git Synchronization | Source code committed and pushed to `https://github.com/kurrukado/VnNews` on `origin/main` | M3 | ORIGINAL_REQUEST R3 |
| 15 | GitHub Release Publication | GitHub release tag `v1.0.0` published to `kurrukado/VnNews` with `app-debug.apk` attached as release asset | M3 | ORIGINAL_REQUEST R3 |

## Milestones
| # | Name | Scope | Dependencies | Status |
|---|------|-------|-------------|--------|
| M1 | Complete Curriculum Integration (Units 4-8) & Unit Tests | Connect DataStore to HomeViewModel & MainActivity, add Room TypeConverter, add POST_NOTIFICATIONS runtime prompt, fix URL double-decoding, add WebView BackHandler, implement unit tests | Survey complete | DONE |
| M2 | Local APK Build Generation & Build Verification | Execute `./gradlew.bat assembleDebug`, verify exit code 0, verify `app/build/outputs/apk/debug/app-debug.apk` artifact existence and integrity | M1 | DONE |
| M3 | GitHub Repository Sync & Release v1.0.0 Publication | Update `.gitignore`, commit clean code to `main`, push to `origin/main`, tag `v1.0.0`, create GitHub Release with APK asset attached | M2 | DONE |

## Interface Contracts
### DataStore ↔ HomeViewModel & MainActivity
- `UserPreferencesRepository.selectedCategory: Flow<String>`
- `UserPreferencesRepository.saveSelectedCategory(category: String): suspend Unit`
- `UserPreferencesRepository.isDarkTheme: Flow<Boolean?>`
- `UserPreferencesRepository.saveDarkTheme(isDark: Boolean): suspend Unit`

### Room Database & TypeConverters
- `Converters.fromTimestamp(value: Long?): Date?`
- `Converters.dateToTimestamp(date: Date?): Long?`
- Added to `@TypeConverters(Converters::class)` on `AppDatabase`.

### Navigation ↔ DetailScreen
- URL parameter encoded with `URLEncoder.encode(url, "UTF-8")`
- NavHost route matches string parameter; decoded properly without redundant second decoding pass.

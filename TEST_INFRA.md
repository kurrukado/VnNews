# E2E Test Infra: VnNews Android Application

## Test Philosophy
- Requirement-driven verification covering Android Basics with Compose Units 4 through 8.
- Methodology: Category-Partition + Boundary Value Analysis + Unit Testing of Architecture Components + Local APK Build Verification + GitHub Release Integrity.

## Feature Inventory & Test Mapping
| # | Feature | Requirement Source | Test Level | Verification Method |
|---|---------|-------------------|------------|---------------------|
| 1 | MVVM & StateFlow Architecture | ORIGINAL_REQUEST R1 (Unit 4) | Unit / Instrumented | StateFlow emissions (Loading -> Success/Error) in HomeViewModel & BookmarksViewModel |
| 2 | Navigation Compose | ORIGINAL_REQUEST R1 (Unit 4) | Unit / Integration | Navigation destinations, argument passing, bottom bar route filtering |
| 3 | Retrofit 2 RSS Feed Fetching | ORIGINAL_REQUEST R1 (Unit 5) | Unit / Mock | NewsApiService parsing rss2json responses across 5 VnExpress categories |
| 4 | Room Offline Bookmarking & TypeConverters | ORIGINAL_REQUEST R1 (Unit 6) | Unit / DAO Test | ArticleDao CRUD operations, Flow observation, TypeConverters serialization/deserialization |
| 5 | DataStore Preferences Integration | ORIGINAL_REQUEST R1 (Unit 6) | Unit / State Test | UserPreferencesRepository dark theme & selected category persistence |
| 6 | WorkManager Periodic Notifications | ORIGINAL_REQUEST R1 (Unit 7) | Static & Unit | CoroutineWorker execution, notification channel creation, 12h periodic constraint |
| 7 | Embedded WebView in AndroidView | ORIGINAL_REQUEST R1 (Unit 8) | Static & UI | WebView setup, JavaScript enabled, loading state indicator, BackHandler |
| 8 | Local APK Build Generation | ORIGINAL_REQUEST R2 | Build / CLI | `.\gradlew.bat assembleDebug` exit 0, APK existence at `app/build/outputs/apk/debug/app-debug.apk` |
| 9 | GitHub Sync & v1.0.0 Release | ORIGINAL_REQUEST R3 | Git / API | Git status clean, origin/main synced, tag `v1.0.0` pushed, release created with attached APK asset |

## Test Architecture
- Test Runner: `.\gradlew.bat testDebugUnitTest` and `.\gradlew.bat assembleDebug`.
- Unit Tests Location: `app/src/test/java/com/example/vnnews/`.
- Release Validation: PowerShell script verifying APK asset on GitHub Release v1.0.0 via `gh release view`.

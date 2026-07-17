# OTA Pulse Architecture

## Purpose

This document describes the current OTA Pulse codebase as it exists in the repository today.

> A practical map of how OTA Pulse is organized right now, with emphasis on ownership, clean modular boundaries, and runtime flow.

It is meant to help with:

- onboarding
- feature placement
- refactors
- avoiding package drift

## Repository Shape

This repository contains three distinct pieces:

1. the Android product in [app](app)
2. a static landing page in [index.html](index.html) with assets in [OTAPulse](OTAPulse)
3. a JSON-driven device catalog in [devices](devices)

The rest of this document focuses on the Android app, because that is where the runtime and package boundaries matter most.

## Build Snapshot

From [app/build.gradle.kts](app/build.gradle.kts) and [gradle/libs.versions.toml](gradle/libs.versions.toml):

- single Android application module
- namespace `com.abhinav.otapulse`
- Kotlin 2.4.0 + Java 17
- `minSdk = 29`
- `targetSdk = 37`
- `compileSdk = 37`
- `versionCode = 29`
- `versionName = 4.0.1`
- Jetpack Compose 100% UI (`composeBom = 2026.06.01`, Material 3 `1.5.0-alpha22`, Material Kolor `5.0.0-alpha07`)
- ViewBinding enabled (`buildFeatures { viewBinding = true; buildConfig = true; compose = true }`)
- Hilt 2.60.1 + KSP 2.3.9
- WorkManager 2.11.2 (with Hilt integration)
- OkHttp 5.4.0 (networking + custom download engine)
- Room 2.8.4 (database persistence)
- Glide 5.0.7
- Markwon 4.6.2 (core)
- Gson 2.14.0
- Flexbox 3.0.0
- Protobuf Lite 4.35.1 (with protobuf Gradle plugin 0.10.0)
- XZ 1.12, Brotli 0.1.2, and Commons Compress 1.28.0 for OTA payload / extraction work
- AndroidX Navigation Compose 2.9.8
- AndroidX Lifecycle Compose 2.11.0
- Coroutines 1.11.0
- Dynamic Animation (`androidx.dynamicanimation.ktx`)

**Important Nuances**

- **100% Jetpack Compose Presentation**: The app has migrated from XML fragments to a pure Jetpack Compose presentation layer. While legacy `fragment-ktx` and `viewBinding` dependencies remain in `build.gradle.kts` for compatibility, primary navigation and UI presentation are driven entirely by `OtaPulseApp.kt` (`setContent { OtaPulseApp() }`) and `OtaPulseNavGraph.kt`.
- **Custom Download Engine**: The app uses a custom high-performance download engine built directly on OkHttp in `core/download` with full pause/resume capabilities and background progress tracking.

## App Entry Points

**From** [AndroidManifest.xml](app/src/main/AndroidManifest.xml):

- application class: [OtaPulseApplication.kt](app/src/main/java/com/abhinav/otapulse/app/OtaPulseApplication.kt)
- launcher activity: [MainActivity.kt](app/src/main/java/com/abhinav/otapulse/app/MainActivity.kt) — exported, launcher intent (`setContent { OtaPulseApp() }`)
- secondary activity: [InAppBrowserActivity.kt](app/src/main/java/com/abhinav/otapulse/feature/browser/InAppBrowserActivity.kt)
- secondary activity: [JsonOutputActivity.kt](app/src/main/java/com/abhinav/otapulse/feature/otatools/ui/JsonOutputActivity.kt)
- receiver: [DownloadActionReceiver.kt](app/src/main/java/com/abhinav/otapulse/core/receiver/DownloadActionReceiver.kt) (not exported)
- receiver: [BootCompletedReceiver.kt](app/src/main/java/com/abhinav/otapulse/core/receiver/BootCompletedReceiver.kt) (exported, handles `BOOT_COMPLETED` + `LOCKED_BOOT_COMPLETED`)
- service: [DownloadForegroundService.kt](app/src/main/java/com/abhinav/otapulse/core/download/DownloadForegroundService.kt) (`foregroundServiceType=dataSync`)
- service: `SystemForegroundService` (WorkManager foreground, `foregroundServiceType=dataSync`)
- provider: `FileProvider` with `@xml/file_paths`
- provider: `InitializationProvider` (removes `WorkManagerInitializer` for custom init)
- locale service: `AppLocalesMetadataHolderService` (disabled, auto locale storage)

[OtaPulseApplication.kt](app/src/main/java/com/abhinav/otapulse/app/OtaPulseApplication.kt) wires Hilt and WorkManager, and applies Dynamic Color / locale configurations when available.

**Manifest Permissions:**

- `INTERNET`, `ACCESS_NETWORK_STATE`
- `WRITE_EXTERNAL_STORAGE` (`maxSdkVersion 29`), `READ_EXTERNAL_STORAGE` (`maxSdkVersion 29`)
- `MANAGE_EXTERNAL_STORAGE`
- `POST_NOTIFICATIONS`
- `VIBRATE`
- `WAKE_LOCK`
- `FOREGROUND_SERVICE`, `FOREGROUND_SERVICE_DATA_SYNC`
- `RECEIVE_BOOT_COMPLETED`
- `REQUEST_IGNORE_BATTERY_OPTIMIZATIONS`
- `REQUEST_INSTALL_PACKAGES`

## Top-Level Source Layout

**Main Package Root**

```text
app/src/main/java/com/abhinav/otapulse/
├─ app/
├─ arb/
├─ catalog/
├─ core/
├─ di/
├─ feature/
├─ navigation/
└─ ota/
```

This layout represents a modern, highly modularized Android codebase separating app startup shell, Compose navigation, feature-specific UI and business logic, domain device catalogs, engine primitives (`ota`/`arb`), and shared `core` utilities.

## Package Ownership

### `app`

This package is the application shell and Compose root container.

Key files:

- [MainActivity.kt](app/src/main/java/com/abhinav/otapulse/app/MainActivity.kt) (14 KB)
- [MainViewModel.kt](app/src/main/java/com/abhinav/otapulse/app/MainViewModel.kt)
- [OtaPulseApp.kt](app/src/main/java/com/abhinav/otapulse/app/OtaPulseApp.kt) (16.6 KB)
- [OtaPulseApplication.kt](app/src/main/java/com/abhinav/otapulse/app/OtaPulseApplication.kt)

What it owns:

- app startup and Hilt wiring (`OtaPulseApplication`)
- WorkManager custom configuration
- splash screen installation (`installSplashScreen()`)
- edge-to-edge window decor and first-launch permission flows (`MainActivity`)
- Compose root `Scaffold`, bottom navigation bar (`NavigationBar`), global top app bar, search bar state, and dialog coordination (`OtaPulseApp`)

Current reality:

- `MainActivity` is minimal and delegates presentation logic directly to `OtaPulseApp()`.
- `OtaPulseApp` orchestrates bottom navigation items (`Home`, `Devices`, `Downloads`, `History`, `Tools`, `Settings`) and wires global haptic feedback and sheet dialogs.

### `navigation`

This package owns global Jetpack Compose navigation definitions and transition animations.

Structure:

```text
navigation/
├─ NavigationAnimations.kt
├─ OtaPulseNavGraph.kt
└─ Screen.kt
```

Responsibilities:

- strong/typed route declarations via sealed class `Screen` (`Screen.HomeUpdate`, `Screen.Devices`, `Screen.Downloads`, `Screen.History`, `Screen.OtaTools`, `Screen.Settings`, `Screen.About`, `Screen.Libraries`, `Screen.ManualQuery`, `Screen.PartitionExtraction`, `Screen.LinkResolver`, `Screen.ArbChecker`, `Screen.InAppBrowser`)
- `NavHost` setup connecting every `Screen` route to its corresponding `@Composable` feature screen (`OtaPulseNavGraph`)
- customized enter/exit slide and fade animation transitions across screen boundaries (`NavigationAnimations`)

### `feature`

This package owns user-facing product flows and `@Composable` screens. Current feature directories are:

- `about`
- `browser`
- `devicecatalog`
- `devices`
- `downloads`
- `history`
- `otatools`
- `settings`
- `updates`

#### `feature/devicecatalog` & `feature/devices`

The device catalog browsing flow is split across two specialized feature packages:

**Presentation (`feature/devicecatalog/ui/`)**:

- [DeviceCatalogScreen.kt](app/src/main/java/com/abhinav/otapulse/feature/devicecatalog/ui/DeviceCatalogScreen.kt) (38 KB) — searchable, filterable grid/list presentation of supported devices and favorites
- [AddDeviceScreen.kt](app/src/main/java/com/abhinav/otapulse/feature/devicecatalog/ui/AddDeviceScreen.kt) (34 KB) — custom device definition creation screen
- [OtaDetailsSheet.kt](app/src/main/java/com/abhinav/otapulse/feature/devicecatalog/ui/OtaDetailsSheet.kt) (48 KB) — rich bottom sheet displaying full OTA metadata, direct link options, and partition extraction controls

**Domain & ViewModels (`feature/devices/`)**:

- `domain/` — `FetchOtaDetailsUseCase`, `FetchOtaUpdateUseCase`, `GetDevicesUseCase`, `ToggleFavoriteUseCase`
- `ui/` — `DevicesViewModel.kt` (14.5 KB), `AddDeviceViewModel.kt` (7.7 KB)

Responsibilities:

- device browsing, search, and region/variant filtering
- favorites management
- custom device creation and persistence
- OTA details presentation and extraction preparation

#### `feature/downloads`

This owns the download queue presentation and file lifecycle management.

Structure:

```text
feature/downloads/
├─ data/
│  └─ DownloadManager.kt
├─ domain/
│  ├─ DeleteFileUseCase.kt
│  ├─ DownloadRepository.kt
│  ├─ EnqueueDownloadUseCase.kt
│  ├─ GetDownloadsUseCase.kt
│  └─ GetTargetFileUseCase.kt
└─ ui/
   ├─ DownloadsScreen.kt
   └─ DownloadsViewModel.kt
```

Responsibilities:

- enqueue, pause, resume, retry, cancel, and delete download records
- target file path generation and management
- exposing download-state flows to Compose (`DownloadsScreen.kt` 33 KB)
- orchestrating download operations through domain use cases and `DownloadManager.kt` (23.7 KB)

#### `feature/history`

This feature tracks and displays past OTA query lookups.

Structure:

```text
feature/history/
├─ data/
│  ├─ local/
│  │  ├─ OtaHistoryDao.kt
│  │  └─ OtaHistoryEntity.kt
│  ├─ OtaHistoryRepository.kt
│  └─ OtaHistoryRepositoryImpl.kt
└─ ui/
   ├─ HistoryScreen.kt
   └─ OtaHistoryViewModel.kt
```

Responsibilities:

- persisting query history locally via Room database (`OtaHistoryDao`, `OtaHistoryEntity`)
- displaying past query cards (`HistoryScreen.kt` 25.6 KB) with one-click re-query support

#### `feature/updates`

This feature powers the Home Screen update experience.

Structure:

```text
feature/updates/
└─ ui/
   ├─ HomeUpdateScreen.kt
   ├─ HomeUpdateUiState.kt
   └─ HomeUpdateViewModel.kt
```

Responsibilities:

- Home screen discovery card (`HomeUpdateScreen.kt` 33.5 KB) showing hardware model, SoC details, device codename, region/variant, and software update status
- background check trigger and state management (`HomeUpdateViewModel.kt` 13.8 KB)

#### `feature/otatools`

This is the central hub for advanced OTA diagnostic and extraction utilities.

Structure:

```text
feature/otatools/
├─ data/
│  └─ ArbLookupService.kt
└─ ui/
   ├─ ArbCheckerScreen.kt
   ├─ JsonOutputActivity.kt
   ├─ LinkResolverScreen.kt
   ├─ ManualQueryScreen.kt
   ├─ OtaToolsScreen.kt
   ├─ OtaToolsViewModel.kt
   └─ PartitionExtractionScreen.kt
```

Included tools:

- **Manual Query** (`ManualQueryScreen.kt` 61.6 KB) — custom parameter querying, region name display, dropdown hints, and direct JSON sharing
- **Partition Extraction** (`PartitionExtractionScreen.kt` 43.2 KB) — payload manifest inspection and selective partition extraction
- **Link Resolver** (`LinkResolverScreen.kt` 18.8 KB) — direct OTA link validation and resolution
- **ARB Checker** (`ArbCheckerScreen.kt` 32.3 KB & `ArbLookupService.kt` 2.6 KB) — Anti-Rollback index inspection and safety verification
- **JSON Output** (`JsonOutputActivity.kt` 10.9 KB) — raw JSON display and share actions
- **Hub Navigation** (`OtaToolsScreen.kt` 13.0 KB & `OtaToolsViewModel.kt` 27.7 KB)

#### `feature/browser`

In-app WebView browser flow.

Files:

- [InAppBrowserActivity.kt](app/src/main/java/com/abhinav/otapulse/feature/browser/InAppBrowserActivity.kt) (2.8 KB fallback activity)
- [InAppBrowserScreen.kt](app/src/main/java/com/abhinav/otapulse/feature/browser/ui/InAppBrowserScreen.kt) (23.4 KB Compose screen)

Responsibilities:

- edge-to-edge in-app browsing for OTA links and documentation
- desktop-mode toggle, share/copy controls, and external browser handover

#### `feature/settings`

Owns app preferences, data import/export, app updates, and open-source licenses display.

Structure:

```text
feature/settings/
├─ AppUpdateRepository.kt
├─ AppUpdateRepositoryImpl.kt
├─ CheckAppUpdateUseCase.kt
├─ SettingsViewModel.kt
├─ libraries/
│  └─ ui/
│     └─ LibrariesScreen.kt
└─ ui/
   ├─ AppUpdateScreen.kt
   ├─ SettingsDialogs.kt
   └─ SettingsScreen.kt
```

Responsibilities:

- theme selection (`Holographic`, `Monochrome`, light/dark, dynamic colors) via `SettingsScreen.kt` (44.6 KB) and `SettingsDialogs.kt` (24.8 KB)
- browser preferences and ARB detection toggle
- SAF-based import/export of custom device backups
- app self-updating (`AppUpdateScreen.kt` 23.7 KB and `CheckAppUpdateUseCase`)
- open-source libraries listing (`LibrariesScreen.kt` 11.5 KB)

#### `feature/about`

Lightweight project info screen.

Files:

- [AboutScreen.kt](app/src/main/java/com/abhinav/otapulse/feature/about/ui/AboutScreen.kt) (34.2 KB)
- [WhatsNewSheet.kt](app/src/main/java/com/abhinav/otapulse/feature/about/ui/WhatsNewSheet.kt) (5.4 KB)

Responsibilities:

- displays developer profile links (GitHub/LinkedIn), project links, version information, and release changelog.

### `catalog`

This package owns device catalog parsing and user persistence (favorites, custom devices).

Structure:

```text
catalog/
├─ model/
│  ├─ PredefinedDevice.kt
│  ├─ Region.kt
│  └─ RegionData.kt
└─ repository/
   ├─ CustomDeviceManager.kt
   ├─ DeviceCatalogParser.kt
   ├─ DeviceRepository.kt
   ├─ DeviceRepositoryImpl.kt
   └─ FavoritesManager.kt
```

Architectural rules:

- all built-in device definitions are stored in the top-level `devices/` directory (`oneplus.json`, `oppo.json`, `realme.json`).
- `DeviceCatalogParser` parses asset JSONs at runtime, generating firmware group trees and region definitions.
- `catalog/repository` owns user persistence (`FavoritesManager`, `CustomDeviceManager`).

### `ota`

This package owns the core OTA engine behavior.

Structure:

```text
ota/
├─ engine/
│  ├─ LocalOtaAccess.kt
│  ├─ OtaExtractor.kt
│  ├─ OtaRepository.kt
│  └─ OtaRepositoryImpl.kt
├─ network/
│  ├─ OtaApi.kt
│  ├─ RangeHttpClient.kt
│  └─ ServerCapabilityChecker.kt
├─ payload/
│  ├─ PayloadExtractor.kt
│  └─ PayloadManifest.kt
├─ resume/
│  └─ ExtractionState.kt
└─ zip/
   └─ ZipRemoteParser.kt
```

Responsibilities:

- server query execution (`OtaApi`, `OtaRepository`)
- range-request HTTP support and capability detection (`RangeHttpClient`, `ServerCapabilityChecker`)
- remote ZIP inspection (`ZipRemoteParser`)
- payload manifest extraction (`PayloadExtractor`, `PayloadManifest`)
- extraction state tracking (`ExtractionState`, `OtaExtractor`)

### `arb`

Anti-Rollback and extraction-safety verification subsystem.

Structure:

```text
arb/
├─ parser/
│  ├─ ArbChecker.kt
│  └─ XblConfigParser.kt
└─ worker/
   └─ PartitionExtractorWorker.kt
```

Responsibilities:

- XBL binary parsing and ARB index extraction (`XblConfigParser`, `ArbChecker`)
- background extraction execution via `PartitionExtractorWorker`

### `core`

This package owns shared infrastructure across the application.

Subpackages:

- `common` — utilities (`AnimationUtils`, `Crypto`, `DeviceUtils` 9.9 KB, `FormatUtils`, `HapticUtils`, `LocaleHelper`, `Mappers`, `Md5Verifier`, `PermissionHelper`), plus UI card/share generation helpers (`OtaCardGenerator.kt` 14.5 KB, `OtaShareHelper.kt` 2.1 KB)
- `database` — Room database setup (`AppDatabase`, `Converters`)
- `download` — custom OkHttp download engine (`OkHttpDownloadEngine.kt` 24.8 KB, `DownloadForegroundService.kt` 5.9 KB, `DownloadRecord`, `DownloadListener`, `DownloadStatus`, `DownloadError`)
- `model` — shared domain models (`AppUpdateInfo`, `Device`, `DownloadInfo`, `OtaError`, `OtaHistoryEntry`, `OtaRequest`, `OtaUpdate`, `RegionVariant`)
- `network` — networking primitives (`AppUpdateDownloader.kt`, `Component`, `CustomOtaRequest`, `Data`, `GitHubUpdater`, `OtaResolver`, `Request.kt` 15.1 KB)
- `notifications` — notification helpers (`DownloadNotificationHelper.kt` 18.3 KB)
- `preferences` — datastore preferences (`AppSettingsPreferences.kt` 4.1 KB, `ThemePreferences.kt` 4.6 KB)
- `receiver` — broadcast receivers (`BootCompletedReceiver.kt`, `DownloadActionReceiver.kt`)
- `ui` — shared Compose design system (`DialogEffects`, `WavyCircularProgressIndicator`), `components/` (`15 reusable composables`: `BentoGridLayout`, `EmptyState`, `ErrorState`, `FloatingSearchBar`, `HolographicSurface`, `LoadingState`, `OtaAnimations`, `OtaButton`, `OtaCard`, `OtaDialogs`, `OtaSwitch`, `OtaTextField`, `OtaTopAppBar`, `ProgressSheet`, `WavyProgressComposable`), and `theme/` (`Color`, `HolographicTheme`, `Motion`, `Shape`, `Theme`, `ThemeMode`, `Type`)
- `worker` — WorkManager workers (`DownloadWorker`, `SoftwareUpdateCheckWorker.kt` 9.0 KB)

### `di`

Hilt dependency injection modules.

Files:

- [AppModule.kt](app/src/main/java/com/abhinav/otapulse/di/AppModule.kt)
- [DatabaseModule.kt](app/src/main/java/com/abhinav/otapulse/di/DatabaseModule.kt)
- [RepositoryModule.kt](app/src/main/java/com/abhinav/otapulse/di/RepositoryModule.kt)
- [Qualifiers.kt](app/src/main/java/com/abhinav/otapulse/di/Qualifiers.kt)

Provides:

- Room database and DAOs
- OkHttpClient singletons and network qualifiers
- `OtaExtractor` and repository interface bindings (`OtaRepository`, `DeviceRepository`, `DownloadRepository`, `OtaHistoryRepository`, `AppUpdateRepository`)

## Runtime Flow

The runtime flow proceeds as follows:

1. [OtaPulseApplication.kt](app/src/main/java/com/abhinav/otapulse/app/OtaPulseApplication.kt) initializes Hilt and WorkManager.
2. [MainActivity.kt](app/src/main/java/com/abhinav/otapulse/app/MainActivity.kt) launches, installs the splash screen, checks permissions, and delegates presentation to `setContent { OtaPulseApp() }`.
3. [OtaPulseApp.kt](app/src/main/java/com/abhinav/otapulse/app/OtaPulseApp.kt) renders the root Compose `Scaffold` and delegates screen routing to [OtaPulseNavGraph.kt](app/src/main/java/com/abhinav/otapulse/navigation/OtaPulseNavGraph.kt).
4. Feature screens (`feature/*/ui/*.kt`) consume data flows from ViewModels, which coordinate with use cases or repository interfaces.
5. Device definitions flow from `catalog/repository/DeviceRepository` parsing the `devices/*.json` files.
6. OTA requests and payload extractions run through `ota/engine/OtaRepository`.
7. ARB checks and partition safety operations run through `arb/`.
8. Downloads are managed by `DownloadManager` in `feature/downloads/data`, which controls the `OkHttpDownloadEngine` and `DownloadForegroundService` in `core/download`.
9. `BootCompletedReceiver` wakes after device reboot to reschedule background firmware update checks via `SoftwareUpdateCheckWorker`.

## Resource Layer & Localization

The resource tree (`app/src/main/res/`) supports comprehensive layout, drawable, and localization assets:

- **Localization (25 regional locales)**: `values-ar`, `values-bn`, `values-de`, `values-es`, `values-fil`, `values-fr`, `values-hi`, `values-id`, `values-in`, `values-it`, `values-ja`, `values-ms`, `values-pt`, `values-pt-rBR`, `values-pt-rPT`, `values-ru`, `values-th`, `values-tl`, `values-tr`, `values-ur`, `values-vi`, `values-zh`, `values-zh-rCN`, `values-zh-rTW`, plus default English (`values`).
- **Theming**: Compose `HolographicTheme` and `MaterialKolor` drive dynamic colors, while `values` / `values-night` provide baseline window attributes and splash decor.
- **Drawables & Mipmaps**: Density-qualified vector icons, adaptive app icons, and background shapes across `anydpi`, `hdpi` through `xxxhdpi` buckets.

## Testing Reality

Current tests cover shared utilities and core primitives:

- Unit tests (`app/src/test/java/`): `ExampleUnitTest.kt`, `util/CryptoTest.kt`, `util/FormatUtilsTest.kt`, `util/RequestTest.kt`
- Instrumented tests (`app/src/androidTest/java/`): `ExampleInstrumentedTest.kt`

## Current Pressure Points

### 1. `ManualQueryScreen.kt` (61.6 KB)

As the largest single Compose file in the project, `ManualQueryScreen` handles parameter input, validation, region selection, and direct querying. It is a strong candidate for future extraction into smaller sub-composables.

### 2. `OtaDetailsSheet.kt` (48.3 KB)

Handles comprehensive OTA metadata presentation, direct download triggers, and partition selection within a bottom sheet. Splitting UI sections into dedicated components will improve maintainability.

### 3. `SettingsScreen.kt` (44.6 KB) & `DeviceCatalogScreen.kt` (38.0 KB)

Rich feature screens carrying significant layout configuration, dialog management, and filtering controls.

### 4. `PartitionExtractionScreen.kt` (43.2 KB)

Coordinates complex manifest inspection and extraction flows.

### 5. `OkHttpDownloadEngine.kt` (24.8 KB) & `DownloadManager.kt` (23.7 KB)

These classes manage range requests, file stream safety, storage paths, atomic state updates, and notification synchronization. They require careful discipline during network or storage refactors.

## Placement Rules

When contributing code:

1. Put user-facing Compose screens in the owning `feature/*/ui/` directory.
2. Put navigation routes and `NavHost` declarations in `navigation/`.
3. Put new device definitions in the `devices/` top-level JSON files, and catalog logic/persistence in `catalog/`.
4. Put OTA engine logic in `ota/` and rollback/safety logic in `arb/`.
5. Put app shell coordination in `app/`.
6. Put shared infrastructure in `core/` only when actually used across multiple feature boundaries.
7. Keep `di/` focused on wiring singletons and qualifiers.
8. Treat `index.html` and `OTAPulse/` as static web assets outside the Android runtime.

## Final Principle

OTA Pulse stays maintainable when ownership is obvious:

- `app` runs the shell and Compose root
- `navigation` owns routes and navigation transitions
- `feature` owns product screens (`about`, `browser`, `devicecatalog`, `devices`, `downloads`, `history`, `otatools`, `settings`, `updates`)
- `catalog` owns parsing supported devices and user persistence
- `ota` owns OTA engine behavior
- `arb` owns rollback and extraction safety
- `core` owns shared utilities, preferences, download engine, and shared UI design system (`core/ui/components`)
- `di` wires dependencies together

If new code does not clearly belong to one of those owners, stop and decide before adding it.

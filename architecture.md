# OTA Pulse Architecture

## Purpose

This document describes the current OTA Pulse codebase as it exists in the repository today.

> A practical map of how OTA Pulse is organized right now, with emphasis on ownership and runtime flow.

It is meant to help with:

- onboarding
- feature placement
- refactors
- avoiding package drift

## Repository Shape

This repository now contains three distinct pieces:

1. the Android product in [app](app)
2. a static landing page in [index.html](index.html) with assets in [OTAPulse](OTAPulse)
3. a JSON-driven device catalog in [devices](devices)

The rest of this document focuses on the Android app, because that is where the runtime and package boundaries matter most.

## Build Snapshot

From [app/build.gradle.kts](app/build.gradle.kts) and [gradle/libs.versions.toml](gradle/libs.versions.toml):

- single Android application module
- namespace `com.abhinav.otapulse`
- Kotlin 2.3.21 + Java 17
- `minSdk = 29`
- `targetSdk = 37`
- `compileSdk = 37`
- `versionCode = 22`
- `versionName = 3.0.7`
- ViewBinding enabled
- Hilt 2.59.2 + KSP 2.3.4
- WorkManager 2.11.2 (with Hilt integration)
- OkHttp 5.3.2 (networking + custom download engine)
- Glide 5.0.7
- Markwon 4.6.2 (core)
- Gson 2.14.0
- Flexbox 3.0.0
- Protobuf Lite 4.35.0 (with protobuf Gradle plugin 0.10.0)
- XZ 1.12 and Commons Compress 1.28.0 for OTA payload / extraction work
- AndroidX Navigation 2.9.8 (fragment-ktx, ui-ktx)
- AndroidX Lifecycle 2.10.0 (viewmodel-ktx, livedata-ktx)
- Coroutines 1.11.0
- Dynamic Animation (androidx.dynamicanimation.ktx)

A version catalog exists at `gradle/libs.versions.toml`. Compose BOM entries are declared in the catalog but **not currently used** in `app/build.gradle.kts` dependencies.

**Important Nuances**

- Navigation dependencies exist in Gradle, but the app shell currently uses manual fragment transactions instead of a NavHost-driven flow.
- The app previously used the Fetch library for downloads. This has been **replaced** by a custom download engine built on OkHttp in `core/download`.

## App Entry Points

**From** [AndroidManifest.xml](app/src/main/AndroidManifest.xml):

- application class: [OtaPulseApplication.kt](app/src/main/java/com/abhinav/otapulse/app/OtaPulseApplication.kt)
- launcher activity: [MainActivity.kt](app/src/main/java/com/abhinav/otapulse/app/MainActivity.kt) — exported, launcher intent
- secondary activity: [InAppBrowserActivity.kt](app/src/main/java/com/abhinav/otapulse/feature/browser/InAppBrowserActivity.kt)
- secondary activity: [JsonOutputActivity.kt](app/src/main/java/com/abhinav/otapulse/feature/otatools/ui/JsonOutputActivity.kt)
- receiver: [DownloadActionReceiver.kt](app/src/main/java/com/abhinav/otapulse/core/receiver/DownloadActionReceiver.kt) (not exported)
- receiver: [BootCompletedReceiver.kt](app/src/main/java/com/abhinav/otapulse/core/receiver/BootCompletedReceiver.kt) (exported, handles `BOOT_COMPLETED` + `LOCKED_BOOT_COMPLETED`)
- service: [DownloadForegroundService.kt](app/src/main/java/com/abhinav/otapulse/core/download/DownloadForegroundService.kt) (foregroundServiceType=dataSync)
- service: `SystemForegroundService` (WorkManager foreground, foregroundServiceType=dataSync)
- provider: `FileProvider` with `@xml/file_paths`
- provider: `InitializationProvider` (removes WorkManagerInitializer for custom init)
- locale service: `AppLocalesMetadataHolderService` (disabled, auto locale storage)

[OtaPulseApplication.kt](app/src/main/java/com/abhinav/otapulse/app/OtaPulseApplication.kt) wires Hilt and WorkManager, and applies Dynamic Color when available.

**Manifest Permissions:**

- `INTERNET`, `ACCESS_NETWORK_STATE`
- `WRITE_EXTERNAL_STORAGE` (maxSdkVersion 29), `READ_EXTERNAL_STORAGE` (maxSdkVersion 29)
- `MANAGE_EXTERNAL_STORAGE`
- `POST_NOTIFICATIONS`
- `VIBRATE`
- `WAKE_LOCK`
- `FOREGROUND_SERVICE`, `FOREGROUND_SERVICE_DATA_SYNC`
- `RECEIVE_BOOT_COMPLETED`
- `REQUEST_IGNORE_BATTERY_OPTIMIZATIONS`

## Top-Level Source Layout

**Main Package Root**

```text
app/src/main/java/com/abhinav/otapulse/
|- app/
|- arb/
|- catalog/
|- core/
|- di/
|- feature/
`- ota/
```

This layout is still healthy. The codebase is not pure clean architecture and not purely feature-only either. In practice it mixes:

1. thin app shell code
2. feature-oriented presentation flows
3. specialized OTA / ARB engine code
4. a very large source-driven device catalog

That is a reasonable shape for this app as long as ownership stays explicit.

## Package Ownership

### `app`

This package is the application shell.

Key files:

- [MainActivity.kt](app/src/main/java/com/abhinav/otapulse/app/MainActivity.kt) (27 KB)
- [MainViewModel.kt](app/src/main/java/com/abhinav/otapulse/app/MainViewModel.kt)
- [OtaPulseApplication.kt](app/src/main/java/com/abhinav/otapulse/app/OtaPulseApplication.kt)

What it owns:

- app startup
- WorkManager configuration
- dynamic color setup
- bottom navigation
- manual fragment switching
- first-launch permission flow
- app-update dialog triggering
- top-level downloads FAB state

Current reality:

- `MainActivity` is the central coordinator for primary navigation.
- The shell knows about multiple features directly.
- This is simple and workable, but it means global UI orchestration is concentrated in one activity.

Rule:

- `app` should coordinate features, not become the home for feature business logic.

### `feature`

This package owns user-facing product flows. Current feature directories are:

- `about`
- `browser`
- `devices`
- `downloads`
- `history`
- `otatools`
- `settings`
- `updates`

#### `feature/devices`

This is the device-browsing side of the app and one of the busiest feature areas.

Structure:

```text
feature/devices/
|- domain/
|  |- FetchOtaDetailsUseCase.kt
|  |- FetchOtaUpdateUseCase.kt
|  |- GetDevicesUseCase.kt
|  `- ToggleFavoriteUseCase.kt
`- ui/
   |- AddDeviceFragment.kt
   |- AddDeviceViewModel.kt
   |- DeviceAdapter.kt
   |- DevicesFragment.kt
   |- DevicesViewModel.kt
   |- FirmwareGroupAdapter.kt
   |- OtaDetailsDialogFragment.kt
   |- OtaViewModel.kt
   `- RegionVariantEditorAdapter.kt
```

Responsibilities:

- device browsing, search, and filtering
- favorites (via `ToggleFavoriteUseCase`)
- custom device addition (via `AddDeviceFragment` / `AddDeviceViewModel`)
- OTA details presentation (via `OtaDetailsDialogFragment`)
- firmware group display
- region/variant editing
- entry into extraction-related flows

The domain layer has expanded with dedicated use cases for fetching OTA details, fetching updates, getting the device list, and toggling favorites.

Pressure point:

- `DevicesFragment` still carries significant orchestration responsibility. `OtaDetailsDialogFragment` at 24 KB is substantial and may benefit from future decomposition.

#### `feature/downloads`

This owns the download queue and downloaded file lifecycle.

Structure:

```text
feature/downloads/
|- data/
|  `- DownloadManager.kt
|- domain/
|  |- DeleteFileUseCase.kt
|  |- DownloadRepository.kt
|  |- EnqueueDownloadUseCase.kt
|  |- GetDownloadsUseCase.kt
|  `- GetTargetFileUseCase.kt
`- ui/
   |- DownloadAdapter.kt
   |- DownloadsFragment.kt
   `- DownloadsViewModel.kt
```

Responsibilities:

- enqueue / pause / resume / retry / cancel / delete
- target path generation
- download-state flow exposure
- download progress and completion notifications
- domain use cases for download operations

Current reality:

- [DownloadManager.kt](app/src/main/java/com/abhinav/otapulse/feature/downloads/data/DownloadManager.kt) (23 KB) acts as both repository implementation and callback hub. The download engine itself has moved to `core/download`.
- The domain layer now has proper use case separation.

#### `feature/history`

This is a **new feature** that tracks OTA query history.

Structure:

```text
feature/history/
|- data/
|  |- local/
|  |  |- OtaHistoryDao.kt
|  |  `- OtaHistoryEntity.kt
|  |- OtaHistoryRepository.kt
|  `- OtaHistoryRepositoryImpl.kt
`- ui/
   |- OtaHistoryAdapter.kt
   |- OtaHistoryFragment.kt
   `- OtaHistoryViewModel.kt
```

Responsibilities:

- persisting OTA query history entries using Room Database
- browsing and displaying past OTA lookups
- repository pattern with interface + implementation

#### `feature/updates`

This is a **new feature** for the home update experience.

Structure:

```text
feature/updates/
`- ui/
   `- HomeUpdateFragment.kt
```

Responsibilities:

- Home screen update flow (at 35 KB this is a substantial single-file feature)
- Primary update discovery and presentation

#### `feature/otatools`

This is the OTA tools hub rather than a single isolated screen.

Structure:

```text
feature/otatools/
|- data/
|  `- ArbLookupService.kt
`- ui/
   |- ArbCheckerFragment.kt
   |- JsonOutputActivity.kt
   |- LinkResolverFragment.kt
   |- ManualQueryFragment.kt
   |- OtaToolsFragment.kt
   `- PartitionExtractionFragment.kt
```

Plus a single shared ViewModel:

- [OtaToolsViewModel.kt](app/src/main/java/com/abhinav/otapulse/feature/otatools/ui/OtaToolsViewModel.kt) (17 KB)

Included tools:

- Manual Query
- Partition Extraction
- Link Resolver
- ARB Checker (new)
- JSON output / share flow

Architectural note:

- `ArbCheckerFragment` and `ArbLookupService` are new additions for ARB verification within the tools hub.
- `ManualQueryFragment` at 44 KB is the largest single fragment in the codebase.

Rule:

- keep OTA tools grouped under `feature/otatools`; do not scatter tool-specific presentation code into unrelated packages.

#### `feature/browser`

This feature is easy to miss, but it is real runtime code and should stay documented.

File:

- [InAppBrowserActivity.kt](app/src/main/java/com/abhinav/otapulse/feature/browser/InAppBrowserActivity.kt) (12 KB)

Responsibilities:

- in-app WebView browsing
- desktop-mode toggle
- browser controls visibility preference
- copy/share/open-external actions
- compact browser chrome for project and OTA links

This belongs in `feature`, not `core`, because it is user-facing product behavior.

#### `feature/settings`

This feature owns app preferences, data import/export, app update behavior, and libraries display.

Structure:

```text
feature/settings/
|- AppUpdateRepository.kt
|- AppUpdateRepositoryImpl.kt
|- CheckAppUpdateUseCase.kt
|- SettingsFragment.kt
|- SettingsViewModel.kt
`- libraries/
   |- LibrariesAdapter.kt
   `- LibrariesFragment.kt
```

Responsibilities:

- theme preferences
- advanced-mode toggle
- browser preferences
- ARB detection toggle
- custom-device backup import/export
- app update checks (via `AppUpdateRepository`)
- open-source libraries display (via `libraries/`)

#### `feature/about`

This is a lightweight presentation feature.

Files:

- [AboutFragment.kt](app/src/main/java/com/abhinav/otapulse/feature/about/AboutFragment.kt) (10 KB)
- [WhatsNewBottomSheet.kt](app/src/main/java/com/abhinav/otapulse/feature/about/WhatsNewBottomSheet.kt)

It should stay simple and avoid becoming a dump site for unrelated project metadata logic. `WhatsNewBottomSheet` presents release notes / changelog.

### `catalog`

This package owns device catalog parsing and user persistence (favorites, custom devices).

Structure:

```text
catalog/
|- model/
|  |- PredefinedDevice.kt
|  |- Region.kt
|  `- RegionData.kt
`- repository/
   |- CustomDeviceManager.kt
   |- DeviceCatalogParser.kt
   |- DeviceRepository.kt
   |- DeviceRepositoryImpl.kt
   `- FavoritesManager.kt
```

Architectural implication:

- the app is heavily JSON-driven for supported-device metadata, which is stored in the top-level `devices/` folder.
- `DeviceCatalogParser` parses the `realme.json`, `oneplus.json`, and `oppo.json` files from assets at runtime.
- `catalog` is a first-class subsystem, not a helper folder.

Rules:

- device JSON modifications happen in the `devices/` top-level directory, not within Android source code.
- favorites and custom device persistence stay in `catalog/repository`
- UI should consume catalog outputs rather than re-encoding catalog rules in fragments

### `ota`

This package owns OTA engine behavior.

Structure:

```text
ota/
|- engine/
|  |- LocalOtaAccess.kt
|  |- OtaExtractor.kt
|  |- OtaRepository.kt
|  `- OtaRepositoryImpl.kt
|- network/
|  |- OtaApi.kt
|  |- RangeHttpClient.kt
|  `- ServerCapabilityChecker.kt
|- payload/
|  |- PayloadExtractor.kt
|  `- PayloadManifest.kt
|- resume/
|  `- ExtractionState.kt
`- zip/
   `- ZipRemoteParser.kt
```

What it owns:

- OTA request execution (`OtaApi`, `OtaRepository`)
- local OTA access and extraction (`LocalOtaAccess`, `OtaExtractor`)
- range-request HTTP support (`RangeHttpClient`)
- server capability detection (`ServerCapabilityChecker`)
- remote ZIP access (`ZipRemoteParser`)
- payload extraction (`PayloadExtractor`, `PayloadManifest`)
- extraction resume state tracking (`ExtractionState`)

This package is cohesive enough that it could become its own module later, but that split is not necessary yet.

### `arb`

This package is the anti-rollback and extraction-safety subsystem.

Subpackages:

- `parser`
- `worker`

Files:

- [ArbChecker.kt](app/src/main/java/com/abhinav/otapulse/arb/parser/ArbChecker.kt)
- [XblConfigParser.kt](app/src/main/java/com/abhinav/otapulse/arb/parser/XblConfigParser.kt)
- [PartitionExtractorWorker.kt](app/src/main/java/com/abhinav/otapulse/arb/worker/PartitionExtractorWorker.kt)

What it owns:

- XBL parsing
- ARB checks
- extraction worker execution

Rule:

- keep rollback / safety-specific logic here instead of diluting it into generic feature code.

### `core`

This package owns shared infrastructure. It has grown significantly and now includes a custom download engine.

Current subpackages:

- `common`
- `database`
- `download`
- `model`
- `network`
- `notifications`
- `receiver`
- `ui`
- `worker`

#### `core/common`

Shared utility code:

- [AnimationUtils.kt](app/src/main/java/com/abhinav/otapulse/core/common/AnimationUtils.kt)
- [Crypto.kt](app/src/main/java/com/abhinav/otapulse/core/common/Crypto.kt)
- [DeviceUtils.kt](app/src/main/java/com/abhinav/otapulse/core/common/DeviceUtils.kt) (9 KB)
- [FormatUtils.kt](app/src/main/java/com/abhinav/otapulse/core/common/FormatUtils.kt)
- [HapticUtils.kt](app/src/main/java/com/abhinav/otapulse/core/common/HapticUtils.kt)
- [InAppBrowser.kt](app/src/main/java/com/abhinav/otapulse/core/common/InAppBrowser.kt)
- [LocaleHelper.kt](app/src/main/java/com/abhinav/otapulse/core/common/LocaleHelper.kt)
- [Mappers.kt](app/src/main/java/com/abhinav/otapulse/core/common/Mappers.kt)
- [Md5Verifier.kt](app/src/main/java/com/abhinav/otapulse/core/common/Md5Verifier.kt)
- [OtaJsonOutputHelper.kt](app/src/main/java/com/abhinav/otapulse/core/common/OtaJsonOutputHelper.kt)
- [PermissionHelper.kt](app/src/main/java/com/abhinav/otapulse/core/common/PermissionHelper.kt)

#### `core/database`

Room Database configuration:

- [AppDatabase.kt](app/src/main/java/com/abhinav/otapulse/core/database/AppDatabase.kt)
- [Converters.kt](app/src/main/java/com/abhinav/otapulse/core/database/Converters.kt)

#### `core/download`

Custom download engine (replaces the former Fetch library dependency):

- [DownloadError.kt](app/src/main/java/com/abhinav/otapulse/core/download/DownloadError.kt)
- [DownloadForegroundService.kt](app/src/main/java/com/abhinav/otapulse/core/download/DownloadForegroundService.kt)
- [DownloadListener.kt](app/src/main/java/com/abhinav/otapulse/core/download/DownloadListener.kt)
- [DownloadRecord.kt](app/src/main/java/com/abhinav/otapulse/core/download/DownloadRecord.kt)
- [DownloadStatus.kt](app/src/main/java/com/abhinav/otapulse/core/download/DownloadStatus.kt)
- [OkHttpDownloadEngine.kt](app/src/main/java/com/abhinav/otapulse/core/download/OkHttpDownloadEngine.kt) (24 KB)

This is a complete download subsystem: engine, foreground service, status tracking, error handling, and listener callbacks.

#### `core/model`

Shared domain models:

- [AppUpdateInfo.kt](app/src/main/java/com/abhinav/otapulse/core/model/AppUpdateInfo.kt)
- [Device.kt](app/src/main/java/com/abhinav/otapulse/core/model/Device.kt)
- [DownloadInfo.kt](app/src/main/java/com/abhinav/otapulse/core/model/DownloadInfo.kt)
- [DownloadMappers.kt](app/src/main/java/com/abhinav/otapulse/core/model/DownloadMappers.kt)
- [OtaError.kt](app/src/main/java/com/abhinav/otapulse/core/model/OtaError.kt)
- [OtaHistoryEntry.kt](app/src/main/java/com/abhinav/otapulse/core/model/OtaHistoryEntry.kt)
- [OtaRequest.kt](app/src/main/java/com/abhinav/otapulse/core/model/OtaRequest.kt)
- [OtaUpdate.kt](app/src/main/java/com/abhinav/otapulse/core/model/OtaUpdate.kt)
- [RegionVariant.kt](app/src/main/java/com/abhinav/otapulse/core/model/RegionVariant.kt)

#### `core/network`

Networking primitives:

- [Component.kt](app/src/main/java/com/abhinav/otapulse/core/network/Component.kt)
- [CustomOtaRequest.kt](app/src/main/java/com/abhinav/otapulse/core/network/CustomOtaRequest.kt)
- [Data.kt](app/src/main/java/com/abhinav/otapulse/core/network/Data.kt) (4 KB)
- [GitHubUpdater.kt](app/src/main/java/com/abhinav/otapulse/core/network/GitHubUpdater.kt)
- [OtaResolver.kt](app/src/main/java/com/abhinav/otapulse/core/network/OtaResolver.kt)
- [Request.kt](app/src/main/java/com/abhinav/otapulse/core/network/Request.kt) (13 KB)

#### `core/notifications`

- [DownloadNotificationHelper.kt](app/src/main/java/com/abhinav/otapulse/core/notifications/DownloadNotificationHelper.kt) (18 KB)

#### `core/receiver`

- [BootCompletedReceiver.kt](app/src/main/java/com/abhinav/otapulse/core/receiver/BootCompletedReceiver.kt)
- [DownloadActionReceiver.kt](app/src/main/java/com/abhinav/otapulse/core/receiver/DownloadActionReceiver.kt)

#### `core/ui`

Shared UI primitives:

- [DialogEffects.kt](app/src/main/java/com/abhinav/otapulse/core/ui/DialogEffects.kt)
- [WavyCircularProgressIndicator.kt](app/src/main/java/com/abhinav/otapulse/core/ui/WavyCircularProgressIndicator.kt) (10 KB)

#### `core/worker`

- [DownloadWorker.kt](app/src/main/java/com/abhinav/otapulse/core/worker/DownloadWorker.kt)
- [SoftwareUpdateCheckWorker.kt](app/src/main/java/com/abhinav/otapulse/core/worker/SoftwareUpdateCheckWorker.kt) (9 KB)

What belongs in `core`:

- shared models
- database definitions (Room database, type converters)
- cross-feature helpers and utilities
- shared notification code
- networking primitives used by multiple flows
- the download engine (used by downloads feature + workers)
- shared receivers and workers
- shared UI primitives

What does not belong here:

- screen-specific UI behavior
- feature-only business rules
- catalog-specific device definitions

Guardrail:

- if something is only used by one feature, prefer keeping it with that feature.

### `di`

This package wires dependencies.

Key files:

- [AppModule.kt](app/src/main/java/com/abhinav/otapulse/di/AppModule.kt)
- [DatabaseModule.kt](app/src/main/java/com/abhinav/otapulse/di/DatabaseModule.kt)
- [RepositoryModule.kt](app/src/main/java/com/abhinav/otapulse/di/RepositoryModule.kt)
- [Qualifiers.kt](app/src/main/java/com/abhinav/otapulse/di/Qualifiers.kt)

What it currently provides or binds:

- Gson
- Room database and DAOs
- `OtaExtractor`
- `OkHttpClient`
- favorites and custom-device shared preferences
- repository bindings for app update, OTA, history, device catalog, and downloads
- Hilt qualifier annotations for disambiguating injected types

Rule:

- keep DI files focused on wiring, not business logic.

## Runtime Flow

The current runtime path is roughly:

1. [OtaPulseApplication.kt](app/src/main/java/com/abhinav/otapulse/app/OtaPulseApplication.kt) initializes Hilt and WorkManager.
2. [MainActivity.kt](app/src/main/java/com/abhinav/otapulse/app/MainActivity.kt) owns the shell UI, permission prompts, bottom navigation, and fragment swapping.
3. Feature UI talks to feature view models, use cases, or repository interfaces.
4. Device information comes from `catalog`.
5. OTA request and extraction behavior flows through `ota`.
6. ARB and extraction safety checks flow through `arb`.
7. Downloads are executed via the custom `core/download` engine running in a foreground service.
8. Shared notifications, models, and utility code come from `core`.
9. `BootCompletedReceiver` reschedules background work (e.g., software update checks) after device boot.
10. `SoftwareUpdateCheckWorker` runs periodic background checks for device firmware updates.

This is not strict layered purity, but it is understandable and productive.

## Resource Layer

The resource tree is mature and supports extensive layout, drawable, and localization buckets:

**Layouts:**

- `layout` — default portrait layouts
- `layout-land` — landscape overrides
- `layout-sw600dp` — small-width tablet layouts
- `layout-w600dp` — width-qualified tablet layouts

**Drawables:**

- `drawable` — vector drawables, shapes, and selectors
- `drawable-hdpi` through `drawable-xxxhdpi` — density-specific raster assets

**Mipmaps:**

- `mipmap-anydpi`, `mipmap-anydpi-v26` — adaptive icon
- `mipmap-hdpi` through `mipmap-xxxhdpi` — launcher icons at all density buckets

**Values / Theming:**

- `values` / `values-night` — light and dark theme definitions
- `values-v31` / `values-night-v31` — Material You overrides for Android 12+

**Localization (20+ locales):**

- `values-ar`, `values-bn`, `values-de`, `values-es`, `values-fil`, `values-fr`, `values-hi`, `values-id`, `values-in`, `values-it`, `values-ja`, `values-ms`, `values-pt`, `values-pt-rBR`, `values-pt-rPT`, `values-ru`, `values-th`, `values-tl`, `values-tr`, `values-ur`, `values-vi`, `values-zh`, `values-zh-rCN`, `values-zh-rTW`

**Other:**

- `anim` — view animations
- `animator` — property animators
- `color` — color state lists
- `menu` — bottom-nav and toolbar menus
- `navigation` — nav graph resource (not yet driving primary navigation)
- `xml` — file provider paths, backup rules, and preferences

Important nuance:

- a `navigation` resource directory exists, but the current user flow is still driven by manual fragment transactions in `MainActivity`.

## Testing Reality

Current tests are light.

Unit tests present:

- [ExampleUnitTest.kt](app/src/test/java/com/abhinav/otapulse/ExampleUnitTest.kt)
- [CryptoTest.kt](app/src/test/java/com/abhinav/otapulse/util/CryptoTest.kt)
- [FormatUtilsTest.kt](app/src/test/java/com/abhinav/otapulse/util/FormatUtilsTest.kt)
- [RequestTest.kt](app/src/test/java/com/abhinav/otapulse/util/RequestTest.kt)

Instrumented tests present:

- [ExampleInstrumentedTest.kt](app/src/androidTest/java/com/abhinav/otapulse/ExampleInstrumentedTest.kt)

Notable leftover:

- the unit-test package path still uses the old `util` namespace even though production code has moved on.

## Current Pressure Points

### 1. `MainActivity`

It owns:

- tab switching
- permission prompts
- update dialogs
- downloads FAB state
- fragment selection rules

This is efficient today, but it means top-level navigation and shell behavior are tightly coupled.

### 2. `HomeUpdateFragment`

At 35 KB in a single file, this is the largest fragment in the codebase. It handles the entire home update flow and is a strong candidate for decomposition into smaller composable pieces.

### 3. `ManualQueryFragment`

At 44 KB this is the largest single fragment file in the entire project. It carries substantial orchestration and UI logic that could benefit from extraction into helper classes or sub-components.

### 4. `OtaDetailsDialogFragment`

At 24 KB this dialog fragment handles a lot of OTA details presentation. Future wins include splitting extraction-related UI state and isolating permission-related branching.

### 5. `DownloadManager`

This class does real work and sits on an important boundary:

- download engine callback handling
- storage path logic
- notification updates
- state publication (uses atomic `update` to prevent concurrency issues)

It is a strong candidate for careful incremental decomposition if complexity grows.

### 6. `OkHttpDownloadEngine`

At 24 KB this is the core of the custom download system. It handles:

- HTTP range requests
- resume support
- progress reporting
- error handling

This replaced the Fetch library and is critical infrastructure.

### 7. `catalog`

The catalog is now JSON-driven, meaning device support is simple data entry in the `devices/` folder rather than code maintenance. The `DeviceCatalogParser` generates complex firmware versions and region mappings at runtime to keep the JSON footprint small.

### 8. `core`

`core` has grown substantially with the addition of the download engine, expanded utilities, and shared UI components. It still needs discipline to stay organized.

Guardrail:

- only move code into `core` when it is truly shared across multiple feature or subsystem boundaries.

## Placement Rules

When adding code:

1. Put user-facing flow code in the owning `feature`.
2. Put new device definitions in the top-level `devices/` JSON files, and catalog logic/persistence in `catalog/`.
3. Put OTA engine logic in `ota`.
4. Put rollback and extraction-safety logic in `arb`.
5. Put app shell coordination in `app`.
6. Put shared infrastructure in `core` only when it is actually shared.
7. Keep `di` focused on wiring.
8. Treat the root landing page and `OTAPulse/` assets as separate web collateral, not Android runtime code.

## Final Principle

OTA Pulse stays maintainable when ownership is obvious:

- `app` runs the shell
- `feature` owns product flows (`about`, `browser`, `devices`, `downloads`, `history`, `otatools`, `settings`, `updates`)
- `catalog` owns parsing supported devices and persistence
- `ota` owns OTA engine behavior
- `arb` owns rollback and extraction safety
- `core` owns shared infrastructure (including the download engine)
- `di` wires dependencies together

If new code does not clearly belong to one of those owners, stop and decide before adding it.

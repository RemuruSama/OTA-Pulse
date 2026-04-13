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

This repository now contains two distinct pieces:

1. the Android product in [app](C:/Users/abhin/AndroidStudioProjects/Otaupdater/app)
2. a static landing page in [index.html](C:/Users/abhin/AndroidStudioProjects/Otaupdater/index.html) with assets in [OTAPulse](C:/Users/abhin/AndroidStudioProjects/Otaupdater/OTAPulse)

The rest of this document focuses on the Android app, because that is where the runtime and package boundaries matter most.

## Build Snapshot

From [app/build.gradle.kts](C:/Users/abhin/AndroidStudioProjects/Otaupdater/app/build.gradle.kts):

- single Android application module
- namespace `com.abhinav.otapulse`
- Kotlin + Java 17
- `minSdk = 29`
- `targetSdk = 35`
- `compileSdk = 36`
- `versionName = 3.0.3`
- ViewBinding enabled
- Hilt + KSP
- WorkManager
- Fetch
- OkHttp
- Glide
- Markwon
- Gson
- Flexbox
- Protobuf Lite
- XZ and Commons Compress for OTA payload / extraction work

**Important Nuance**

- Navigation dependencies exist in Gradle, but the app shell currently uses manual fragment transactions instead of a NavHost-driven flow.

## App Entry Points

**From** [AndroidManifest.xml](C:/Users/abhin/AndroidStudioProjects/Otaupdater/app/src/main/AndroidManifest.xml):

- application class: [OtaPulseApplication.kt](C:/Users/abhin/AndroidStudioProjects/Otaupdater/app/src/main/java/com/abhinav/otapulse/app/OtaPulseApplication.kt)
- launcher activity: [MainActivity.kt](C:/Users/abhin/AndroidStudioProjects/Otaupdater/app/src/main/java/com/abhinav/otapulse/app/MainActivity.kt)
- secondary activity: [InAppBrowserActivity.kt](C:/Users/abhin/AndroidStudioProjects/Otaupdater/app/src/main/java/com/abhinav/otapulse/feature/browser/InAppBrowserActivity.kt)
- secondary activity: [JsonOutputActivity.kt](C:/Users/abhin/AndroidStudioProjects/Otaupdater/app/src/main/java/com/abhinav/otapulse/feature/otatools/ui/JsonOutputActivity.kt)
- shared receiver: [DownloadActionReceiver.kt](C:/Users/abhin/AndroidStudioProjects/Otaupdater/app/src/main/java/com/abhinav/otapulse/core/receiver/DownloadActionReceiver.kt)

[OtaPulseApplication.kt](C:/Users/abhin/AndroidStudioProjects/Otaupdater/app/src/main/java/com/abhinav/otapulse/app/OtaPulseApplication.kt) wires Hilt and WorkManager, and applies Dynamic Color when available.

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

- [MainActivity.kt](C:/Users/abhin/AndroidStudioProjects/Otaupdater/app/src/main/java/com/abhinav/otapulse/app/MainActivity.kt)
- [MainViewModel.kt](C:/Users/abhin/AndroidStudioProjects/Otaupdater/app/src/main/java/com/abhinav/otapulse/app/MainViewModel.kt)
- [OtaPulseApplication.kt](C:/Users/abhin/AndroidStudioProjects/Otaupdater/app/src/main/java/com/abhinav/otapulse/app/OtaPulseApplication.kt)

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
- `otatools`
- `settings`

#### `feature/devices`

This is the device-browsing side of the app and one of the busiest feature areas.

Representative files:

- [DeviceFragment.kt](C:/Users/abhin/AndroidStudioProjects/Otaupdater/app/src/main/java/com/abhinav/otapulse/feature/devices/ui/DeviceFragment.kt)
- [DevicesFragment.kt](C:/Users/abhin/AndroidStudioProjects/Otaupdater/app/src/main/java/com/abhinav/otapulse/feature/devices/ui/DevicesFragment.kt)
- [DevicesViewModel.kt](C:/Users/abhin/AndroidStudioProjects/Otaupdater/app/src/main/java/com/abhinav/otapulse/feature/devices/ui/DevicesViewModel.kt)
- [FetchOtaUpdateUseCase.kt](C:/Users/abhin/AndroidStudioProjects/Otaupdater/app/src/main/java/com/abhinav/otapulse/feature/devices/domain/FetchOtaUpdateUseCase.kt)

Responsibilities:

- device browsing
- search and filtering
- favorites
- custom device handling
- OTA details presentation
- entry into extraction-related flows

Pressure point:

- `DevicesFragment` still carries a lot of orchestration responsibility and is a likely future split point.

#### `feature/downloads`

This owns the download queue and downloaded file lifecycle.

Representative files:

- [DownloadManager.kt](C:/Users/abhin/AndroidStudioProjects/Otaupdater/app/src/main/java/com/abhinav/otapulse/feature/downloads/data/DownloadManager.kt)
- [DownloadsFragment.kt](C:/Users/abhin/AndroidStudioProjects/Otaupdater/app/src/main/java/com/abhinav/otapulse/feature/downloads/ui/DownloadsFragment.kt)
- [DownloadsViewModel.kt](C:/Users/abhin/AndroidStudioProjects/Otaupdater/app/src/main/java/com/abhinav/otapulse/feature/downloads/ui/DownloadsViewModel.kt)

Responsibilities:

- enqueue / pause / resume / retry / cancel / delete
- target path generation under external storage
- download-state flow exposure
- Fetch listener callbacks
- download progress and completion notifications

Current reality:

- [DownloadManager.kt](C:/Users/abhin/AndroidStudioProjects/Otaupdater/app/src/main/java/com/abhinav/otapulse/feature/downloads/data/DownloadManager.kt) acts as both repository implementation and callback hub.
- It is cohesive, but it is dense and important enough to deserve careful future edits.

#### `feature/otatools`

This is the OTA tools hub rather than a single isolated screen.

Representative files:

- [OtaToolsFragment.kt](C:/Users/abhin/AndroidStudioProjects/Otaupdater/app/src/main/java/com/abhinav/otapulse/feature/otatools/ui/OtaToolsFragment.kt)
- [ManualQueryFragment.kt](C:/Users/abhin/AndroidStudioProjects/Otaupdater/app/src/main/java/com/abhinav/otapulse/feature/otatools/ui/ManualQueryFragment.kt)
- [PartitionExtractionFragment.kt](C:/Users/abhin/AndroidStudioProjects/Otaupdater/app/src/main/java/com/abhinav/otapulse/feature/otatools/ui/PartitionExtractionFragment.kt)
- [LinkResolverFragment.kt](C:/Users/abhin/AndroidStudioProjects/Otaupdater/app/src/main/java/com/abhinav/otapulse/feature/otatools/ui/LinkResolverFragment.kt)
- [JsonOutputActivity.kt](C:/Users/abhin/AndroidStudioProjects/Otaupdater/app/src/main/java/com/abhinav/otapulse/feature/otatools/ui/JsonOutputActivity.kt)

Included tools:

- Manual Query
- Partition Extraction
- Link Resolver
- JSON output / share flow

Rule:

- keep OTA tools grouped under `feature/otatools`; do not scatter tool-specific presentation code into unrelated packages.

#### `feature/browser`

This feature is easy to miss, but it is real runtime code and should stay documented.

Representative file:

- [InAppBrowserActivity.kt](C:/Users/abhin/AndroidStudioProjects/Otaupdater/app/src/main/java/com/abhinav/otapulse/feature/browser/InAppBrowserActivity.kt)

Responsibilities:

- in-app WebView browsing
- desktop-mode toggle
- browser controls visibility preference
- copy/share/open-external actions
- compact browser chrome for project and OTA links

This belongs in `feature`, not `core`, because it is user-facing product behavior.

#### `feature/settings`

This feature owns app preferences, data import/export, and app update behavior.

Representative files:

- [SettingsFragment.kt](C:/Users/abhin/AndroidStudioProjects/Otaupdater/app/src/main/java/com/abhinav/otapulse/feature/settings/SettingsFragment.kt)
- [SettingsViewModel.kt](C:/Users/abhin/AndroidStudioProjects/Otaupdater/app/src/main/java/com/abhinav/otapulse/feature/settings/SettingsViewModel.kt)
- [CheckAppUpdateUseCase.kt](C:/Users/abhin/AndroidStudioProjects/Otaupdater/app/src/main/java/com/abhinav/otapulse/feature/settings/CheckAppUpdateUseCase.kt)

Responsibilities:

- theme preferences
- advanced-mode toggle
- browser preferences
- ARB detection toggle
- custom-device backup import/export
- app update checks
- library / developer / contributors entry points

#### `feature/about`

This is a lightweight presentation feature.

It should stay simple and avoid becoming a dump site for unrelated project metadata logic.

### `catalog`

This package owns device definitions and catalog-backed persistence.

Key files:

- [DeviceCatalog.kt](C:/Users/abhin/AndroidStudioProjects/Otaupdater/app/src/main/java/com/abhinav/otapulse/catalog/DeviceCatalog.kt)
- [PredefinedDevice.kt](C:/Users/abhin/AndroidStudioProjects/Otaupdater/app/src/main/java/com/abhinav/otapulse/catalog/model/PredefinedDevice.kt)
- [DeviceRepository.kt](C:/Users/abhin/AndroidStudioProjects/Otaupdater/app/src/main/java/com/abhinav/otapulse/catalog/repository/DeviceRepository.kt)
- [DeviceRepositoryImpl.kt](C:/Users/abhin/AndroidStudioProjects/Otaupdater/app/src/main/java/com/abhinav/otapulse/catalog/repository/DeviceRepositoryImpl.kt)
- [CustomDeviceManager.kt](C:/Users/abhin/AndroidStudioProjects/Otaupdater/app/src/main/java/com/abhinav/otapulse/catalog/repository/CustomDeviceManager.kt)

Provider groups:

- `provider/oneplus`
- `provider/realme`
- `provider/oppo`

Current scale from the source tree:

- 34 OnePlus provider files
- 99 Realme provider files
- 1 OPPO provider file

Architectural implication:

- the app is heavily source-driven for supported-device metadata
- `catalog` is a first-class subsystem, not a helper folder

Rules:

- built-in device definitions stay in `catalog`
- favorites and custom device persistence stay in `catalog`
- UI should consume catalog outputs rather than re-encoding catalog rules in fragments

### `ota`

This package owns OTA engine behavior.

Subpackages:

- `engine`
- `network`
- `payload`
- `resume`
- `zip`

Representative files:

- [OtaRepository.kt](C:/Users/abhin/AndroidStudioProjects/Otaupdater/app/src/main/java/com/abhinav/otapulse/ota/engine/OtaRepository.kt)
- [OtaRepositoryImpl.kt](C:/Users/abhin/AndroidStudioProjects/Otaupdater/app/src/main/java/com/abhinav/otapulse/ota/engine/OtaRepositoryImpl.kt)
- [OtaExtractor.kt](C:/Users/abhin/AndroidStudioProjects/Otaupdater/app/src/main/java/com/abhinav/otapulse/ota/engine/OtaExtractor.kt)
- [PayloadExtractor.kt](C:/Users/abhin/AndroidStudioProjects/Otaupdater/app/src/main/java/com/abhinav/otapulse/ota/payload/PayloadExtractor.kt)
- [ZipRemoteParser.kt](C:/Users/abhin/AndroidStudioProjects/Otaupdater/app/src/main/java/com/abhinav/otapulse/ota/zip/ZipRemoteParser.kt)

What it owns:

- OTA request execution
- OTA metadata mapping
- remote ZIP access
- payload extraction
- resume-oriented OTA file work

This package is cohesive enough that it could become its own module later, but that split is not necessary yet.

### `arb`

This package is the anti-rollback and extraction-safety subsystem.

Subpackages:

- `parser`
- `worker`

Representative files:

- [ArbChecker.kt](C:/Users/abhin/AndroidStudioProjects/Otaupdater/app/src/main/java/com/abhinav/otapulse/arb/parser/ArbChecker.kt)
- [XblConfigParser.kt](C:/Users/abhin/AndroidStudioProjects/Otaupdater/app/src/main/java/com/abhinav/otapulse/arb/parser/XblConfigParser.kt)
- [PartitionExtractorWorker.kt](C:/Users/abhin/AndroidStudioProjects/Otaupdater/app/src/main/java/com/abhinav/otapulse/arb/worker/PartitionExtractorWorker.kt)

What it owns:

- XBL parsing
- ARB checks
- extraction worker execution

Rule:

- keep rollback / safety-specific logic here instead of diluting it into generic feature code.

### `core`

This package owns shared infrastructure.

Current subpackages:

- `common`
- `model`
- `network`
- `notifications`
- `receiver`
- `ui`
- `worker`

Representative files:

- [FormatUtils.kt](C:/Users/abhin/AndroidStudioProjects/Otaupdater/app/src/main/java/com/abhinav/otapulse/core/common/FormatUtils.kt)
- [OtaRequest.kt](C:/Users/abhin/AndroidStudioProjects/Otaupdater/app/src/main/java/com/abhinav/otapulse/core/model/OtaRequest.kt)
- [OtaResolver.kt](C:/Users/abhin/AndroidStudioProjects/Otaupdater/app/src/main/java/com/abhinav/otapulse/core/network/OtaResolver.kt)
- [DownloadNotificationHelper.kt](C:/Users/abhin/AndroidStudioProjects/Otaupdater/app/src/main/java/com/abhinav/otapulse/core/notifications/DownloadNotificationHelper.kt)

What belongs here:

- shared models
- cross-feature helpers
- shared notification code
- networking primitives used by multiple flows
- shared receivers and workers

What does not belong here:

- screen-specific UI behavior
- feature-only business rules
- catalog-specific device definitions

Guardrail:

- if something is only used by one feature, prefer keeping it with that feature.

### `di`

This package wires dependencies.

Key files:

- [AppModule.kt](C:/Users/abhin/AndroidStudioProjects/Otaupdater/app/src/main/java/com/abhinav/otapulse/di/AppModule.kt)
- [RepositoryModule.kt](C:/Users/abhin/AndroidStudioProjects/Otaupdater/app/src/main/java/com/abhinav/otapulse/di/RepositoryModule.kt)

What it currently provides or binds:

- Fetch instance and notification manager hookup
- Gson
- `OtaExtractor`
- `OkHttpClient`
- favorites and custom-device shared preferences
- repository bindings for app update, OTA, device catalog, and downloads

Rule:

- keep DI files focused on wiring, not business logic.

## Runtime Flow

The current runtime path is roughly:

1. [OtaPulseApplication.kt](C:/Users/abhin/AndroidStudioProjects/Otaupdater/app/src/main/java/com/abhinav/otapulse/app/OtaPulseApplication.kt) initializes Hilt and WorkManager.
2. [MainActivity.kt](C:/Users/abhin/AndroidStudioProjects/Otaupdater/app/src/main/java/com/abhinav/otapulse/app/MainActivity.kt) owns the shell UI, permission prompts, bottom navigation, and fragment swapping.
3. Feature UI talks to feature view models, use cases, or repository interfaces.
4. Device information comes from `catalog`.
5. OTA request and extraction behavior flows through `ota`.
6. ARB and extraction safety checks flow through `arb`.
7. Shared notifications, models, and utility code come from `core`.

This is not strict layered purity, but it is understandable and productive.

## Resource Layer

The resource tree is fairly mature and supports several layout buckets:

- `layout`
- `layout-land`
- `layout-sw600dp`
- `layout-w600dp`
- `values`
- `values-night`
- `values-v31`
- `values-night-v31`
- `anim`
- `animator`
- `menu`
- `navigation`
- `xml`

Important nuance:

- a `navigation` resource directory exists, but the current user flow is still driven by manual fragment transactions in `MainActivity`.

## Testing Reality

Current tests are light.

Unit tests present:

- [ExampleUnitTest.kt](C:/Users/abhin/AndroidStudioProjects/Otaupdater/app/src/test/java/com/abhinav/otapulse/ExampleUnitTest.kt)
- [CryptoTest.kt](C:/Users/abhin/AndroidStudioProjects/Otaupdater/app/src/test/java/com/abhinav/otapulse/util/CryptoTest.kt)
- [FormatUtilsTest.kt](C:/Users/abhin/AndroidStudioProjects/Otaupdater/app/src/test/java/com/abhinav/otapulse/util/FormatUtilsTest.kt)
- [RequestTest.kt](C:/Users/abhin/AndroidStudioProjects/Otaupdater/app/src/test/java/com/abhinav/otapulse/util/RequestTest.kt)

Instrumented tests present:

- [ExampleInstrumentedTest.kt](C:/Users/abhin/AndroidStudioProjects/Otaupdater/app/src/androidTest/java/com/abhinav/otapulse/ExampleInstrumentedTest.kt)

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

### 2. `DevicesFragment`

This feature area still looks like one of the largest UI orchestration points in the app.

Likely future wins:

- split OTA details dialog logic
- split extraction-related UI state
- isolate permission-related branching where possible

### 3. `DownloadManager`

This class does real work and sits on an important boundary:

- Fetch callback handling
- storage path logic
- notification updates
- state publication

It is a strong candidate for careful incremental decomposition if complexity grows.

### 4. `catalog`

The catalog is large by source file count. That is acceptable for now, but it does mean device support is code maintenance rather than data entry.

Future option:

- move some provider metadata toward structured assets or generation if scale becomes painful.

### 5. `core`

`core` is healthier than an old-style `util` bucket, but it still needs discipline to stay that way.

Guardrail:

- only move code into `core` when it is truly shared across multiple feature or subsystem boundaries.

## Placement Rules

When adding code:

1. Put user-facing flow code in the owning `feature`.
2. Put device definitions and related persistence in `catalog`.
3. Put OTA engine logic in `ota`.
4. Put rollback and extraction-safety logic in `arb`.
5. Put app shell coordination in `app`.
6. Put shared infrastructure in `core` only when it is actually shared.
7. Keep `di` focused on wiring.
8. Treat the root landing page and `OTAPulse/` assets as separate web collateral, not Android runtime code.

## Final Principle

OTA Pulse stays maintainable when ownership is obvious:

- `app` runs the shell
- `feature` owns product flows
- `catalog` owns supported devices
- `ota` owns OTA engine behavior
- `arb` owns rollback and extraction safety
- `core` owns shared infrastructure
- `di` wires dependencies together

If new code does not clearly belong to one of those owners, stop and decide before adding it.

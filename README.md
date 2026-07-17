# OTA Pulse

OTA Pulse is an Android app for discovering, downloading, and inspecting OTA packages for supported `OnePlus`, `Realme`, and `OPPO` devices. This repository also includes a static landing page at [index.html](https://remurusama.github.io/OTA-Pulse/).

> Request and download OTA packages from BBK server(s).

## What the App Does

- Browse supported devices from built-in JSON catalog (148+ device definitions across OnePlus, Realme, and OPPO)
- Mark favorite devices for quick access
- Fetch and view OTA update metadata with rich details and partition extraction options
- Download OTA packages with progress notifications (custom OkHttp download engine with pause/resume support)
- Inspect OTA links using built-in edge-to-edge browser
- Advanced tools:
  - Manual Query
  - Partition Extraction
  - Link Resolver
  - ARB Checker
- Import / export custom device definitions and settings
- Add custom device definitions
- OTA query history tracking
- Home update discovery flow with hardware model & SoC identification
- Check for OTA Pulse app updates with background software update checks and boot persistence
- 25+ localized regional language translations
- High reliability with robust error handling, leak-free streams, thread-safe data persistence, and low-end hardware guards

## Tech Stack

- Kotlin (Android)
- Jetpack Compose (100% Compose UI with Material Design 3 and Material Kolor dynamic theming)
- minSdk 29 • targetSdk 37 • compileSdk 37
- Coroutines & Flow (Reactive state management)
- Hilt (Dependency Injection)
- WorkManager (with Hilt integration)
- Room Database (Local Persistence)
- OkHttp (Networking + Custom Download Engine with resume capabilities)
- Markwon, Gson, Flexbox, Brotli
- Protobuf Lite
- XZ / Apache Commons Compress
- AndroidX Navigation Compose, Lifecycle Compose, Dynamic Animation

## Project Structure

```text
Otaupdater/
├─ app/
│  ├─ build.gradle.kts
│  └─ src/main/
│     ├─ java/com/abhinav/otapulse/
│     │  ├─ app/             # Application shell, MainActivity, MainViewModel, OtaPulseApp (Compose root)
│     │  ├─ arb/             # Anti-rollback & extraction safety
│     │  ├─ catalog/         # Device catalog logic & user persistence
│     │  │  ├─ model/        # Shared models (PredefinedDevice, Region, etc.)
│     │  │  └─ repository/   # JSON parsing, favorites, custom devices
│     │  ├─ core/            # Shared infrastructure
│     │  │  ├─ common/       # Utilities, crypto, permissions, mappers, OTA card & share helpers
│     │  │  ├─ database/     # Room database configuration & type converters
│     │  │  ├─ download/     # Custom OkHttp download engine & foreground service
│     │  │  ├─ model/        # Shared domain models
│     │  │  ├─ network/      # Networking primitives, OTA resolver, GitHub updater, app downloader
│     │  │  ├─ notifications/ # Download notification helpers
│     │  │  ├─ preferences/  # App & theme settings datastores and preferences
│     │  │  ├─ receiver/     # Boot & download action receivers
│     │  │  ├─ ui/           # Shared Compose design system components & holographic theme
│     │  │  └─ worker/       # Background workers (downloads, update checks)
│     │  ├─ di/              # Hilt dependency injection modules
│     │  ├─ feature/         # User-facing product flows & Compose screens
│     │  │  ├─ about/        # App info, social profiles, what's new sheet
│     │  │  ├─ browser/      # In-app WebView browser activity and screen
│     │  │  ├─ devicecatalog/ # Compose presentation screens (catalog browsing, OTA details sheet, add device)
│     │  │  ├─ devices/      # Domain use cases & ViewModels for device catalog
│     │  │  ├─ downloads/    # Download queue, file lifecycle, Compose downloads screen
│     │  │  ├─ history/      # OTA query history tracking & browsing
│     │  │  ├─ otatools/     # Manual Query, Partition Extraction, Link Resolver, ARB Checker screens
│     │  │  ├─ settings/     # Theme, browser prefs, ARB toggle, import/export, libraries
│     │  │  └─ updates/      # Home update discovery screen & state
│     │  ├─ navigation/      # Compose Navigation host (OtaPulseNavGraph), screen routes & transitions
│     │  └─ ota/             # OTA engine, payload, remote ZIP, resume
│     └─ res/                # Drawables, layouts, and 25+ locale translations
├─ OTAPulse/
│  ├─ logo/                  # App logo assets
│  └─ Screenshot/            # Store/README screenshots
├─ devices/                  # JSON device catalog (oneplus.json, oppo.json, realme.json)
├─ gradle/
│  └─ libs.versions.toml     # Version catalog
├─ architecture.md
├─ index.html                # Static landing page
└─ settings.gradle.kts
```

See [architecture.md](architecture.md) for the deeper package map, ownership rules, and runtime flow.

## Android Features

- `feature/devicecatalog` & `feature/devices` — device browsing, region/variant filtering, favorites, OTA details sheet, custom device addition
- `feature/downloads` — download queue with custom OkHttp engine, pause/resume flow, file lifecycle management
- `feature/history` — Room-backed OTA query history tracking and browsing
- `feature/updates` — home screen update discovery card with device codename and SoC details
- `feature/otatools` — Manual Query, Partition Extraction, Link Resolver, and ARB Checker
- `feature/browser` — edge-to-edge in-app WebView browser
- `feature/settings` — holographic & monochrome theme preferences, browser settings, ARB toggle, import/export, open-source libraries
- `feature/about` — project info, developer profiles, what's new changelog

## Setup

1. Open the project in Android Studio.
2. Create `keystore.properties` in the project root if you want signed release builds.
3. Add:

```properties
STORE_PASSWORD=your_store_password
KEY_PASSWORD=your_key_password
```

4. Sync Gradle.
5. Run the `app` configuration on an Android 10+ device or emulator.

## Notes for Contributors

- keep device definitions in the top-level `devices/` JSON files and parser logic in `catalog/`
- keep user-facing screens and UI components in `feature` (or `core/ui/components` if shared across multiple features)
- keep navigation definitions and route declarations in `navigation/`
- keep OTA parsing / extraction logic in `ota` and `arb`
- move code into `core` only when it is genuinely shared across feature boundaries
- the download engine lives in `core/download` — keep download infrastructure there
- treat the root landing page as a separate web asset, not part of the Android runtime

## Credits

- [R0rt1z2/realme-ota](https://github.com/R0rt1z2/realme-ota)

## Support

> If you find this project useful, consider giving it a **star ⭐ on GitHub** — it helps improve visibility and SEO!
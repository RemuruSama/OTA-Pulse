# OTA Pulse

OTA Pulse is an Android app for discovering, downloading, and inspecting OTA packages for supported `OnePlus`, `Realme`, and `OPPO` devices. This repository also now includes a static landing page at [index.html](https://remurusama.github.io/OTA-Pulse/).

> Request and download OTA packages from BBK server(s).

## What the App Does

- Browse supported devices from built-in catalog
- Mark favorite devices for quick access
- Fetch and view OTA update metadata
- Download OTA packages with progress notifications
- Inspect OTA links using built-in browser
- Advanced tools:
  - Manual Query
  - Partition Extraction
  - Link Resolver
- Import / export custom device definitions
- Check for OTA Pulse app updates

## Tech Stack

- Kotlin (Android)
- minSdk 29 • targetSdk 35 • compileSdk 36
- ViewBinding UI
- Hilt (Dependency Injection)
- WorkManager (with Hilt integration)
- Fetch (Download Manager)
- OkHttp (Networking)
- Glide, Markwon, Gson, Flexbox
- Protobuf Lite
- XZ / Apache Commons Compress

## Project Structure

```text
Otaupdater/
├─ app/
│  ├─ build.gradle.kts
│  └─ src/main/
│     ├─ java/com/abhinav/otapulse/
│     │  ├─ app/
│     │  ├─ arb/
│     │  ├─ catalog/
│     │  ├─ core/
│     │  ├─ di/
│     │  ├─ feature/
│     │  └─ ota/
│     └─ res/
├─ OTAPulse/
│  ├─ logo/
│  └─ Screenshot/
├─ architecture.md
├─ index.html
└─ settings.gradle.kts
```

See [architecture.md](C:/Users/abhin/AndroidStudioProjects/Otaupdater/architecture.md) for the deeper package map and ownership rules.

## Android Features

- `feature/devices`: home device flow, browsing, variants, OTA details
- `feature/downloads`: download queue and file lifecycle
- `feature/otatools`: Manual Query, Partition Extraction, Link Resolver
- `feature/browser`: in-app WebView browser
- `feature/settings`: theme, browser preferences, etc.
- `feature/about`: project information

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

- keep device definitions and provider mappings in `catalog`
- keep user-facing flows in `feature`
- keep OTA parsing / extraction logic in `ota` and `arb`
- move code into `core` only when it is genuinely shared
- treat the root landing page as a separate web asset, not part of the Android runtime

## Credits

- [R0rt1z2/realme-ota](https://github.com/R0rt1z2/realme-ota)

## Support

> If you find this project useful, consider giving it a **star ⭐ on GitHub** — it helps improve visibility and SEO!
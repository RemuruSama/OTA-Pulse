<div align="center">

<img src="https://github.com/CodeSenseiX/OTAPulse/raw/main/OTAPulse/logo/logo.webp" width="120" alt="OTA Pulse Logo"/>

# 🚀 OTA Pulse

### Android OTA Update Downloader & Payload Extractor for Realme, OPPO, OnePlus

<b>![total-download-count](https://img.shields.io/github/downloads/RemuruSama/OTA-Pulse/total?color=brightgreen)<b>

![Platform](https://img.shields.io/badge/Platform-Android-green)
![Min Android](https://img.shields.io/badge/Android-10%2B-blue)
![Language](https://img.shields.io/badge/Kotlin-100%25-orange)
![UI](https://img.shields.io/badge/UI-Jetpack%20Compose-purple)
![License](https://img.shields.io/badge/License-Open%20Source-brightgreen)


**[⬇️ Download APK](https://github.com/RemuruSama/OTA-Pulse/releases) • [📢 Telegram Channel](https://t.me/abhinav_v1) • [🐛 Report Issue](https://github.com/RemuruSama/OTA-Pulse/issues)**

</div>

---

## 📱 What is OTA Pulse?

**OTA Pulse** is a free, open-source Android app that lets you **fetch, download, and extract official OTA firmware updates** for devices running ColorOS / RealmeUI / OxygenOS — including **Realme, OPPO, and OnePlus** phones.

Unlike stock OTA tools, OTA Pulse lets you:
- Fetch OTA packages for **any supported device**, not just your own
- **Extract individual partitions** (boot, vendor, system, etc.) directly from `payload.bin` without flashing
- Download full OTA ZIPs with pause/resume support
- View detailed firmware info: Android version, security patch, size, MD5

Perfect for **custom ROM developers, kernel developers, and advanced Android users** who need raw partition images from official OTA packages.

---

## ✨ Features

| Feature | Description |
|---|---|
| 📦 OTA Fetch | Fetch latest OTA metadata for any supported device model |
| ⬇️ Download Manager | Built-in downloader with pause, resume, retry, and 3x auto-retry on CDN failures |
| 🔧 Payload Extractor | Extract specific partitions from `payload.bin` inside the OTA ZIP |
| 🔐 Secure Communication | AES + RSA encrypted OTA API calls |
| 📊 Device Info | View Android version, security patch level, build number, OTA size & MD5 |
| 🔄 In-app Updates | Auto-detects new OTA Pulse releases via GitHub |
| ⚡ Background Downloads | Uses WorkManager for reliable background extraction |
| 🎨 Material You | Full Material Design 3 with dynamic color (Android 12+) |

---

## 📱 Supported Devices

OTA Pulse supports devices from:
- **Realme** — Realme 12 Pro+, Realme P series, Realme GT series, and more
- **OPPO** — Find series
- **OnePlus** — OnePlus 15R, Nord 5, Nord CE 5, Ace 6T, and more

> Don't see your device? Device definitions are easy to add — PRs welcome!

---

## 🛠 Tech Stack

- **Language:** Kotlin 100%
- **UI:** Jetpack Compose + Material Design 3
- **DI:** Hilt
- **Networking:** OkHttp
- **Background Work:** WorkManager
- **Async:** Coroutines + Flow

---

## 📦 Requirements

- Android 10+ (API 29)
- Active internet connection

---

## ⬇️ Download

👉 **[Get the latest APK from Releases](https://github.com/RemuruSama/OTA-Pulse/releases)**

---

## ⚠️ Disclaimer

OTA Pulse is an independent utility and is **not affiliated with OnePlus, Realme, or OPPO**. All firmware files are fetched from official manufacturer servers.

---

<div align="center">

⭐ If OTA Pulse helped you, please star the repo — it helps others find it!

</div>

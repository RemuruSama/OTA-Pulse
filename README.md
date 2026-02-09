# OTA Pulse

OTA Pulse is a modern Android application designed to simplify the process of downloading and managing Over-the-Air (OTA) updates for your devices. The app is built with a focus on a clean user interface and a robust architecture, ensuring a seamless and reliable user experience.

## Features

* **Wide Range of Supported Devices:** OTA Pulse supports a vast and ever-growing list of devices from major brands like OnePlus, Realme, and OPPO.
* **Advanced User Features:**
    * **Custom Device Profiles:** Create and manage your own device profiles. This allows you to extend the app's functionality to unsupported devices by specifying the necessary details to fetch OTA updates.
    * **Custom OTA Requests:** Manually request OTA updates for your device, even if they are not officially available yet.
* **Comprehensive Download Management:**
    * **Full Control:** Pause, resume, cancel, retry, and delete your downloads directly from the app.
    * **Manual Downloads:** Initiate downloads by providing a direct URL.
* **Detailed Device Information:** Get a comprehensive overview of your device's hardware and software, including:
    * **Software:** OS version, security patch, and more.
    * **Hardware:** SoC, CPU, RAM, storage, battery, and display details.
* **OTA Update Management:** Easily view, download, and manage OTA updates for your supported devices.
* **Advanced OTA Protocol Implementation:** The app features a sophisticated implementation of the OTA protocol, allowing it to communicate with regional OTA servers and handle complex request/response formats.
* **Secure Communication:** All communication with the OTA servers is secured using a combination of AES and RSA encryption.
* **Background Downloads:** OTA updates are downloaded in the background using WorkManager, ensuring that the process continues even if the app is closed.
* **Interactive Notifications:** Stay informed about download progress with rich notifications that include controls to pause, resume, and cancel downloads directly.
* **Modern UI:** The app features a clean and intuitive user interface built with a mix of Android Views and Jetpack Compose.
* **App Settings:** Customize the app's behavior through a dedicated settings screen.
* **About Section:** View information about the app, such as its version and developer.
* **In-App Updates:** The app can check for updates from GitHub and notify you about new versions.
* **OTA Link Resolution:** The app can resolve complex OTA links, including those that require special headers, to ensure a successful download.

## Tech Stack

* **Kotlin:** The app is written entirely in Kotlin.
* **Layered Architecture:** The app follows a layered architecture, separating concerns and promoting maintainability.
* **Jetpack Compose:** Used for building modern, declarative UI components.
* **AndroidX:** A suite of libraries that provide backward compatibility and new features.
* **Hilt:** For dependency injection.
* **WorkManager:** For managing background tasks.
* **Retrofit & Gson:** For networking and JSON parsing.
* **Glide:** For image loading.
* **Coroutines:** For asynchronous programming.
* **Fetch:** For downloading files.

## Getting Started

To build and run the app, you'll need to have Android Studio installed. Then, follow these steps:

1. **Clone the repository:**
   ```bash
   git clone https://github.com/your-username/ota-pulse.git
   ```
2. **Open the project in Android Studio.**
3. **Create a `keystore.properties` file** in the root directory of the project with the following content:
    ```
    STORE_PASSWORD=your_store_password
    KEY_PASSWORD=your_key_password
    ```
4. **Build and run the app.**

## Permissions

* **INTERNET:** To download OTA update files and retrieve device information.
* **ACCESS_NETWORK_STATE:** To check for network connectivity before starting a download.
* **WRITE_EXTERNAL_STORAGE/READ_EXTERNAL_STORAGE:** To store the downloaded OTA update files (only on older Android versions).
* **POST_NOTIFICATIONS:** To display notifications about the download progress (on Android 13 and above).
* **VIBRATE:** To provide haptic feedback.

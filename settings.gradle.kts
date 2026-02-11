pluginManagement {
    repositories {
        gradlePluginPortal()
        google() // Add the full Google Maven repository for plugins here
        mavenCentral() // Keep mavenCentral here
    }
}
plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        maven { url = uri("https://jitpack.io") }
    }
}

rootProject.name = "Ota Pulse"
include(":app")
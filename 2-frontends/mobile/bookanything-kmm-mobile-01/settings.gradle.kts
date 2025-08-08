
pluginManagement {
    repositories {
        google()
        gradlePluginPortal()
        mavenCentral()
    }
    plugins {
        id("com.android.application") version "8.1.0"
        id("com.android.library") version "8.1.0"
        kotlin("android") version "1.9.23"
        kotlin("multiplatform") version "1.9.23"
    }
}

rootProject.name = "BookAnythingKMMMobile01"

include(":androidApp")
include(":shared")

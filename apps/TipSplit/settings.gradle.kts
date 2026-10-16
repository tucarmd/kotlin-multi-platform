/*
 * [Bài 1 · KMP là gì] Tip Split is the "shared logic, native UI" kind of KMP app:
 *   - :shared      Kotlin Multiplatform. Model, rules and state, compiled for Android AND iOS.
 *   - :androidApp  plain Android app (Jetpack Compose) that depends on :shared.
 *   - iosApp/      Xcode project (SwiftUI), NOT a Gradle module. It imports the framework :shared builds.
 * The other kind, "shared UI" with Compose Multiplatform (the composeApp/ module in the lessons), also shares the
 * screens. The practice apps and apps/DevicePulse keep the UI native so you learn both Compose and SwiftUI.
 *
 * [Bài 2 · Cài máy] This file is the first thing Gradle reads: project name, where plugins and libraries are
 * downloaded from, and which modules exist. Android Studio's "Sync" starts here.
 */
rootProject.name = "TipSplit"

// [Bài 2] Where Gradle downloads PLUGINS (AGP, Kotlin, SKIE…). google() is limited to Google's groups so
// Gradle does not ask Google's server for every other library first (faster sync).
pluginManagement {
    repositories {
        google {
            mavenContent {
                includeGroupAndSubgroups("androidx")
                includeGroupAndSubgroups("com.android")
                includeGroupAndSubgroups("com.google")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}

// [Bài 2] Where Gradle downloads LIBRARIES (coroutines, Koin, Compose…). One list for every module.
dependencyResolutionManagement {
    repositories {
        google {
            mavenContent {
                includeGroupAndSubgroups("androidx")
                includeGroupAndSubgroups("com.android")
                includeGroupAndSubgroups("com.google")
            }
        }
        mavenCentral()
    }
}

// [Bài 3 · Cấu trúc project] Two Gradle modules. iosApp is missing on purpose: Xcode builds it, and its
// "Compile Kotlin Framework" build phase calls ./gradlew :shared:embedAndSignAppleFrameworkForXcode.
include(":shared")
include(":androidApp")

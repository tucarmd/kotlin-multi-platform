rootProject.name = "ObdLink"

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

// [App 5 · B4] First app split into several modules. Dependencies only point down:
// androidApp → shared → core:ble → core:obd. core:obd knows nothing about BLE, Android or iOS.
include(":core:obd")
include(":core:ble")
include(":shared")
include(":androidApp")

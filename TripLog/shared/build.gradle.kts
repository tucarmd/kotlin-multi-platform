import org.jetbrains.kotlin.gradle.dsl.JvmTarget

// [App 4] KMP core: model, rules, TripRecorder, TripLibrary, SQLDelight, GpxWriter, LocationStream.
// No UI here: androidApp draws it with Jetpack Compose, iosApp with SwiftUI.
plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidLibrary)
    alias(libs.plugins.sqldelight)
    alias(libs.plugins.skie)
}

kotlin {
    androidTarget {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_11)
        }
    }

    listOf(
        iosArm64(),
        iosSimulatorArm64(),
    ).forEach { iosTarget ->
        iosTarget.binaries.framework {
            baseName = "Shared" // Swift: import Shared
            isStatic = true
            // SQLDelight's native driver links against the system SQLite
            linkerOpts("-lsqlite3")
        }
    }

    compilerOptions {
        optIn.add("kotlin.time.ExperimentalTime")
        optIn.add("kotlinx.coroutines.ExperimentalCoroutinesApi") // runCurrent / advanceTimeBy in tests
    }

    sourceSets {
        commonMain.dependencies {
            implementation(libs.kotlinx.coroutines.core) // StateFlow, callbackFlow
            api(libs.koin.core) // androidApp adds its own module through initKoin { }
            implementation(libs.sqldelight.coroutines) // Query.asFlow()
        }
        androidMain.dependencies {
            implementation(libs.kotlinx.coroutines.android)
            implementation(libs.koin.android)
            implementation(libs.sqldelight.android.driver)
        }
        iosMain.dependencies {
            implementation(libs.sqldelight.native.driver)
        }
        commonTest.dependencies {
            implementation(kotlin("test"))
            implementation(libs.kotlinx.coroutines.test)
            implementation(libs.turbine)
        }
    }
}

android {
    namespace = "dev.triplog.shared"
    compileSdk = libs.versions.android.compileSdk.get().toInt()
    defaultConfig {
        minSdk = libs.versions.android.minSdk.get().toInt()
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

sqldelight {
    databases {
        create("TripDb") {
            packageName.set("dev.triplog.db")
        }
    }
}

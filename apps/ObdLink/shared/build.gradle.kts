import org.jetbrains.kotlin.gradle.dsl.JvmTarget

// [App 5 · B2/B4] :shared — ObdCore (state machine), CommandQueue, Koin and the Swift door.
// No UI here: androidApp draws it with Jetpack Compose, iosApp with SwiftUI.
plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidLibrary)
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
            // TRAP: without export, Swift sees :core:obd types under a module prefix (Core_obdDtc)
            // and cannot call their helpers. export() needs the matching api() dependency below.
            export(project(":core:obd"))
        }
    }

    compilerOptions {
        // limitedParallelism (Koin.kt) and the kotlinx-coroutines-test clock helpers are still marked experimental.
        optIn.add("kotlinx.coroutines.ExperimentalCoroutinesApi")
    }

    sourceSets {
        commonMain.dependencies {
            api(project(":core:obd")) // ObdState exposes Dtc, Dongle, LiveData… to both UIs
            implementation(project(":core:ble"))
            implementation(libs.kotlinx.coroutines.core)
            api(libs.koin.core) // androidApp adds its own module through initKoin { }
        }
        androidMain.dependencies {
            implementation(libs.koin.android)
        }
        commonTest.dependencies {
            implementation(kotlin("test"))
            implementation(libs.kotlinx.coroutines.test) // runTest + virtual time for timeouts
        }
    }
}

android {
    namespace = "dev.obdlink.shared"
    compileSdk = libs.versions.android.compileSdk.get().toInt()
    defaultConfig {
        minSdk = libs.versions.android.minSdk.get().toInt()
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

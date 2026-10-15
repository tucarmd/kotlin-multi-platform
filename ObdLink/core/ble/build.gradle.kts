// [App 5 · B3] :core:ble — ElmTransport implemented on BLE with Kable (scan, connect, write, notify).
//
// Why a separate module: the only module that knows Kable. Swapping Kable for raw BluetoothGatt / CoreBluetooth,
// or adding a Wi-Fi ELM327 transport, changes this module only. It depends on :core:obd for ElmTransport.
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidLibrary)
}

kotlin {
    androidTarget {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_11)
        }
    }
    iosArm64()
    iosSimulatorArm64()

    compilerOptions {
        // Kable's API takes kotlin.uuid.Uuid.
        optIn.add("kotlin.uuid.ExperimentalUuidApi")
    }

    sourceSets {
        commonMain.dependencies {
            api(project(":core:obd"))
            implementation(libs.kotlinx.coroutines.core)
            implementation(libs.kable.core)
        }
        commonTest.dependencies {
            implementation(kotlin("test"))
        }
    }
}

android {
    namespace = "dev.obdlink.ble"
    compileSdk = libs.versions.android.compileSdk.get().toInt()
    defaultConfig {
        minSdk = libs.versions.android.minSdk.get().toInt()
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

// [App 5 · B1] :core:obd — the ELM327 / OBD-II language: commands, response parser, PID formulas, DTC decoding.
//
// Why a separate module: it is pure Kotlin. It has no Android plugin and no BLE library, so it CANNOT import
// either by accident (the compiler enforces the "core:obd knows nothing about BLE" rule for us).
// jvm() is enough for Android: an Android module accepts a plain JVM variant of a KMP library.
plugins {
    alias(libs.plugins.kotlinMultiplatform)
}

kotlin {
    jvm()
    iosArm64()
    iosSimulatorArm64()

    sourceSets {
        commonMain.dependencies {
            // Only for the ElmTransport interface (suspend + Flow). No platform code.
            implementation(libs.kotlinx.coroutines.core)
        }
        commonTest.dependencies {
            implementation(kotlin("test"))
        }
    }
}

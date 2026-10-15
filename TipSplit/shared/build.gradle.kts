import org.jetbrains.kotlin.gradle.dsl.JvmTarget

// [App 1] KMP core: model, rules, TipCore, Koin. No UI here:
// androidApp draws it with Jetpack Compose, iosApp with SwiftUI.
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
        }
    }

    sourceSets {
        commonMain.dependencies {
            implementation(libs.kotlinx.coroutines.core) // StateFlow
            api(libs.koin.core) // androidApp adds its own module through initKoin { }
        }
        androidMain.dependencies {
            implementation(libs.koin.android)
        }
        commonTest.dependencies {
            implementation(kotlin("test"))
            implementation(libs.kotlinx.coroutines.test) // runTest, virtual time
            implementation(libs.turbine) // flow.test { awaitItem() }
        }
    }
}

android {
    namespace = "dev.tipsplit.shared"
    compileSdk = libs.versions.android.compileSdk.get().toInt()
    defaultConfig {
        minSdk = libs.versions.android.minSdk.get().toInt()
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

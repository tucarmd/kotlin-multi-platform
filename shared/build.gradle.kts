import org.jetbrains.kotlin.gradle.dsl.JvmTarget

/**
 * P11 — the KMP library holds ONLY what both apps share: model, rules, ViewModels, interfaces, Ktor, logger,
 * monitor loop + the Android implementations (androidMain). No Compose: each platform draws its own UI
 * (androidApp = Jetpack Compose, iosApp = SwiftUI).
 */
plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidMultiplatformLibrary)
    alias(libs.plugins.kotlinxSerialization)
    alias(libs.plugins.skie)          // P9: Flow → AsyncSequence for SwiftUI
}

kotlin {
    listOf(
        iosArm64(),
        iosSimulatorArm64()
    ).forEach { iosTarget ->
        iosTarget.binaries.framework {
            baseName = "Shared"
            isStatic = true
        }
    }

    androidLibrary {
        namespace = "com.learn.ndmkmp.shared"
        compileSdk = libs.versions.android.compileSdk.get().toInt()
        minSdk = libs.versions.android.minSdk.get().toInt()

        compilerOptions {
            jvmTarget = JvmTarget.JVM_11
        }
        androidResources {
            enable = true          // P5: res/xml/file_paths.xml for the camera FileProvider
        }
        // P1: run commonTest on the JVM too (`:shared:testAndroidHostTest`), no emulator needed
        withHostTest {}
    }

    sourceSets {
        androidMain.dependencies {
            implementation(libs.androidx.activity)                // P3/P5: ComponentActivity + ActivityResult launchers
            implementation(libs.androidx.core.ktx)
            implementation(libs.androidx.work.runtime)        // P7: MonitorWorker every 15′
            implementation(libs.ktor.client.okhttp)           // P6: Android HTTP engine
            implementation(libs.kotlinx.coroutines.android)   // P11: Dispatchers.Main for the ViewModels
        }
        iosMain.dependencies {
            implementation(libs.ktor.client.darwin)           // P6: NSURLSession engine
        }
        commonMain.dependencies {
            implementation(libs.kotlinx.coroutines.core)      // P8: ViewModel scope + StateFlow
            implementation(libs.kotlinx.serialization.json)
            implementation(libs.ktor.client.core)             // P6
            implementation(libs.kotlinx.io.core)              // P7: FileLogger
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
            implementation(libs.kotlinx.coroutines.test)
            implementation(libs.ktor.client.mock)             // P6: ApiService tests without network
        }
    }
}

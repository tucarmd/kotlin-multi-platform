import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidMultiplatformLibrary)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    // 📍 P1 note: serialization compiler plugin — it GENERATES `DeviceStatus.serializer()` at build time from
    // `@Serializable`. Without it the annotation compiles but encode/decode fails. Replaces Android-only org.json.
    alias(libs.plugins.kotlinxSerialization)
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
            enable = true
        }
        // 📍 P1 note: one test file in commonTest runs TWICE:
        //    ./gradlew :shared:testAndroidHostTest      → on your Mac's JVM (fast, no emulator)
        //    ./gradlew :shared:iosSimulatorArm64Test    → compiled to native, run in the iOS simulator
        //    ./gradlew :shared:allTests                 → both. Same 52 tests, 2 platforms = proof the code is really shared.
        // P1: run commonTest on the JVM too (`:shared:testAndroidHostTest`), no emulator needed
        withHostTest {}
    }

    sourceSets {
        androidMain.dependencies {
            implementation(libs.compose.uiToolingPreview)
            implementation(libs.androidx.activity.compose)
        }
        commonMain.dependencies {
            implementation(libs.compose.runtime)
            implementation(libs.compose.foundation)
            implementation(libs.compose.material3)
            implementation(libs.compose.ui)
            implementation(libs.compose.components.resources)
            implementation(libs.compose.uiToolingPreview)
            implementation(libs.androidx.lifecycle.runtimeCompose)
            // 📍 P1 note: runtime half of serialization (Json, encode/decode). commonMain → usable on Android AND iOS.
            implementation(libs.kotlinx.serialization.json)
        }
        // 📍 P1 note: kotlin-test = @Test + assertEquals… that work in common code (JUnit is JVM-only, XCTest iOS-only).
        commonTest.dependencies {
            implementation(libs.kotlin.test)
        }
    }
}

dependencies {
    androidRuntimeClasspath(libs.compose.uiTooling)
}

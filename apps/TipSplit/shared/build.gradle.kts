import org.jetbrains.kotlin.gradle.dsl.JvmTarget

// [App 1] KMP core: model, rules, TipCore, Koin. No UI here:
// androidApp draws it with Jetpack Compose, iosApp with SwiftUI.
plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidLibrary)
    alias(libs.plugins.skie)
    alias(libs.plugins.kover) // Phụ lục P1 · Coverage
}

kotlin {
    // [Bài 3 · Target] A target = one platform this module is compiled for. Tip Split has three:
    //   androidTarget()      → an Android library (.aar) that androidApp depends on
    //   iosArm64()           → a real iPhone
    //   iosSimulatorArm64()  → the iOS Simulator on an Apple-chip Mac
    // No iosX64(): Intel Macs are not supported, and leaving it out makes every iOS build faster.
    androidTarget {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_11)
        }
    }

    // [Bài 3] Each iOS target is packed into an Apple framework. That framework is the ONLY thing Swift sees.
    listOf(
        iosArm64(),
        iosSimulatorArm64(),
    ).forEach { iosTarget ->
        iosTarget.binaries.framework {
            baseName = "Shared" // Swift: import Shared
            // Static: copied into the app binary at link time, so there is no separate .framework to embed and sign.
            isStatic = true
        }
    }

    // [Bài 3 · Source set] Which folder under shared/src/ compiles for which target:
    //   commonMain   → all three targets. Pure Kotlin + multiplatform libraries only (no android.*, no platform.*)
    //   androidMain  → Android only. May use the Android SDK (Context…)
    //   iosMain      → both iOS targets. May use Apple APIs (platform.Foundation, platform.UIKit…)
    //   commonTest   → tests that run on every target (JVM for Android, the Simulator for iOS)
    //
    // [Bài 3 · Hierarchy] There is no iosArm64Main / iosSimulatorArm64Main folder: the default hierarchy template
    // puts both under iosMain (→ appleMain → nativeMain → commonMain), so iOS code is written once.
    //
    // [Bài 5 · Thư viện nào cho source set nào] A multiplatform library goes in commonMain,
    // a platform-only one in that platform's source set.
    // Coroutines and koin-core are multiplatform → commonMain. koin-android needs Context → androidMain only.
    sourceSets {
        commonMain.dependencies {
            implementation(libs.kotlinx.coroutines.core) // StateFlow
            // api, not implementation: initKoin(config: KoinAppDeclaration) is public, so modules that depend on
            // :shared (androidApp) must also see Koin's types. `implementation` would hide them.
            api(libs.koin.core) // androidApp adds its own module through initKoin { }
        }
        androidMain.dependencies {
            implementation(libs.koin.android)
        }
        // iosMain needs no extra library: Koin and coroutines from commonMain already have iOS builds.

        // [Bài 13 · Test] kotlin("test") gives one @Test / assertEquals API. Gradle runs the same files on the JVM
        // (testDebugUnitTest) and on the iOS Simulator (iosSimulatorArm64Test).
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

// [Phụ lục P1 · Coverage] Kover runs the Android unit tests (JVM) and measures which lines they touched.
//   ./gradlew :shared:koverHtmlReportDebug   → shared/build/reports/kover/htmlDebug/index.html
//   ./gradlew :shared:koverVerifyDebug       → whole module ≥ 60 % lines
//   ./gradlew :shared:koverVerifyRules       → money rules ≥ 80 % lines
kover {
    currentProject {
        // A second view of the same debug unit tests that only looks at the rules package.
        // TRAP: Kover 0.9 has no per-rule filters; one variant = one filter set, so a stricter bar needs its own variant.
        copyVariant("rules", "debug")
    }
    reports {
        filters {
            excludes {
                // Koin wiring: no logic, only declarations. Covering it would only inflate the number.
                packages("dev.tipsplit.di")
            }
        }
        variant("debug") {
            // Everything (TipCore, model, rules): a floor that still catches "deleted all the tests".
            verify { rule("whole module") { minBound(60) } }
        }
        variant("rules") {
            filters { includes { packages("dev.tipsplit.rules") } }
            // The money rules are where a bug costs real money.
            verify { rule("money rules") { minBound(80) } }
        }
    }
}

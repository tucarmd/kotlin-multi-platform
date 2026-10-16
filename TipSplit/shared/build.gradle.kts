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

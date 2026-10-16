import org.jetbrains.kotlin.gradle.dsl.JvmTarget

// [App 2] KMP core: model, rules, NoteRepository (SQLDelight), NotesCore, Koin. No UI here:
// androidApp draws it with Jetpack Compose, iosApp with SwiftUI.
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
            // TRAP: SQLDelight's native driver calls the system SQLite. Without -lsqlite3 here (and in Xcode's
            // OTHER_LDFLAGS, see iosApp/project.yml) linking fails with "Undefined symbols: _sqlite3_*".
            linkerOpts("-lsqlite3")
        }
    }

    compilerOptions {
        // kotlin.time.Instant / Clock: same opt-in as DevicePulse, so the code compiles whether or not
        // this Kotlin version still marks them experimental.
        optIn.add("kotlin.time.ExperimentalTime")
        // runCurrent / advanceTimeBy in NotesCoreTest
        optIn.add("kotlinx.coroutines.ExperimentalCoroutinesApi")
    }

    sourceSets {
        commonMain.dependencies {
            implementation(libs.kotlinx.coroutines.core) // StateFlow, combine
            api(libs.kotlinx.datetime) // TimeZone for "yesterday"; api because NotesCore takes a TimeZone
            api(libs.koin.core) // androidApp adds its own module through initKoin { }
            implementation(libs.sqldelight.coroutines) // Query.asFlow().mapToList()
        }
        androidMain.dependencies {
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
        androidUnitTest.dependencies {
            implementation(libs.sqldelight.sqlite.driver)
        }
    }
}

android {
    namespace = "dev.pocketnotes.shared"
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
        create("NotesDb") {
            packageName.set("dev.pocketnotes.db")
            // Schema version = highest .sqm number + 1. 1.sqm exists, so NotesDb.Schema.version == 2.
        }
    }
}

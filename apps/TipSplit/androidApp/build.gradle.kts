// [App 1] Android app: Jetpack Compose UI on top of the shared core. Only drawing and Koin startup live here.
//
// [Bài 3] A normal Android module, NOT a KMP one: kotlinAndroid instead of kotlinMultiplatform, and no
// commonMain / iosMain. Its code is Android-only; everything it shares with iOS comes from project(":shared").
plugins {
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.kotlinAndroid)
    // [Bài 6 · Compose] The Compose compiler plugin turns @Composable functions into UI code. Since Kotlin 2.0
    // it ships with Kotlin, so its version is the Kotlin version (see libs.versions.toml).
    alias(libs.plugins.composeCompiler)
}

android {
    namespace = "dev.tipsplit.android"
    compileSdk = libs.versions.android.compileSdk.get().toInt()

    defaultConfig {
        // [Bài 14 · Build và phát hành] applicationId is the app's identity on Google Play: never change it after
        // the first upload. versionCode must go up for every upload; versionName is only what users see.
        applicationId = "dev.tipsplit"
        minSdk = libs.versions.android.minSdk.get().toInt()
        targetSdk = libs.versions.android.targetSdk.get().toInt()
        versionCode = 1
        versionName = "1.0"
        // [Phụ lục P2] Runs src/androidTest on an emulator: ./gradlew :androidApp:connectedDebugAndroidTest
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }
    buildFeatures {
        compose = true
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_11)
    }
}

dependencies {
    // [Bài 4 · Chia sẻ code] The one line that gives Android the model, rules and TipCore written in shared/.
    implementation(project(":shared"))
    // [Bài 5] The BOM (bill of materials) picks one tested set of Compose versions, so the Compose libraries
    // below have no version of their own in libs.versions.toml.
    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.ui.tooling.preview)
    implementation(libs.compose.material3)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.koin.android)
    implementation(libs.koin.compose.viewmodel) // [Bài 8] koinViewModel() in SplitRoute
    debugImplementation(libs.compose.ui.tooling) // [Bài 6] draws @Preview in Android Studio; debug builds only

    // [Phụ lục P2 · Compose UI test]
    androidTestImplementation(platform(libs.compose.bom))
    androidTestImplementation(libs.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.test.ext.junit)
    androidTestImplementation(libs.androidx.test.runner)
    // TRAP: debugImplementation, not androidTestImplementation. It adds the empty Activity that createComposeRule()
    // launches to the APP's debug manifest; without it the test fails with "No activity found to handle Intent".
    debugImplementation(libs.compose.ui.test.manifest)
}

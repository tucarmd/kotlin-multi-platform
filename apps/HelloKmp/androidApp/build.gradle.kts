// [App 0 · 1.1] Android app: Jetpack Compose UI on top of shared/.
// A normal Android module, NOT a KMP one: kotlinAndroid instead of kotlinMultiplatform, no commonMain / iosMain.
// Everything it shares with iOS comes from project(":shared").
plugins {
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.kotlinAndroid)
    alias(libs.plugins.composeCompiler) // turns @Composable functions into UI code; version = Kotlin version
}

android {
    namespace = "dev.hellokmp.android"
    compileSdk = libs.versions.android.compileSdk.get().toInt()

    defaultConfig {
        applicationId = "dev.hellokmp"
        minSdk = libs.versions.android.minSdk.get().toInt()
        targetSdk = libs.versions.android.targetSdk.get().toInt()
        versionCode = 1
        versionName = "1.0"
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
    // The one line that "embeds" shared/ into Android: its classes are callable like any other Kotlin code.
    implementation(project(":shared"))
    implementation(platform(libs.compose.bom)) // one tested set of Compose versions
    implementation(libs.compose.ui)
    implementation(libs.compose.ui.tooling.preview)
    implementation(libs.compose.material3)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose) // 1.3: viewModel()
    implementation(libs.androidx.lifecycle.runtime.compose)   // 1.3: collectAsStateWithLifecycle()
    debugImplementation(libs.compose.ui.tooling) // draws @Preview in Android Studio; debug builds only
}

// [App 0 · 1.1] Root build file: only declares plugin versions. Each module applies what it needs.
// `apply false` = "load this version once for the whole build, but do not turn it on here".
// shared/ turns on kotlinMultiplatform + androidLibrary, androidApp/ turns on androidApplication + kotlinAndroid.
plugins {
    alias(libs.plugins.androidApplication) apply false
    alias(libs.plugins.androidLibrary) apply false
    alias(libs.plugins.kotlinAndroid) apply false
    alias(libs.plugins.kotlinMultiplatform) apply false
    alias(libs.plugins.composeCompiler) apply false
}

// [App 1] Root build file: only declares plugin versions. Each module applies what it needs.
//
// [Bài 5 · Thêm thư viện] `libs.plugins.x` is generated from the [plugins] table of gradle/libs.versions.toml.
// `apply false` = "load this version once for the whole build, but do not turn it on here".
// shared/ then turns on kotlinMultiplatform + androidLibrary, androidApp/ turns on androidApplication + kotlinAndroid.
plugins {
    alias(libs.plugins.androidApplication) apply false
    alias(libs.plugins.androidLibrary) apply false
    alias(libs.plugins.kotlinAndroid) apply false
    alias(libs.plugins.kotlinMultiplatform) apply false
    alias(libs.plugins.composeCompiler) apply false
    alias(libs.plugins.skie) apply false
    alias(libs.plugins.kover) apply false
}

// Desktop-only sandbox for Compose Hot Reload and its MCP server (needs Compose Multiplatform 1.12+).
// DevicePulse itself has no desktop target, so the UI here is a stand-in fed by fake readings.
plugins {
    kotlin("multiplatform") version "2.4.20"
    kotlin("plugin.compose") version "2.4.20"
    id("org.jetbrains.compose") version "1.12.1"
}

kotlin {
    jvm()
    jvmToolchain(21)

    sourceSets {
        jvmMain.dependencies {
            implementation(compose.desktop.currentOs)
            implementation(compose.material3)
            implementation("org.jetbrains.kotlinx:kotlinx-coroutines-swing:1.11.0")
        }
    }
}

compose.desktop {
    application {
        mainClass = "dev.devicepulse.demo.MainKt"
    }
}

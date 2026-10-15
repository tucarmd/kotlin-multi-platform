// Android Studio plugin: a gutter icon next to every Kotlin function that asks Claude Code
// for 3 alternative implementations and opens them in a split beside the code, for learning.
import org.jetbrains.kotlin.gradle.dsl.KotlinVersion

plugins {
    kotlin("jvm") version "2.4.20"
    id("org.jetbrains.intellij.platform") version "2.19.0"
}

group = "dev.devicepulse"
version = "0.1.0"

repositories {
    mavenCentral()
    intellijPlatform {
        defaultRepositories()
    }
}

dependencies {
    intellijPlatform {
        // Compile against the Android Studio already installed instead of downloading an IDE
        local(providers.gradleProperty("studioPath"))
        bundledPlugin("org.jetbrains.kotlin")
    }
}

kotlin {
    jvmToolchain(21)
    compilerOptions {
        // Stay within the Kotlin API the IDE's bundled stdlib provides
        apiVersion.set(KotlinVersion.KOTLIN_2_2)
    }
}

intellijPlatform {
    pluginConfiguration {
        ideaVersion {
            sinceBuild = "253"
            untilBuild = provider { null }
        }
    }
    pluginVerification {
        ides {
            // Check binary compatibility against the same Android Studio
            local(file(providers.gradleProperty("studioPath")))
        }
    }
    buildSearchableOptions = false
    instrumentCode = false
}

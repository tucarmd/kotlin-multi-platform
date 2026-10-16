rootProject.name = "studio-variants-plugin"

plugins {
    // Lets Gradle download a JDK 21 when the machine only has an older one
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

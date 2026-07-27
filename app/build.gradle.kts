import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
    kotlin("jvm") version "2.1.0"
    id("org.jetbrains.compose") version "1.7.3"
    id("org.jetbrains.kotlin.plugin.compose") version "2.1.0"
    kotlin("plugin.serialization") version "2.1.0"
}

group = "com.javscraper"
version = "1.0.0"

repositories { mavenCentral(); google() }

dependencies {
    implementation(compose.desktop.currentOs)
    implementation(compose.material3)
    implementation(compose.materialIconsExtended)
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.3")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.9.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-swing:1.9.0")
    implementation("io.github.microutils:kotlin-logging:3.0.5")
    implementation("ch.qos.logback:logback-classic:1.5.15")

    testImplementation(kotlin("test"))
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.9.0")
    testImplementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.3")
}

compose.desktop {
    application {
        mainClass = "javscraper.MainKt"
        nativeDistributions {
            targetFormats(TargetFormat.Msi, TargetFormat.Exe)
            packageName = "JavScraper"
            packageVersion = "1.0.0"
            vendor = "JavScraper"
            description = "Local video metadata scraper for Jellyfin/Emby/Kodi"
            licenseFile = rootProject.file("LICENSE.txt")

            windows {
                menuGroup = "JavScraper"
                upgradeUuid = "a1b2c3d4-e5f6-7890-abcd-ef1234567890"
                // Include the worker executable in the package
                appResourcesRootDir.set(rootProject.file("src/main/resources"))
            }

            macOS {
                bundleID = "com.javscraper.app"
            }

            linux {
                packageName = "javscraper"
            }
        }
    }
}

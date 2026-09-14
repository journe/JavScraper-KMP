import org.jetbrains.compose.desktop.application.dsl.TargetFormat
import org.gradle.api.tasks.testing.Test

plugins {
    kotlin("jvm") version "2.4.10"
    id("org.jetbrains.compose") version "1.12.0"
    id("org.jetbrains.kotlin.plugin.compose") version "2.4.10"
    kotlin("plugin.serialization") version "2.4.10"
}

group = "com.javscraper"
version = "1.0.0"

repositories { mavenCentral(); google() }

dependencies {
    implementation(compose.desktop.currentOs)
    // CMP 1.12.0 的 Material3 随 Jetpack M3 1.5.0-alpha22 发布,版本号为 1.12.0-alpha03
    implementation("org.jetbrains.compose.material3:material3:1.12.0-alpha03")
    implementation("org.jetbrains.compose.material:material-icons-extended:1.7.3")
    implementation("org.jetbrains.compose.ui:ui-tooling-preview:1.12.0")
    implementation("io.github.oikvpqya.compose.fastscroller:fastscroller-core:0.3.2")
    implementation("io.github.oikvpqya.compose.fastscroller:fastscroller-material3:0.3.2")
    implementation("io.coil-kt.coil3:coil-compose:3.3.0")
    implementation("io.coil-kt.coil3:coil-network-okhttp:3.3.0")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.11.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.11.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-swing:1.11.0")
    implementation("io.github.microutils:kotlin-logging:3.0.5")
    implementation("ch.qos.logback:logback-classic:1.6.3")

    // FileKit - cross-platform native file/directory pickers
    implementation("io.github.vinceglb:filekit-core:0.15.0")
    implementation("io.github.vinceglb:filekit-dialogs:0.15.0")

    testImplementation(kotlin("test"))
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.11.0")
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
                iconFile.set(project.file("src/main/resources/icon/app_icon.ico"))
                appResourcesRootDir.set(rootProject.file("src/main/resources"))
            }

            macOS {
                bundleID = "com.javscraper.app"
            }

            linux {
                packageName = "javscraper"
                iconFile.set(project.file("src/main/resources/icon/app_icon.png"))
            }
        }
    }
}
tasks.withType<Test>().configureEach {
    // 限制测试 JVM 并行度，避免 Windows 页面文件不足导致 JVM 崩溃（Gradle 9 默认并行 fork）
    maxParallelForks = 1
    maxHeapSize = "768m"
}

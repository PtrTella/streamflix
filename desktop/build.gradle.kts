import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
    kotlin("jvm")
    id("org.jetbrains.compose")
    id("org.jetbrains.kotlin.plugin.compose")
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
}

dependencies {
    implementation(project(":core"))

    implementation(compose.desktop.currentOs)
    implementation(compose.material3)
    implementation(compose.materialIconsExtended)

    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-swing:1.9.0")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")

    // Embedded VLC Player via LibVLC on macOS
    implementation("uk.co.caprica:vlcj:4.8.3")
}

compose.desktop {
    application {
        mainClass = "com.streamflix.desktop.MainKt"

        nativeDistributions {
            targetFormats(TargetFormat.Dmg)
            packageName = "StreamFlix"
            packageVersion = "1.0.0"
            description = "StreamFlix Reborn for macOS"
            vendor = "StreamFlix"
            copyright = "© 2026 StreamFlix"

            macOS {
                bundleID = "com.streamflix.desktop"
                dockName = "StreamFlix"
                packageBuildVersion = "1.0.0"
                appStore = false
            }
        }
    }
}

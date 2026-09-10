import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
}

kotlin {
    jvm()
    
    sourceSets {
        val         jvmMain by getting {
            dependencies {
                implementation(project(":shared"))
                implementation(compose.desktop.currentOs)
                implementation(libs.room.runtime)
                implementation(libs.sqlite.bundled)
                implementation(libs.kotlinx.coroutines.swing)
            }
        }
    }
}

compose.desktop {
    application {
        mainClass = "com.freetime.geoweather.MainKt"

        nativeDistributions {
            targetFormats(TargetFormat.Msi, TargetFormat.Exe, TargetFormat.Deb, TargetFormat.Rpm)
            packageName = "GeoWeather"
            packageVersion = "3.1.3"
            vendor = "Freetime Maker"
            description = "GeoWeather - Privacy-focused weather app"
            copyright = "Copyright © 2026 Freetime Maker"

            windows {
                // Make the installed app findable: Start menu entry + desktop shortcut
                menu = true
                menuGroup = "GeoWeather"
                shortcut = true
                // Let the user see/pick the install folder in the setup wizard
                dirChooser = true
                // Per-user install: no admin rights needed, lands in %LOCALAPPDATA%
                perUserInstall = true
                // Fixed upgrade code so future versions upgrade instead of installing side-by-side
                upgradeUuid = "446ac3ee-60c3-457c-b11d-7a335f8bf8eb"
                iconFile.set(file("icons/icon.ico"))
            }

            linux {
                iconFile.set(file("icons/icon.png"))
            }
        }
    }
}

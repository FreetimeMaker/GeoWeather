import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
}

kotlin {
    jvm()
    
    sourceSets {
        val jvmMain by getting {
            dependencies {
                implementation(project(":shared"))
                implementation(compose.desktop.currentOs)
                implementation(libs.room.runtime)
                implementation(libs.sqlite.bundled)
            }
        }
    }
}

compose.desktop {
    application {
        mainClass = "com.freetime.geoweather.MainKt"

        nativeDistributions {
            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb)
            packageName = "com.freetime.geoweather"
            packageVersion = "2.3.0"
        }
    }
}

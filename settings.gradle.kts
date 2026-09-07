pluginManagement {
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
        maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
        maven { url = uri("https://jitpack.io") }
    }
}

rootProject.name = "GeoWeather"
include(":androidApp")
include(":shared")
include(":desktopApp")

// Map the SDK project directly to its subdirectory to avoid issues with parent projects
include(":freetime-sdk")
project(":freetime-sdk").projectDir = file("SDK/SDK")

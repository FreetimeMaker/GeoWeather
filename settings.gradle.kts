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
        maven { 
            url = uri("https://jitpack.io")
            url = uri("https://repo.free-time.me/maven/")
          }
    }
}

rootProject.name = "GeoWeather"
include(":app")

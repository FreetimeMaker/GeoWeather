# GeoWeather

## Overview

GeoWeather is a **native Android weather application** written in Kotlin with
Jetpack Compose, inspired by the Swiss MeteoSwiss app. It compiles to an APK
and runs on Android devices (API 26+); it is not a web app.

- Source: `app/` (Kotlin sources, resources, AndroidManifest.xml)
- Build system: Gradle with Kotlin DSL (`build.gradle.kts`, `settings.gradle.kts`)
- Signing: `GeoWeather-KeyStore.jks`, `build_and_sign_workflow.sh`
- Publishing metadata: `fastlane/`

## Building the Android app

You need a JDK and the Android SDK installed on your machine. From the project
root:

```bash
./gradlew assembleDebug      # debug APK in app/build/outputs/apk/debug/
./gradlew assembleRelease    # unsigned release APK
./gradlew installDebug       # install onto a connected device/emulator
```

The Replit container does not have the Android SDK installed and is not used
to build the APK. CI builds run via GitHub Actions (see `.github/workflows/`).

## Replit environment

Because the Android app cannot run inside the Replit web preview, this Repl
serves a small **static landing page** describing the project so the preview
pane has something meaningful to show.

- Static site source: `web/` (`index.html` + `screenshots/`)
- Workflow `Start application`:
  `python3 -m http.server 5000 --bind 0.0.0.0 --directory web`
- Deployment: configured as `static` with `publicDir = "web"`

## Project structure

```
app/                        # Android application module (Kotlin/Compose)
fastlane/                   # F-Droid / store metadata and screenshots
gradle/                     # Gradle wrapper and version catalog
build.gradle.kts            # Root Gradle build script
settings.gradle.kts         # Gradle settings
build_and_sign_workflow.sh  # CI build/sign helper
web/                        # Static landing page served in Replit preview
```

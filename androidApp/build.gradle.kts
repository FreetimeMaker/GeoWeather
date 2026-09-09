import com.android.build.api.variant.ApplicationAndroidComponentsExtension
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.composeCompiler)
}

android {
    namespace = "com.freetime.geoweather"
    compileSdk = 37

    defaultConfig {
        applicationId = "com.freetime.geoweather"
        minSdk = 26
        targetSdk = 37
        versionCode = 66
        versionName = "2.3.0"
    }

    buildFeatures {
        compose = true
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

// See the workaround in shared/build.gradle.kts: the Compose plugin cannot
// wire :shared compose resources into the AAR assets with AGP 9.x, so we merge
// the assembled tree here via the Variant API. Its content lands at
// assets/composeResources/geoweather.shared.generated.resources/...
// which is exactly what DefaultAndroidResourceReader looks up at runtime.
// See the workaround in shared/build.gradle.kts: the Compose plugin cannot
// wire :shared compose resources into the AAR assets with AGP 9.x, so we merge
// the assembled tree here via the Variant API. Its content lands at
// assets/composeResources/geoweather.shared.generated.resources/...
// which is exactly what DefaultAndroidResourceReader looks up at runtime.
extensions.configure<ApplicationAndroidComponentsExtension> {
    onVariants(selector().all()) { variant ->
        variant.sources.assets?.addStaticSourceDirectory(
            project(":shared").layout.buildDirectory
                .dir("composeAndroidAssets").get().asFile.absolutePath
        )
    }
}

// The static asset directory above carries no task dependency info, so every
// task reading variant assets (mergers, lint, ...) must be ordered explicitly
// after its producer.
tasks.configureEach {
    if ((name.startsWith("merge") && name.contains("Assets")) || name.contains("lint", ignoreCase = true)) {
        dependsOn(":shared:assembleAndroidComposeAssets")
    }
}

dependencies {
    implementation(project(":shared"))
    implementation(libs.androidx.glance.appwidget)
    implementation(libs.androidx.glance.material3)
    implementation(libs.androidx.work.runtime)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.core.ktx)
}

kotlin {
    compilerOptions {
        jvmTarget = JvmTarget.JVM_11
    }
}

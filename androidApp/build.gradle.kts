import com.android.build.api.variant.ApplicationAndroidComponentsExtension
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.composeCompiler)
}

android {
    ndkVersion = "28.0.13004108"
    namespace = "com.freetime.geoweather"
    compileSdk = 37

    defaultConfig {
        applicationId = "com.freetime.geoweather"
        minSdk = 26
        targetSdk = 37
        versionCode = 70
        versionName = "3.1.2"
    }

    buildFeatures {
        compose = true
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }

        jniLibs {
            keepDebugSymbols += "**/libsqliteJni.so"
        }
    }
}

extensions.configure<ApplicationAndroidComponentsExtension> {
    onVariants(selector().all()) { variant ->
        variant.sources.assets?.addStaticSourceDirectory(
            project(":shared").layout.buildDirectory
                .dir("composeAndroidAssets").get().asFile.absolutePath
        )
    }
}

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

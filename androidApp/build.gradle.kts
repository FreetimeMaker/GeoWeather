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
        versionName = "3.0.0"
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

dependencies {
    implementation(project(":shared"))
    implementation("org.jetbrains.compose.components:components-resources:1.6.11") // Use appropriate version or from libs
    implementation(libs.androidx.glance.appwidget)
    implementation(libs.androidx.glance.material3)
    implementation("androidx.work:work-runtime-ktx:2.11.2")
    implementation("androidx.activity:activity-compose:1.13.0")
    implementation(project(":SDK:SDK"))
    implementation(libs.room.runtime)
    implementation(libs.sqlite.bundled)
    
    // Compose dependencies for Android
    val composeBom = platform("androidx.compose:compose-bom:2025.02.00")
    implementation(composeBom)
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.compose.material:material-icons-extended")
}

kotlin {
    compilerOptions {
        jvmTarget = JvmTarget.JVM_11
    }
}

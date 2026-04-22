plugins {
    alias(libs.plugins.kotlinMultiplatform)
    id("com.android.application")
    id("com.google.devtools.ksp")
    id("androidx.room")
    alias(libs.plugins.composeCompiler)
}

kotlin {
    androidTarget()

    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation(project(":composeApp"))
            }
        }
        val androidMain by getting {
            dependencies {
                implementation("androidx.core:core-ktx:1.18.0")
                implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.10.0")
                implementation("androidx.appcompat:appcompat:1.7.1")
                implementation(platform("androidx.compose:compose-bom:2024.10.00"))
                implementation("androidx.compose.ui:ui")
                implementation("androidx.compose.ui:ui-graphics")
                implementation("androidx.compose.ui:ui-tooling-preview")
                implementation("androidx.compose.material3:material3")
                implementation("androidx.compose.material:material-icons-extended")
                implementation("androidx.activity:activity-compose:1.13.0")
                implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.0")
                debugImplementation("androidx.compose.ui:ui-tooling")
                debugImplementation("androidx.compose.ui:ui-test-manifest")
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.10.2")
                implementation("org.jetbrains.kotlinx:kotlinx-datetime:0.7.1")
                implementation("com.russhwolf:multiplatform-settings:1.3.0")
                implementation("androidx.room:room-runtime:2.8.4")
                implementation("androidx.sqlite:sqlite-bundled:2.6.2")
            }
        }
    }
}

android {
    namespace = "com.freetime.geoweather"
    compileSdk = libs.versions.android.compileSdk.get().toInt()

    defaultConfig {
        applicationId = "com.freetime.geoweather"
        minSdk = libs.versions.android.minSdk.get().toInt()
        targetSdk = libs.versions.android.compileSdk.get().toInt()
        versionCode = 37
        versionName = "1.3.6"
    }

    buildFeatures {
        compose = true
    }

    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.14"
    }
}

room {
    schemaDirectory("$projectDir/schemas")
}

dependencies {
    add("kspAndroid", "androidx.room:room-compiler:2.8.4")
}

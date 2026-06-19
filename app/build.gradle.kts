plugins {
    id("com.android.application")
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.ksp)
    alias(libs.plugins.room)
    kotlin("plugin.serialization")
}

android {
    namespace = "com.freetime.geoweather"
    compileSdk = 37

    defaultConfig {
        applicationId = "com.freetime.geoweather"
        minSdk = 26
        targetSdk = 37
        versionCode = 58
        versionName = "2.0.0"
    }

    buildFeatures {
        compose = true
    }

    lint {
        baseline = file("lint-baseline.xml")
        disable += "NewApi"
    }
}

room {
    schemaDirectory("$projectDir/schemas")
}

dependencies {
    implementation("io.coil-kt:coil-compose:2.7.0")
    implementation("androidx.core:core-ktx:1.19.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.10.0")
    implementation("androidx.appcompat:appcompat:1.7.1")
    implementation(platform("androidx.compose:compose-bom:2026.05.01"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.compose.runtime:runtime-livedata:1.11.2")
    implementation("androidx.activity:activity-compose:1.13.0")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.11.0")
    implementation("androidx.work:work-runtime-ktx:2.11.2")
    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest:1.11.2")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.11.0")
    implementation("org.jetbrains.kotlinx:kotlinx-datetime:0.8.0-0.6.x-compat")
    implementation("com.russhwolf:multiplatform-settings:1.3.0")
    implementation(libs.ktor.client.okhttp)
    implementation(libs.ktor.client.content.negotiation)
    implementation(libs.ktor.serialization.kotlinx.json)
    implementation(libs.room.runtime)
    implementation(libs.sqlite.bundled)
    implementation(libs.androidx.glance.appwidget)
    implementation(libs.androidx.glance.material3)
    ksp(libs.room.compiler)
}

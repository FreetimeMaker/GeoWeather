import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidMultiplatformLibrary)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.kotlinSerialization)
    alias(libs.plugins.ksp)
    alias(libs.plugins.room)
}

compose.resources {
    publicResClass = true
}

kotlin {
    android {
        namespace = "com.freetime.geoweather.shared"
        compileSdk = 37
        minSdk = 26
        
        compilerOptions {
            jvmTarget = JvmTarget.JVM_11
        }
    }
    
    jvm()
    
    sourceSets {
        commonMain.dependencies {
            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.material3)
            implementation(compose.ui)
            implementation(compose.materialIconsExtended)
            implementation(compose.components.resources)
            implementation(compose.components.uiToolingPreview)
            
            implementation("io.ktor:ktor-client-core:3.5.2")
            implementation("io.ktor:ktor-client-content-negotiation:3.5.2")
            implementation("io.ktor:ktor-serialization-kotlinx-json:3.5.2")
            implementation(libs.room.runtime)
            implementation(libs.sqlite.bundled)
            implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.11.0")
            
            implementation("org.jetbrains.kotlinx:kotlinx-datetime:0.8.0-0.6.x-compat")
            implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.11.0")
            api("com.russhwolf:multiplatform-settings:1.3.0")
            implementation(libs.androidx.lifecycle.viewmodel)
            implementation(libs.androidx.lifecycle.runtime)
            implementation(project(":SDK:SDK"))
        }
        
        androidMain.dependencies {
            implementation("io.ktor:ktor-client-okhttp:3.5.2")
            implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.11.0")
        }
        
        jvmMain.dependencies {
            implementation(compose.desktop.currentOs)
            implementation("io.ktor:ktor-client-okhttp:3.5.2")
        }
    }
}

room {
    schemaDirectory("$projectDir/schemas")
}

dependencies {
    add("kspAndroid", libs.room.compiler)
    add("kspJvm", libs.room.compiler)
}

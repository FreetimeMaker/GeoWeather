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
            api(compose.runtime)
            api(compose.foundation)
            api(compose.material3)
            api(compose.ui)
            api(compose.materialIconsExtended)
            api(compose.components.resources)
            implementation(compose.components.uiToolingPreview)
            
            implementation("io.ktor:ktor-client-core:3.5.2")
            implementation("io.ktor:ktor-client-content-negotiation:3.5.2")
            implementation("io.ktor:ktor-serialization-kotlinx-json:3.5.2")
            api(libs.room.runtime)
            api(libs.sqlite.bundled)
            implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.11.0")
            
            implementation("org.jetbrains.kotlinx:kotlinx-datetime:0.8.0-0.6.x-compat")
            implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.11.0")
            api("com.russhwolf:multiplatform-settings:1.3.0")
            implementation(libs.androidx.lifecycle.viewmodel)
            implementation(libs.androidx.lifecycle.runtime)
            api(project(":freetime-sdk"))
        }
        
        androidMain.dependencies {
            implementation("io.ktor:ktor-client-okhttp:3.5.2")
            implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.11.0")
            implementation("androidx.appcompat:appcompat:1.7.0")
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

// Workaround for a Compose Multiplatform 1.12.0 + AGP 9.x gap:
// with the `com.android.kotlin.multiplatform.library` plugin,
// `variant.sources.assets` is null, so the Compose plugin cannot wire
// its `copy...ComposeResourcesToAndroidAssets` task into packaging.
// The AAR/APK would end up WITHOUT any compose resources, crashing on
// device with MissingResourceException. Instead we assemble the same
// asset tree ourselves; :androidApp merges it (see androidApp/build.gradle.kts).
val assembleAndroidComposeAssets = tasks.register("assembleAndroidComposeAssets", Copy::class) {
    dependsOn(
        "prepareComposeResourcesTaskForCommonMain",
        "convertXmlValueResourcesForCommonMain",
        "copyNonXmlValueResourcesForCommonMain"
    )
    into(layout.buildDirectory.dir("composeAndroidAssets"))
    // NB: the two tasks below output the bare `values*/drawable` tree;
    // the `composeResources/<package>` prefix (which
    // DefaultAndroidResourceReader looks up at runtime) is added here.
    into("composeResources/geoweather.shared.generated.resources") {
        from(tasks.named("convertXmlValueResourcesForCommonMain"))
        from(tasks.named("copyNonXmlValueResourcesForCommonMain"))
    }
}

dependencies {
    add("kspAndroid", libs.room.compiler)
    add("kspJvm", libs.room.compiler)
}

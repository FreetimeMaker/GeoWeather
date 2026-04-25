# Keep multiplatform-settings
-keep class com.russhwolf.settings.** { *; }
-keep interface com.russhwolf.settings.** { *; }

# Keep Ktor and OkHttp
-keep class io.ktor.** { *; }
-keep class okhttp3.** { *; }
-keep class okio.** { *; }

# Keep serialization
-keep class kotlinx.serialization.** { *; }

# Keep all our application code
-keep class com.freetime.geoweather.** { *; }
-keep class com.freetime.geoweather.data.** { *; }
-keep class com.freetime.geoweather.ui.** { *; }
-keep class com.freetime.geoweather.network.** { *; }

# Keep the entry point
-keep class com.freetime.geoweather.MainKt {
    public static void main(java.lang.String[]);
}

# Keep generated Compose Resources
-keep class **.Res { *; }
-keep class **.Res$* { *; }
-keep class geoweather.composeapp.generated.resources.** { *; }
-keep class org.jetbrains.compose.resources.** { *; }

# Necessary for Compose Desktop
-keepattributes *Annotation*
-keepattributes Signature
-keepattributes InnerClasses
-keepattributes EnclosingMethod

# Desktop specific keeps
-keep class androidx.compose.ui.window.** { *; }
-keep class org.jetbrains.skiko.** { *; }
-keep class java.awt.** { *; }
-keep class javax.swing.** { *; }

-dontnote **
-dontwarn **
-ignorewarnings

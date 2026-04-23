# ProGuard rules for GeoWeather Compose Desktop

# Ignore all warnings to allow the build to proceed despite missing optional dependencies
-ignorewarnings
-dontnote **
-dontwarn **

# Keep our application code
-keep class com.freetime.geoweather.** { *; }

# Keep Kotlin and Compose internal classes that might be accessed via reflection
-keep class kotlin.** { *; }
-keep class kotlinx.** { *; }
-keep class androidx.compose.** { *; }
-keep class org.jetbrains.skiko.** { *; }

# Necessary for Compose Desktop
-keepattributes *Annotation*
-keepattributes Signature
-keepattributes InnerClasses
-keepattributes EnclosingMethod

# Keep the main class
-keep class com.freetime.geoweather.MainKt {
    public static void main(java.lang.String[]);
}

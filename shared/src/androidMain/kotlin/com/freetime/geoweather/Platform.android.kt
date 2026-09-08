package com.freetime.geoweather

import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.location.LocationManager
import android.net.Uri
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.core.os.LocaleListCompat

private var androidContext: Context? = null

fun setAndroidContext(context: Context) {
    androidContext = context
}

actual fun openUrl(url: String) {
    androidContext?.let { context ->
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }
}

@Composable
actual fun rememberPaymentContext(): Any? {
    return LocalContext.current as? Activity ?: androidContext
}

actual fun applyAppLanguage(language: String) {
    val tags = if (language == "system") "" else language
    AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(tags))
}

actual fun copyToClipboard(text: String) {
    val context = androidContext ?: return
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager ?: return
    clipboard.setPrimaryClip(ClipData.newPlainText("GeoWeather", text))
}

actual suspend fun getCurrentCoordinates(): Pair<Double, Double>? {
    return try {
        val context = androidContext ?: return null
        val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
            ?: return null
        val providers = locationManager.getProviders(true)
        var best: android.location.Location? = null
        for (provider in providers) {
            val location = try {
                locationManager.getLastKnownLocation(provider)
            } catch (e: SecurityException) {
                return null
            }
            if (location != null && (best == null || location.accuracy < best.accuracy)) {
                best = location
            }
        }
        best?.let { it.latitude to it.longitude }
    } catch (e: Exception) {
        null
    }
}

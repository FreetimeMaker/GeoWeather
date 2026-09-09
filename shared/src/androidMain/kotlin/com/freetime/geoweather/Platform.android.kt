package com.freetime.geoweather

import android.Manifest
import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.location.Location
import android.location.LocationManager
import android.net.Uri
import androidx.annotation.RequiresPermission
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

private var androidContext: Context? = null

fun setAndroidContext(context: Context) {
    androidContext = context
}

internal fun getAndroidAppContext(): Context? = androidContext

actual fun openUrl(url: String) {
    androidContext?.let { context ->
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }
}

actual val isDesktop: Boolean = false

@Composable
actual fun rememberPaymentContext(): Any? {
    return LocalContext.current as? Activity ?: androidContext
}

actual fun copyToClipboard(text: String) {
    val context = androidContext ?: return
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager ?: return
    clipboard.setPrimaryClip(ClipData.newPlainText(null, text))
}

@RequiresPermission(anyOf = [Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION])
actual suspend fun getCurrentCoordinates(): Pair<Double, Double>? {
    return try {
        val context = androidContext ?: return null
        val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
            ?: return null
        val providers = locationManager.getProviders(true)
        var best: Location? = null
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

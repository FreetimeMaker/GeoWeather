package com.freetime.geoweather

import android.Manifest
import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.location.Location
import android.location.LocationManager
import android.location.Geocoder
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import android.net.Uri
import androidx.annotation.RequiresPermission
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

private var androidContext: Context? = null

fun setAndroidContext(context: Context) {
    androidContext = context
}

internal fun getAndroidAppContext(): Context? = androidContext

fun openUrl(url: String) {
    androidContext?.let { context ->
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }
}

@Composable
fun rememberPaymentContext(): Any? = LocalContext.current as? Activity ?: androidContext

fun copyToClipboard(text: String) {
    val context = androidContext ?: return
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager ?: return
    clipboard.setPrimaryClip(ClipData.newPlainText(null, text))
}

@RequiresPermission(anyOf = [Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION])
suspend fun getCurrentCoordinates(): Pair<Double, Double>? {
    return try {
        val context = androidContext ?: return null
        val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager ?: return null
        val providers = locationManager.getProviders(true)
        var best: Location? = null
        for (provider in providers) {
            val location = try {
                locationManager.getLastKnownLocation(provider)
            } catch (_: SecurityException) {
                return null
            }
            if (location != null && (best == null || location.accuracy < best.accuracy)) {
                best = location
            }
        }
        best?.let { it.latitude to it.longitude }
    } catch (_: Exception) {
        null
    }
}

@Suppress("DEPRECATION")
suspend fun getDetectedLocationName(latitude: Double, longitude: Double): String? = withContext(Dispatchers.IO) {
    val context = androidContext ?: return@withContext null
    if (!Geocoder.isPresent()) return@withContext null
    try {
        val address = Geocoder(context, Locale.getDefault())
            .getFromLocation(latitude, longitude, 1)
            ?.firstOrNull()
            ?: return@withContext null
        listOfNotNull(
            address.locality ?: address.subAdminArea ?: address.adminArea,
            address.adminArea?.takeUnless { it == address.locality || it == address.subAdminArea },
            address.countryName
        ).distinct().joinToString(", ").takeIf { it.isNotBlank() }
    } catch (_: Exception) {
        null
    }
}

var systemBackHandler: (() -> Boolean)? = null

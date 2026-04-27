package io.github.freetimemaker.geoweather

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import android.location.LocationManager
import io.github.freetimemaker.geoweather.data.LocationEntity
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

class AndroidLocationService(private val context: Context) : LocationService {
    @SuppressLint("MissingPermission")
    override suspend fun getCurrentLocation(): LocationEntity? {
        val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        
        return try {
            // Try GPS first, then Network
            val location = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)
                ?: locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)

            location?.let {
                LocationEntity(
                    name = "Current Location",
                    latitude = it.latitude,
                    longitude = it.longitude
                )
            }
        } catch (e: Exception) {
            null
        }
    }
}

package io.github.freetimemaker.geoweather

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import io.github.freetimemaker.geoweather.data.LocationEntity
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import kotlinx.coroutines.tasks.*

class AndroidLocationService(private val context: Context) : LocationService {
    @SuppressLint("MissingPermission")
    override suspend fun getCurrentLocation(): LocationEntity? {
        return try {
            val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)
            var location = fusedLocationClient.lastLocation.await()
            
            if (location == null) {
                location = fusedLocationClient.getCurrentLocation(
                    Priority.PRIORITY_BALANCED_POWER_ACCURACY,
                    CancellationTokenSource().token
                ).await()
            }

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

package com.freetime.geoweather

import android.annotation.SuppressLint
import android.content.Context
import com.freetime.geoweather.data.LocationEntity
import com.freetime.geoweather.data.appContext
import com.google.android.gms.location.LocationServices
import kotlinx.coroutines.tasks.await

class AndroidLocationService(private val context: Context) : LocationService {
    @SuppressLint("MissingPermission")
    override suspend fun getCurrentLocation(): LocationEntity? {
        return try {
            val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)
            val location = fusedLocationClient.lastLocation.await()
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

actual fun createLocationService(): LocationService = AndroidLocationService(appContext)

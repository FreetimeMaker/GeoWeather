package com.freetime.geoweather.domain

import kotlinx.serialization.Serializable

@Serializable
data class City(
    val name: String,
    val latitude: Double,
    val longitude: Double,
    val country: String? = null,
    val admin1: String? = null
)

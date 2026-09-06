package com.freetime.geoweather.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.datetime.Clock

@Entity(tableName = "weather_history")
data class WeatherHistoryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val location: String,
    val temperature: Double,
    val humidity: Double? = null,
    val pressure: Double? = null,
    val windSpeed: Double? = null,
    val conditions: String? = null,
    val timestamp: Long = kotlin.time.Clock.System.now().toEpochMilliseconds()
)

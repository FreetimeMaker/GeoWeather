package com.freetime.geoweather.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import com.freetime.geoweather.data.LocationDatabase
import com.freetime.geoweather.data.WeatherHistoryDao
import com.freetime.geoweather.data.WeatherHistoryEntity

class WeatherHistoryViewModel(application: Application) : AndroidViewModel(application) {

    private val weatherHistoryDao: WeatherHistoryDao = LocationDatabase.getDatabase(application).weatherHistoryDao()
    val history: LiveData<List<WeatherHistoryEntity>> = weatherHistoryDao.getAllHistory()

    fun recordWeatherData(location: String, temperature: Double, humidity: Double? = null,
                          pressure: Double? = null, windSpeed: Double? = null, conditions: String? = null) {
        val historyEntity = WeatherHistoryEntity(
            location = location,
            temperature = temperature,
            humidity = humidity,
            pressure = pressure,
            windSpeed = windSpeed,
            conditions = conditions
        )

        LocationDatabase.databaseWriteExecutor.execute {
            weatherHistoryDao.insertHistory(historyEntity)
        }
    }
}

package com.freetime.geoweather.data

import com.russhwolf.settings.Settings

object DependencyManager {
    private var database: WeatherDatabase? = null
    private var settings: Settings? = null
    private var repository: WeatherRepository? = null
    private var appSettings: AppSettings? = null

    fun initialize(context: android.content.Context, database: WeatherDatabase, settings: Settings) {
        this.database = database
        this.settings = settings
        this.repository = WeatherRepository(
            appContext = context.applicationContext,
            locationDao = database.locationDao(),
            historyDao = database.weatherHistoryDao(),
            forecastSnapshotDao = database.forecastSnapshotDao(),
            apiClient = WeatherApiClient()
        )
        this.appSettings = AppSettings(settings)
    }

    fun getRepository(): WeatherRepository {
        return repository ?: throw IllegalStateException("DependencyManager not initialized")
    }

    fun getAppSettings(): AppSettings {
        return appSettings ?: throw IllegalStateException("DependencyManager not initialized")
    }

    fun getDatabase(): WeatherDatabase {
        return database ?: throw IllegalStateException("DependencyManager not initialized")
    }
}

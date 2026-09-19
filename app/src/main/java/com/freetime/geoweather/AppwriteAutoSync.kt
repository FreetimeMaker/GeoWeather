package com.freetime.geoweather

import android.content.Context
import com.freetime.geoweather.data.AppSettings
import com.freetime.geoweather.data.WeatherRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.launch

object AppwriteAutoSync {
    private var job: Job? = null

    fun start(
        context: Context,
        scope: CoroutineScope,
        repository: WeatherRepository,
        settings: AppSettings
    ) {
        job?.cancel()
        job = scope.launch {
            combine(
                repository.getAllLocations(),
                settings.tempUnit,
                settings.windUnit,
                settings.pressureUnit,
                settings.persistentNotif,
                settings.tempThreshold,
                settings.windThreshold,
                settings.openExternalBrowser,
                settings.weatherAnimations
            ) { values -> values.contentHashCode() }
                .drop(1)
                .collect {
                    delay(1200)
                    runCatching { AppwriteSync.push(context, repository, settings) }
                }
        }
    }

    fun stop() {
        job?.cancel()
        job = null
    }
}

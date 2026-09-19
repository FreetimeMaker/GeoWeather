package com.freetime.geoweather

import android.content.Context
import com.freetime.geoweather.data.AppSettings
import com.freetime.geoweather.data.WeatherRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge
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
            merge(
                repository.getAllLocations().map { Unit },
                settings.tempUnit.map { Unit },
                settings.windUnit.map { Unit },
                settings.pressureUnit.map { Unit },
                settings.persistentNotif.map { Unit },
                settings.tempThreshold.map { Unit },
                settings.windThreshold.map { Unit },
                settings.openExternalBrowser.map { Unit },
                settings.weatherAnimations.map { Unit }
            ).drop(9)
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

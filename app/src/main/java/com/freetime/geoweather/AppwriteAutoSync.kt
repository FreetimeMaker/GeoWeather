package com.freetime.geoweather

import android.content.Context
import com.freetime.geoweather.data.AppSettings
import com.freetime.geoweather.data.WeatherRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.debounce
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
                settings.useSystemTheme.map { Unit },
                settings.darkModeEnabled.map { Unit },
                settings.dynamicColor.map { Unit },
                settings.oledBlack.map { Unit },
                settings.persistentNotif.map { Unit },
                settings.tempThreshold.map { Unit },
                settings.windThreshold.map { Unit },
                settings.disablePrivateView.map { Unit },
                settings.openExternalBrowser.map { Unit },
                settings.weatherAnimations.map { Unit },
                settings.notificationProfile.map { Unit },
                settings.quietHours.map { Unit }
            ).drop(16)
                .debounce(1200)
                .collect {
                    runCatching { AppwriteSync.push(context, repository, settings) }
                }
        }
    }

    fun stop() {
        job?.cancel()
        job = null
    }
}

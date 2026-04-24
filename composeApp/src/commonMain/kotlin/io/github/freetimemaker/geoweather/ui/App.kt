package io.github.freetimemaker.geoweather.ui

import androidx.compose.runtime.*
import io.github.freetimemaker.geoweather.LocationService
import io.github.freetimemaker.geoweather.SettingsManager
import io.github.freetimemaker.geoweather.data.LocationDao
import io.github.freetimemaker.geoweather.data.LocationEntity
import io.github.freetimemaker.geoweather.network.WeatherApi
import io.github.freetimemaker.geoweather.ui.theme.GeoWeatherTheme
import kotlinx.coroutines.launch

@Composable
fun App(
    locationDao: LocationDao,
    api: WeatherApi,
    settingsManager: SettingsManager,
    locationService: LocationService
) {
    val locationsViewModel = remember { LocationsViewModel(locationDao) }
    val weatherViewModel = remember { WeatherViewModel(api, locationDao) }
    val coroutineScope = rememberCoroutineScope()

    var currentScreen by remember { mutableStateOf<Screen>(Screen.Main) }

    val useSystemTheme by settingsManager.useSystemTheme.collectAsState()
    val darkModeSetting by settingsManager.darkModeEnabled.collectAsState()
    val dynamicColor by settingsManager.dynamicColor.collectAsState()

    val darkTheme = if (useSystemTheme) androidx.compose.foundation.isSystemInDarkTheme() else darkModeSetting

    GeoWeatherTheme(
        darkTheme = darkTheme,
        dynamicColor = dynamicColor
    ) {
        val screen = currentScreen
        when (screen) {
            is Screen.Main -> {
                MainScreen(
                    viewModel = locationsViewModel,
                    api = api,
                    onOpenDetail = { location ->
                        currentScreen = Screen.Detail(location)
                    },
                    onOpenSettings = {
                        currentScreen = Screen.Settings
                    },
                    onOpenDonate = {
                        currentScreen = Screen.Donate
                    },
                    onGetCurrentLocation = {
                        coroutineScope.launch {
                            locationService.getCurrentLocation()?.let { loc ->
                                locationsViewModel.addLocation(loc.name, loc.latitude, loc.longitude)
                            }
                        }
                    }
                )
            }
            is Screen.Detail -> {
                WeatherDetailScreen(
                    name = screen.location.name,
                    lat = screen.location.latitude,
                    lon = screen.location.longitude,
                    viewModel = weatherViewModel,
                    settingsManager = settingsManager,
                    onBack = {
                        currentScreen = Screen.Main
                    }
                )
            }
            is Screen.Settings -> {
                SettingsScreen(
                    settingsManager = settingsManager,
                    onBack = {
                        currentScreen = Screen.Main
                    },
                    onOpenChangeLog = {
                        currentScreen = Screen.ChangeLog
                    }
                )
            }
            is Screen.ChangeLog -> {
                ChangeLogScreen(
                    onBack = {
                        currentScreen = Screen.Settings
                    }
                )
            }
            is Screen.Donate -> {
                DonateScreen(
                    onBack = {
                        currentScreen = Screen.Main
                    }
                )
            }
        }
    }
}

sealed class Screen {
    object Main : Screen()
    data class Detail(val location: LocationEntity) : Screen()
    object Settings : Screen()
    object Donate : Screen()
    object ChangeLog : Screen()
}

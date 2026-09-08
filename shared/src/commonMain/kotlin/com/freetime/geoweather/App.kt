package com.freetime.geoweather

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import com.freetime.geoweather.data.*
import com.freetime.geoweather.ui.*
import com.freetime.geoweather.ui.theme.GeoWeatherTheme

sealed class Screen {
    data object Main : Screen()
    data object Search : Screen()
    data class Detail(val location: LocationEntity) : Screen()
    data object Settings : Screen()
    data object Donate : Screen()
    data object ChangeLog : Screen()
    data object Radar : Screen()
    data object About : Screen()
}

@Composable
fun WeatherApp(database: WeatherDatabase, appSettings: AppSettings) {
    val repository = remember {
        WeatherRepository(
            locationDao = database.locationDao(),
            historyDao = database.weatherHistoryDao(),
            apiClient = WeatherApiClient()
        )
    }
    val viewModel = remember { WeatherViewModel(repository) }
    var currentScreen by remember { mutableStateOf<Screen>(Screen.Main) }

    val useSystemTheme by appSettings.useSystemTheme.collectAsState()
    val darkModeEnabled by appSettings.darkModeEnabled.collectAsState()
    val dynamicColor by appSettings.dynamicColor.collectAsState()

    val darkTheme = if (useSystemTheme) isSystemInDarkTheme() else darkModeEnabled

    GeoWeatherTheme(darkTheme = darkTheme, dynamicColor = dynamicColor) {
        Surface(modifier = Modifier.fillMaxSize()) {
            when (val screen = currentScreen) {
                is Screen.Main -> {
                    MainWeatherScreen(
                        viewModel = viewModel,
                        onAddLocationClick = { currentScreen = Screen.Search },
                        onLocationClick = { currentScreen = Screen.Detail(it) },
                        onSettingsClick = { currentScreen = Screen.Settings },
                        onDonateClick = { currentScreen = Screen.Donate },
                        onRadarClick = { currentScreen = Screen.Radar }
                    )
                }
                is Screen.Search -> {
                    SearchScreen(
                        viewModel = viewModel,
                        onCitySelected = { currentScreen = Screen.Main },
                        onBack = { currentScreen = Screen.Main }
                    )
                }
                is Screen.Detail -> {
                    WeatherDetailScreen(
                        location = screen.location,
                        appSettings = appSettings,
                        onBack = { currentScreen = Screen.Main }
                    )
                }
                is Screen.Settings -> {
                    SettingsScreen(
                        appSettings = appSettings,
                        onBack = { currentScreen = Screen.Main },
                        onChangeLogClick = { currentScreen = Screen.ChangeLog },
                        onAboutClick = { currentScreen = Screen.About }
                    )
                }
                is Screen.Donate -> {
                    DonateScreen(
                        onBack = { currentScreen = Screen.Main }
                    )
                }
                is Screen.ChangeLog -> {
                    ChangeLogScreen(
                        onBack = { currentScreen = Screen.Main }
                    )
                }
                is Screen.Radar -> {
                    RadarScreen(
                        onBack = { currentScreen = Screen.Main }
                    )
                }
                is Screen.About -> {
                    AboutScreen(
                        onBack = { currentScreen = Screen.Main }
                    )
                }
            }
        }
    }
}

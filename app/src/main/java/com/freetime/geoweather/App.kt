package com.freetime.geoweather

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.freetime.geoweather.Screen.*
import com.freetime.geoweather.data.*
import com.freetime.geoweather.ui.*
import com.freetime.geoweather.ui.theme.GeoWeatherTheme

sealed class Screen {
    data object Main : Screen()
    data object Search : Screen()
    data class Detail(
        val locationId: Long = -1,
        val transientName: String? = null,
        val transientLat: Double = 0.0,
        val transientLon: Double = 0.0
    ) : Screen()
    data class Radar(val lat: Double, val lon: Double) : Screen()
    data class Web(val url: String, val title: String = "") : Screen()
    data object Settings : Screen()
    data object Donate : Screen()
    data object ChangeLog : Screen()
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
    val backStack = remember { mutableStateListOf<Screen>(Main) }
    fun navigate(screen: Screen) { backStack.add(screen) }
    fun goBack() { if (backStack.size > 1) backStack.removeLast() }

    DisposableEffect(Unit) {
        systemBackHandler = {
            if (backStack.size > 1) {
                backStack.removeLast()
                true
            } else {
                false
            }
        }
        onDispose { systemBackHandler = null }
    }

    val useSystemTheme by appSettings.useSystemTheme.collectAsState()
    val darkModeEnabled by appSettings.darkModeEnabled.collectAsState()
    val dynamicColor by appSettings.dynamicColor.collectAsState()
    val oledBlack by appSettings.oledBlack.collectAsState()

    val darkTheme = if (useSystemTheme) isSystemInDarkTheme() else darkModeEnabled

    GeoWeatherTheme(darkTheme = darkTheme, dynamicColor = dynamicColor, oledBlack = oledBlack) {
        Surface(modifier = Modifier.fillMaxSize()) {
            Box(Modifier.fillMaxSize()) {
                backStack.forEachIndexed { index, screen ->
                    key(index) {
                        val isTop = index == backStack.lastIndex
                        Box(
                            if (isTop) Modifier.fillMaxSize() else Modifier.size(0.dp)
                        ) {
                            ScreenContent(
                                screen = screen,
                                viewModel = viewModel,
                                appSettings = appSettings,
                                onNavigate = ::navigate,
                                onGoBack = ::goBack
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ScreenContent(
    screen: Screen,
    viewModel: WeatherViewModel,
    appSettings: AppSettings,
    onNavigate: (Screen) -> Unit,
    onGoBack: () -> Unit
) {
    when (screen) {
        is Main -> {
            MainWeatherScreen(
                viewModel = viewModel,
                onAddLocationClick = { onNavigate(Search) },
                onLocationClick = { onNavigate(Detail(locationId = it.id)) },
                onSettingsClick = { onNavigate(Settings) },
                onDonateClick = { onNavigate(Donate) },
                onCurrentLocationClick = { name, lat, lon ->
                    onNavigate(Detail(transientName = name, transientLat = lat, transientLon = lon))
                }
            )
        }
        is Search -> {
            SearchScreen(
                viewModel = viewModel,
                onCitySelected = { onGoBack() },
                onBack = { onGoBack() }
            )
        }
        is Detail -> {
            WeatherDetailScreen(
                locationId = screen.locationId,
                transientName = screen.transientName,
                transientLat = screen.transientLat,
                transientLon = screen.transientLon,
                viewModel = viewModel,
                appSettings = appSettings,
                onBack = { onGoBack() },
                onRadarClick = { lat, lon -> onNavigate(Radar(lat, lon)) }
            )
        }
        is Settings -> {
            SettingsScreen(
                viewModel = viewModel,
                appSettings = appSettings,
                onBack = { onGoBack() },
                onChangeLogClick = { onNavigate(ChangeLog) },
                onWebViewClick = { url, title -> onNavigate(Web(url, title)) },
            )
        }
        is Donate -> {
            DonateScreen(
                onBack = { onGoBack() },
                onWebViewClick = { url, title -> onNavigate(Web(url, title)) }
            )
        }
        is ChangeLog -> {
            ChangeLogScreen(
                onBack = { onGoBack() }
            )
        }
        is Radar -> {
            RadarScreen(
                lat = screen.lat,
                lon = screen.lon,
                onBack = { onGoBack() }
            )
        }
        is Web -> {
            WebScreen(
                url = screen.url,
                title = screen.title,
                onBack = { onGoBack() }
            )
        }
    }
}

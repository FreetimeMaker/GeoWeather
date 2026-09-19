package com.freetime.geoweather

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
    data class HourlyDetail(val locationName: String, val forecasts: List<HourlyForecast>, val index: Int) : Screen()
    data class DailyDetail(val locationName: String, val forecasts: List<DailyForecast>, val index: Int) : Screen()
    data class Radar(val lat: Double, val lon: Double) : Screen()
    data class Web(val url: String, val title: String = "") : Screen()
    data object Settings : Screen()
    data object Donate : Screen()
    data object ChangeLog : Screen()
    data object Auth : Screen()
    data object Subscriptions : Screen()
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

    // Theme follows daylight automatically: light from sunrise, dark from sunset.
    // Until weather data is available, use a conservative 07:00/19:00 fallback.
    val locations by viewModel.locations.collectAsState(initial = emptyList())
    val themeLocation = locations.firstOrNull()
    val daily = remember(themeLocation?.weatherData) {
        themeLocation?.let { viewModel.getDailyForecasts(it) }.orEmpty()
    }
    val today = daily.firstOrNull()
    fun parseSunTime(value: String?): java.time.LocalTime? {
        if (value.isNullOrBlank()) return null
        return runCatching {
            java.time.LocalTime.parse(value.takeLast(5))
        }.getOrNull()
    }
    val sunrise = parseSunTime(today?.sunrise)
    val sunset = parseSunTime(today?.sunset)
    var currentTime by remember { mutableStateOf(java.time.LocalTime.now()) }
    LaunchedEffect(Unit) {
        while (true) {
            currentTime = java.time.LocalTime.now()
            kotlinx.coroutines.delay(30_000)
        }
    }
    val darkTheme = if (sunrise != null && sunset != null) {
        currentTime.isBefore(sunrise) || !currentTime.isBefore(sunset)
    } else {
        currentTime.isBefore(java.time.LocalTime.of(7, 0)) ||
            !currentTime.isBefore(java.time.LocalTime.of(19, 0))
    }

    GeoWeatherTheme(darkTheme = darkTheme) {
        Surface(modifier = Modifier.fillMaxSize(), color = androidx.compose.ui.graphics.Color.Transparent) {
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
                onRadarClick = { lat, lon -> onNavigate(Radar(lat, lon)) },
                onHourlyClick = { locationName, forecasts, index -> onNavigate(HourlyDetail(locationName, forecasts, index)) },
                onDailyClick = { locationName, forecasts, index -> onNavigate(DailyDetail(locationName, forecasts, index)) }
            )
        }
        is HourlyDetail -> {
            ForecastDetailScreen(
                locationName = screen.locationName,
                hourlyForecasts = screen.forecasts,
                dailyForecasts = emptyList(),
                initialIndex = screen.index,
                appSettings = appSettings,
                onBack = { onGoBack() }
            )
        }
        is DailyDetail -> {
            ForecastDetailScreen(
                locationName = screen.locationName,
                hourlyForecasts = emptyList(),
                dailyForecasts = screen.forecasts,
                initialIndex = screen.index,
                appSettings = appSettings,
                onBack = { onGoBack() }
            )
        }
        is Settings -> {
            SettingsScreen(
                viewModel = viewModel,
                appSettings = appSettings,
                onBack = { onGoBack() },
                onChangeLogClick = { onNavigate(ChangeLog) },
                onWebViewClick = { url, title -> onNavigate(Web(url, title)) },
                onAccountClick = { onNavigate(Auth) },
                onSubscriptionsClick = { onNavigate(Subscriptions) }
            )
        }
        is Auth -> {
            AuthScreen(
                onAuthenticated = { onGoBack() },
                onBack = { onGoBack() }
            )
        }
        is Subscriptions -> {
            SubscriptionScreen(onBack = { onGoBack() })
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

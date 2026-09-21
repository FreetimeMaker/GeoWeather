package com.freetime.geoweather

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalContext
import com.freetime.geoweather.Screen.*
import com.freetime.geoweather.data.*
import com.freetime.geoweather.ui.*
import com.freetime.design.FreetimeApp
import com.freetime.design.FreetimeAppConfig
import com.freetime.design.FreetimeThemeMode

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
    data object Diagnostics : Screen()
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
    val context = LocalContext.current
    val launchPrefs = remember { context.getSharedPreferences("launch_state", android.content.Context.MODE_PRIVATE) }
    val appVersion = remember {
        runCatching { context.packageManager.getPackageInfo(context.packageName, 0).versionName }.getOrNull() ?: "unknown"
    }
    var onboarding by remember { mutableStateOf(!launchPrefs.getBoolean("onboarding_done", false)) }
    var whatsNew by remember {
        mutableStateOf(!onboarding && launchPrefs.getString("last_seen_version", "") != appVersion)
    }
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

    val weatherCode = themeLocation?.currentWeatherCode
    val weatherIsDay = themeLocation?.isDay ?: !darkTheme
    val adaptiveGlassColors = remember(weatherCode, weatherIsDay, darkTheme) {
        weatherGlassBackdrop(weatherCode, weatherIsDay, darkTheme)
    }

    FreetimeApp(
        config = FreetimeAppConfig(
            themeMode = if (darkTheme) FreetimeThemeMode.DARK else FreetimeThemeMode.LIGHT,
            backdropColors = adaptiveGlassColors
        )
    ) {
        if (onboarding) {
            OnboardingScreen(onDone = {
                launchPrefs.edit().putBoolean("onboarding_done", true).apply()
                onboarding = false
                whatsNew = true
            })
            return@FreetimeApp
        }
        if (whatsNew) {
            ChangeLogScreen(onBack = {
                launchPrefs.edit().putString("last_seen_version", appVersion).apply()
                backStack.clear()
                backStack.add(Main)
                whatsNew = false
            })
            return@FreetimeApp
        }
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
                onDiagnosticsClick = { onNavigate(Diagnostics) }
            )
        }
        is Diagnostics -> {
            DiagnosticsScreen(onBack = { onGoBack() })
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


private fun weatherGlassBackdrop(code: Int?, isDay: Boolean, darkTheme: Boolean): List<Color> {
    if (!isDay || darkTheme) {
        return when (code) {
            95, 96, 99 -> listOf(Color(0xFF090A18), Color(0xFF201538), Color(0xFF080910))
            71, 73, 75, 77, 85, 86 -> listOf(Color(0xFF101824), Color(0xFF243447), Color(0xFF0A1018))
            45, 48 -> listOf(Color(0xFF11171B), Color(0xFF273238), Color(0xFF0C1013))
            51, 53, 55, 56, 57, 61, 63, 65, 66, 67, 80, 81, 82 ->
                listOf(Color(0xFF08141F), Color(0xFF16344A), Color(0xFF081018))
            else -> listOf(Color(0xFF07111F), Color(0xFF102A46), Color(0xFF070A10))
        }
    }

    return when (code) {
        0 -> listOf(Color(0xFF75C8FF), Color(0xFFFFD27A), Color(0xFFBCE7FF))
        1, 2 -> listOf(Color(0xFF8CCFFF), Color(0xFFC8E8F6), Color(0xFFFFD89A))
        3 -> listOf(Color(0xFF9CB4C5), Color(0xFFD5DEE4), Color(0xFFB9C9D3))
        45, 48 -> listOf(Color(0xFFB9C7C9), Color(0xFFE1E7E5), Color(0xFFAABABC))
        51, 53, 55, 56, 57, 61, 63, 65, 66, 67, 80, 81, 82 ->
            listOf(Color(0xFF5E9FC7), Color(0xFF9EC6D8), Color(0xFF6F91AA))
        71, 73, 75, 77, 85, 86 -> listOf(Color(0xFFD8ECF7), Color(0xFFF7FBFF), Color(0xFFB8D5E7))
        95, 96, 99 -> listOf(Color(0xFF48557A), Color(0xFF746B9A), Color(0xFF34445D))
        else -> listOf(Color(0xFF83C9F4), Color(0xFFD8EEF7), Color(0xFFB5DCEC))
    }
}

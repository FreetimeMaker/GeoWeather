package com.freetime.geoweather

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.freetime.geoweather.Screen.*
import com.freetime.geoweather.R as Res
import com.freetime.geoweather.data.*
import com.freetime.geoweather.ui.*
import com.freetime.design.FreetimeApp
import com.freetime.design.FreetimeAppConfig
import com.freetime.design.FreetimeThemeMode
import com.freetime.core.FreetimePreferences
import com.freetime.design.rememberFreetimePreferencesController
import com.freetime.warn.FreetimeWarn
import com.freetime.warn.FreetimeWarnContent
import com.freetime.warn.FreetimeWarnFrequency
import com.freetime.warn.rememberFreetimeWarnState

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
    data class DailyDetail(
        val locationName: String,
        val dailyForecasts: List<DailyForecast>,
        val hourlyForecasts: List<HourlyForecast>,
        val index: Int
    ) : Screen()
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
            forecastSnapshotDao = database.forecastSnapshotDao(),
            apiClient = WeatherApiClient()
        )
    }
    val viewModel = remember { WeatherViewModel(repository) }
    val context = LocalContext.current
    val freetimePreferences = remember { FreetimePreferences.from(context, "geoweather_freetime_preferences") }
    val freetimePreferencesController = rememberFreetimePreferencesController(freetimePreferences)
    val freetimeWarning = rememberFreetimeWarnState(
        context = context,
        appName = "GeoWeather",
        versionCode = runCatching {
            val info = context.packageManager.getPackageInfo(context.packageName, 0)
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) info.longVersionCode
            else @Suppress("DEPRECATION") info.versionCode.toLong()
        }.getOrDefault(0L),
        warningId = "android-distribution-notice",
        frequency = FreetimeWarnFrequency.ONCE_PER_VERSION
    )
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
    val systemDarkTheme = isSystemInDarkTheme()
    val darkTheme = if (sunrise != null && sunset != null) {
        currentTime.isBefore(sunrise) || !currentTime.isBefore(sunset)
    } else {
        systemDarkTheme
    }

    val glassBackdropColors = remember(darkTheme) {
        if (darkTheme) {
            listOf(Color(0xFF090909), Color(0xFF101010), Color(0xFF080808))
        } else {
            listOf(Color(0xFFF4F4F4), Color(0xFFEEEEEE), Color(0xFFF8F8F8))
        }
    }

    FreetimeApp(
        controller = freetimePreferencesController,
        modifierConfig = { config ->
            config.copy(
                themeMode = if (darkTheme) FreetimeThemeMode.DARK else FreetimeThemeMode.LIGHT,
                backdropColors = glassBackdropColors
            )
        }
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
                                onGoBack = ::goBack,
                                onShowDistributionNotice = { freetimeWarning.show() }
                            )
                        }
                    }
                }
            }
        val warnTitle = stringResource(Res.string.freetime_warn_title)
        val warnMessage = stringResource(Res.string.freetime_warn_message, "GeoWeather")
        val warnLearnMore = stringResource(Res.string.freetime_warn_learn_more)
        val warnAcknowledge = stringResource(Res.string.freetime_warn_acknowledge)
        FreetimeWarn(
            state = freetimeWarning,
            onLearnMore = {
                freetimeWarning.dismiss()
                navigate(Web("https://keepandroidopen.org", warnLearnMore))
            },
            content = FreetimeWarnContent(
                title = warnTitle,
                message = { warnMessage },
                detailsLabel = warnLearnMore,
                acknowledgeLabel = warnAcknowledge
            )
        )
    }
}

@Composable
private fun ScreenContent(
    screen: Screen,
    viewModel: WeatherViewModel,
    appSettings: AppSettings,
    onNavigate: (Screen) -> Unit,
    onGoBack: () -> Unit,
    onShowDistributionNotice: () -> Unit
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
                onDailyClick = { locationName, dailyForecasts, hourlyForecasts, index ->
                    onNavigate(DailyDetail(locationName, dailyForecasts, hourlyForecasts, index))
                }
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
                hourlyForecasts = screen.hourlyForecasts,
                dailyForecasts = screen.dailyForecasts,
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
                onDiagnosticsClick = { onNavigate(Diagnostics) },
                onShowDistributionNotice = onShowDistributionNotice
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
            val dataSaver by appSettings.dataSaver.collectAsState()
            RadarScreen(
                lat = screen.lat,
                lon = screen.lon,
                onBack = { onGoBack() },
                dataSaver = dataSaver
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


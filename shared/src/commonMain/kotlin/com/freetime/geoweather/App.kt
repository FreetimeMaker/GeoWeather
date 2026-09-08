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
    data class Detail(
        val locationId: Long = -1,
        val transientName: String? = null,
        val transientLat: Double = 0.0,
        val transientLon: Double = 0.0
    ) : Screen()
    data object Settings : Screen()
    data object Donate : Screen()
    data object Supporters : Screen()
    data object WalletAddresses : Screen()
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
    val backStack = remember { mutableStateListOf<Screen>(Screen.Main) }
    fun navigate(screen: Screen) { backStack.add(screen) }
    fun goBack() { if (backStack.size > 1) backStack.removeLast() }
    val currentScreen = backStack.last()

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
            when (val screen = currentScreen) {
                is Screen.Main -> {
                    MainWeatherScreen(
                        viewModel = viewModel,
                        appSettings = appSettings,
                        onAddLocationClick = { navigate(Screen.Search) },
                        onLocationClick = { navigate(Screen.Detail(it.id)) },
                        onSettingsClick = { navigate(Screen.Settings) },
                        onDonateClick = { navigate(Screen.Donate) },
                        onRadarClick = { navigate(Screen.Radar) },
                        onCurrentLocationClick = { name, lat, lon ->
                            navigate(Screen.Detail(transientName = name, transientLat = lat, transientLon = lon))
                        }
                    )
                }
                is Screen.Search -> {
                    SearchScreen(
                        viewModel = viewModel,
                        onCitySelected = { goBack() },
                        onBack = { goBack() }
                    )
                }
                is Screen.Detail -> {
                    WeatherDetailScreen(
                        locationId = screen.locationId,
                        transientName = screen.transientName,
                        transientLat = screen.transientLat,
                        transientLon = screen.transientLon,
                        viewModel = viewModel,
                        appSettings = appSettings,
                        onBack = { goBack() }
                    )
                }
                is Screen.Settings -> {
                    SettingsScreen(
                        appSettings = appSettings,
                        onBack = { goBack() },
                        onChangeLogClick = { navigate(Screen.ChangeLog) },
                        onAboutClick = { navigate(Screen.About) }
                    )
                }
                is Screen.Donate -> {
                    DonateScreen(
                        onBack = { goBack() },
                        onSupportersClick = { navigate(Screen.Supporters) },
                        onWalletAddressesClick = { navigate(Screen.WalletAddresses) }
                    )
                }
                is Screen.Supporters -> {
                    SupportersScreen(onBack = { goBack() })
                }
                is Screen.WalletAddresses -> {
                    WalletAddressesScreen(onBack = { goBack() })
                }
                is Screen.ChangeLog -> {
                    ChangeLogScreen(
                        onBack = { goBack() }
                    )
                }
                is Screen.Radar -> {
                    RadarScreen(
                        onBack = { goBack() }
                    )
                }
                is Screen.About -> {
                    AboutScreen(
                        onBack = { goBack() }
                    )
                }
            }
        }
    }
}

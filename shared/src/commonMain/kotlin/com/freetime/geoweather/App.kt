package com.freetime.geoweather

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.foundation.layout.Box
import androidx.compose.ui.Alignment
import com.freetime.geoweather.data.*
import com.freetime.geoweather.ui.*
import com.freetime.geoweather.auth.AuthManager
import com.freetime.geoweather.domain.SubscriptionManager
import io.github.jan.supabase.auth.status.SessionStatus
import kotlinx.coroutines.launch

private enum class AppScreen {
    MAIN,
    SEARCH,
    SETTINGS,
    BACKUP,
    CHANGELOG,
    SUPPORT
}

@Composable
fun WeatherApp(database: WeatherDatabase) {
    var error by remember { mutableStateOf<String?>(null) }
    
    val supabase = remember { 
        try {
            SupabaseConfig.createClient() 
        } catch (e: Exception) {
            error = "Supabase Init Failed: ${e.message}"
            null
        }
    }
    
    if (supabase == null) {
        Surface(modifier = Modifier.fillMaxSize()) {
            Text(error ?: "Initializing Supabase...")
        }
        return
    }

    val authManager = remember { AuthManager(supabase) }
    val subscriptionManager = remember { SubscriptionManager(supabase) }
    val platform = remember { getPlatform() }
    val scope = rememberCoroutineScope()
    
    val sessionStatus by authManager.sessionStatus.collectAsState()
    val currentTier by subscriptionManager.currentTier.collectAsState()

    val repository = remember {
        WeatherRepository(
            locationDao = database.locationDao(),
            historyDao = database.weatherHistoryDao(),
            apiClient = WeatherApiClient()
        )
    }
    val viewModel = remember { WeatherViewModel(repository) }
    val locations by viewModel.locations.collectAsState()

    LaunchedEffect(sessionStatus, currentTier) {
        if (sessionStatus is SessionStatus.Authenticated) {
            subscriptionManager.syncSubscription()
            viewModel.refreshWeather(currentTier)
        }
    }

    MaterialTheme {
        Surface(modifier = Modifier.fillMaxSize()) {
            when (sessionStatus) {
                is SessionStatus.Authenticated -> {
                    var screen by remember { mutableStateOf(AppScreen.MAIN) }
                    when (screen) {
                        AppScreen.SEARCH -> {
                        SearchScreen(
                            viewModel = viewModel,
                            onCitySelected = { screen = AppScreen.MAIN }
                        )
                        }
                        AppScreen.SETTINGS -> {
                            SettingsScreen(
                                subscriptionTier = currentTier,
                                onBack = { screen = AppScreen.MAIN },
                                onOpenBackup = { screen = AppScreen.BACKUP },
                                onOpenChangeLog = { screen = AppScreen.CHANGELOG },
                                onOpenSupport = { screen = AppScreen.SUPPORT }
                            )
                        }
                        AppScreen.BACKUP -> {
                            BackupScreen(
                                locations = locations,
                                platform = platform,
                                onImport = viewModel::importLocations,
                                onBack = { screen = AppScreen.SETTINGS }
                            )
                        }
                        AppScreen.CHANGELOG -> {
                            ChangeLogScreen(onBack = { screen = AppScreen.SETTINGS })
                        }
                        AppScreen.SUPPORT -> {
                            SupportScreen(
                                platform = platform,
                                onBack = { screen = AppScreen.SETTINGS }
                            )
                        }
                        AppScreen.MAIN -> {
                        MainWeatherScreen(
                            viewModel = viewModel,
                            subscriptionTier = currentTier,
                            locations = locations,
                            onLocationSelected = { viewModel.selectLocation(it, currentTier) },
                            onLocationDeleted = viewModel::deleteLocation,
                            onOpenRadar = { location ->
                                platform.openUrl("https://www.windy.com/?${location.latitude},${location.longitude},8")
                            },
                            onOpenSettings = { screen = AppScreen.SETTINGS },
                            onOpenSupport = { screen = AppScreen.SUPPORT },
                            onLogout = { scope.launch { authManager.logout() } },
                            onAddLocationClick = { screen = AppScreen.SEARCH }
                        )
                        }
                    }
                }
                SessionStatus.Initializing -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
                else -> {
                    LoginScreen(authManager)
                }
            }
        }
    }
}

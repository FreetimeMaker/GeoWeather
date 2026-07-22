package com.freetime.geoweather

import android.content.Context
import android.os.Bundle
import android.view.View
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.freetime.geoweather.ui.theme.GeoWeatherTheme

class ChangeLogActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        hideSystemBars()
        setContent {
            val sharedPreferences = remember { getSharedPreferences("geo_weather_prefs", Context.MODE_PRIVATE) }
            val useSystemTheme = sharedPreferences.collectAsState(key = "use_system_theme", defaultValue = true)
            val darkModeEnabled = sharedPreferences.collectAsState(key = "dark_mode_enabled", defaultValue = false)
            val dynamicColor = sharedPreferences.collectAsState(key = "dynamic_color", defaultValue = true)
            val oledBlack = sharedPreferences.collectAsState(key = "oled_black", defaultValue = false)
            
            val darkTheme = if (useSystemTheme.value) isSystemInDarkTheme() else darkModeEnabled.value
            
            GeoWeatherTheme(darkTheme = darkTheme, dynamicColor = dynamicColor.value, oledBlack = oledBlack.value) {
                ChangeLogScreen(onBack = { finish() })
            }
        }
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) {
            hideSystemBars()
        }
    }

    private fun hideSystemBars() {
        val windowInsetsController =
            WindowCompat.getInsetsController(window, window.decorView)
        windowInsetsController.systemBarsBehavior =
            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        windowInsetsController.hide(WindowInsetsCompat.Type.systemBars())
    }
}

@Composable
fun ReleaseCard(
    version: String,
    details: List<String>,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
    ) {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.Start
        ) {
            Text(
                text = version,
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(8.dp))

            details.forEach { line ->
                Text(
                    text = "• $line",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChangeLogScreen(onBack: () -> Unit) {
    val releases = listOf(
        "v2.2.1" to listOf(
            "Added ScrollView in FreetimeSDK Donation Screen",
            "Changed the E-Mail Address"
        ),
        "v2.2.0" to listOf(
            stringResource(R.string.cl_220_sdk),
            stringResource(R.string.cl_220_tools),
            stringResource(R.string.cl_220_lang),
            stringResource(R.string.cl_220_trans),
            stringResource(R.string.cl_220_fixes)
        ),
        "v2.1.2" to listOf(
            "Added Feedback System and removed Affiliate Links",
            stringResource(R.string.cl_212_russian),
            stringResource(R.string.cl_212_lang_select),
            stringResource(R.string.cl_212_persistent)
        ),
        "v2.1.1" to listOf(
            stringResource(R.string.cl_211_localization),
            stringResource(R.string.cl_211_stability)
        ),
        "v2.1.0" to listOf(
            stringResource(R.string.cl_210_forecast),
            stringResource(R.string.cl_210_charts),
            stringResource(R.string.cl_210_wind),
            stringResource(R.string.cl_210_radar),
            stringResource(R.string.cl_210_aqi),
            stringResource(R.string.cl_210_pollen),
            stringResource(R.string.cl_210_earthquake),
            stringResource(R.string.cl_210_backup),
            stringResource(R.string.cl_210_oled),
            stringResource(R.string.cl_210_moon),
            stringResource(R.string.cl_210_photography),
            stringResource(R.string.cl_210_technical),
            stringResource(R.string.cl_210_icons),
            stringResource(R.string.cl_210_scores),
            stringResource(R.string.cl_210_shortcuts),
            stringResource(R.string.cl_210_notifs),
            stringResource(R.string.cl_210_privacy),
            stringResource(R.string.cl_210_noads)
        ),
        "v2.0.0" to listOf(
            stringResource(R.string.cl_200_desc)
        ),
        "v1.9.0" to listOf(
            stringResource(R.string.cl_190_webview)
        ),
        "v1.8.1" to listOf(
            stringResource(R.string.cl_181_reset)
        ),
        "v1.8.0" to listOf(
            stringResource(R.string.cl_180_repository)
        ),
        "v1.7.2" to listOf(
            stringResource(R.string.cl_172_home),
            stringResource(R.string.cl_172_fallbacks),
            stringResource(R.string.cl_172_login),
            stringResource(R.string.cl_172_refresh)
        ),
        "v1.7.1" to listOf(
            stringResource(R.string.cl_171_weather),
            stringResource(R.string.cl_171_home_refresh),
            stringResource(R.string.cl_171_login_stable),
            stringResource(R.string.cl_171_tomorrow_icons),
            stringResource(R.string.cl_171_cache)
        ),
        "v1.7.0" to listOf(
            stringResource(R.string.cl_170_local),
            stringResource(R.string.cl_170_supabase),
            stringResource(R.string.cl_170_sync),
            stringResource(R.string.cl_170_providers),
            stringResource(R.string.cl_170_cache_display),
            stringResource(R.string.cl_170_fixes)
        ),
        "v1.6.0" to listOf(
            stringResource(R.string.cl_160_modrinth)
        ),
        "v1.5.8" to listOf(
            stringResource(R.string.cl_158_providers),
            stringResource(R.string.cl_158_tiers),
            stringResource(R.string.cl_158_loading),
            stringResource(R.string.cl_158_account),
            stringResource(R.string.cl_158_localization)
        ),
        "v1.5.7" to listOf(
            stringResource(R.string.cl_157_strings),
            stringResource(R.string.cl_157_localization),
            stringResource(R.string.cl_157_versioning)
        ),
        "v1.5.6" to listOf(
            stringResource(R.string.cl_156_auth),
            stringResource(R.string.cl_156_bg_sync),
            stringResource(R.string.cl_156_weather),
            stringResource(R.string.cl_156_translations),
            stringResource(R.string.cl_156_stability)
        ),
        "v1.5.5" to listOf(
            stringResource(R.string.cl_155_cloud),
            stringResource(R.string.cl_155_sync_btn),
            stringResource(R.string.cl_155_auto_sync),
            stringResource(R.string.cl_155_persistence)
        ),
        "v1.5.4" to listOf(
            stringResource(R.string.cl_154_profile),
            stringResource(R.string.cl_154_history),
            stringResource(R.string.cl_154_openmeteo),
            stringResource(R.string.cl_154_icons),
            stringResource(R.string.cl_154_coil)
        ),
        "v1.5.3" to listOf(
            stringResource(R.string.cl_153_github),
            stringResource(R.string.cl_153_optional),
            stringResource(R.string.cl_153_deeplink),
            stringResource(R.string.cl_153_merge),
            stringResource(R.string.cl_153_icons)
        ),
        "v1.5.2" to listOf(
            stringResource(R.string.cl_152_auth),
            stringResource(R.string.cl_152_login_setting),
            stringResource(R.string.cl_152_history),
            stringResource(R.string.cl_152_pro),
            stringResource(R.string.cl_152_api),
            stringResource(R.string.cl_152_charts),
            stringResource(R.string.cl_152_workers)
        ),
        "v1.5.1" to listOf(
            stringResource(R.string.cl_v143_fix_start)
        ),
        "v1.5.0" to listOf(
            stringResource(R.string.cl_v142_added_api)
        ),
        "v1.4.1" to listOf(
            stringResource(R.string.cl_v141_more_dons)
        ),
        "v1.4.0" to listOf(
            stringResource(R.string.cl_v140_api)
        ),
        "v1.3.9" to listOf(
            stringResource(R.string.cl_v139_fix_dons)
        ),
        "v1.3.8" to listOf(
            stringResource(R.string.cl_v138_ca)
        ),
        "v1.3.7" to listOf(
            stringResource(R.string.cl_v137_ci_cd),
            stringResource(R.string.cl_v137_signing),
            stringResource(R.string.cl_v137_wasm)
        ),
        "v1.3.6" to listOf(
            stringResource(R.string.cl_v136_auto_open),
            stringResource(R.string.cl_v136_default_location),
            stringResource(R.string.cl_v136_current_location),
            stringResource(R.string.cl_v136_degoogled),
            stringResource(R.string.cl_v136_ui_ux),
            stringResource(R.string.cl_v136_stability)
        ),
        "v1.3.5" to listOf(
            stringResource(R.string.cl_135_donations),
            stringResource(R.string.cl_135_system_theme)
        ),
        "v1.3.4" to listOf(
            stringResource(R.string.cl_material_you),
            stringResource(R.string.cl_unit_switching),
            stringResource(R.string.cl_multi_service),
            stringResource(R.string.cl_historical_data),
            stringResource(R.string.cl_forecast_enhanced),
            stringResource(R.string.cl_custom_notifs),
            stringResource(R.string.cl_widget_refresh),
            stringResource(R.string.cl_localization),
            stringResource(R.string.cl_moon_phase)
        ),
        "v1.3.3" to listOf(
            stringResource(R.string.cl_coordinate_search),
            stringResource(R.string.cl_state_in_search),
            stringResource(R.string.cl_material_you)
        ),
        "v1.3.2" to listOf(
            stringResource(R.string.AddedMoreDons),
            stringResource(R.string.HopefullyFixedFMSDK)
        ),
        "v1.3.1" to listOf(
            stringResource(R.string.FixActivities),
            stringResource(R.string.AddedMoonData),
            stringResource(R.string.changelog_remove_api_key)
        ),
        "v1.3.0" to listOf(
            stringResource(R.string.AddedMoonData),
            stringResource(R.string.FixFMSDK),
            stringResource(R.string.changelog_remove_coin)
        )
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.whats_new_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back_nav_desc))
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            releases.forEach { (version, details) ->
                ReleaseCard(version = version, details = details)
            }
        }
    }
}

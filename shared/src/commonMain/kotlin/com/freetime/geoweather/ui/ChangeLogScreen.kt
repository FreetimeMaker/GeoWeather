package com.freetime.geoweather.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import geoweather.shared.generated.resources.*
import org.jetbrains.compose.resources.stringResource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChangeLogScreen(onBack: () -> Unit) {
    val releases = listOf(
        "v3.0.0" to listOf(
            "Migrated to Kotlin Multiplatform (KMP)",
            "Added support for Windows and Linux",
            "Refactored Database to Room Multiplatform",
            "Refactored Networking to Ktor",
            "Updated UI to Compose Multiplatform",
            "New App Icon",
            "P.S.: Tell me with a Feedback what you think about the new UI and the new App Icon. I would love to hear your opinion!"
        ),
        "v2.3.0" to listOf(
            "Enhanced the Weather Widget with responsive hourly Forecast and made it better",
            "Added Safety Wrappers around FreetimeSDK's FreetimePay and fixed the Promotion Rendering in DonateScreen for Freetime SDK",
            "Improved FreetimeSDK Reliability by adding error handler and more for Promotions",
            "Updated E-Mail address for Feedback (The Provider (addy.io) I used for Sending it to the Mail Address didn't work anymore)",
            "Updated Dependencies"
        ),
        "v2.2.2" to listOf(
            "Expandable 16-day forecast for a cleaner UI",
            "Integrated In-App Weather Radar",
            "Optimized compact weather details grid",
            "Added m/s wind speed unit",
            "Cardinal wind direction text added"
        ),
        "v2.2.1" to listOf(
            stringResource(Res.string.cl_220_sdk),
            stringResource(Res.string.cl_220_tools),
            stringResource(Res.string.cl_220_lang),
            stringResource(Res.string.cl_220_trans),
            stringResource(Res.string.cl_220_fixes)
        ),
        "v2.1.2" to listOf(
            "Added Feedback System and removed Affiliate Links",
            stringResource(Res.string.cl_212_russian),
            stringResource(Res.string.cl_212_lang_select),
            stringResource(Res.string.cl_212_persistent)
        ),
        "v2.1.1" to listOf(
            stringResource(Res.string.cl_211_localization),
            stringResource(Res.string.cl_211_stability)
        ),
        "v2.1.0" to listOf(
            stringResource(Res.string.cl_210_forecast),
            stringResource(Res.string.cl_210_charts),
            stringResource(Res.string.cl_210_wind),
            stringResource(Res.string.cl_210_radar),
            stringResource(Res.string.cl_210_aqi),
            stringResource(Res.string.cl_210_pollen),
            stringResource(Res.string.cl_210_earthquake),
            stringResource(Res.string.cl_210_backup),
            stringResource(Res.string.cl_210_oled),
            stringResource(Res.string.cl_210_moon),
            stringResource(Res.string.cl_210_photography),
            stringResource(Res.string.cl_210_technical),
            stringResource(Res.string.cl_210_icons),
            stringResource(Res.string.cl_210_scores),
            stringResource(Res.string.cl_210_shortcuts),
            stringResource(Res.string.cl_210_notifs),
            stringResource(Res.string.cl_210_privacy),
            stringResource(Res.string.cl_210_noads)
        ),
        "v2.0.0" to listOf(
            stringResource(Res.string.cl_200_desc)
        ),
        "v1.9.0" to listOf(
            stringResource(Res.string.cl_190_webview)
        ),
        "v1.8.1" to listOf(
            stringResource(Res.string.cl_181_reset)
        ),
        "v1.8.0" to listOf(
            stringResource(Res.string.cl_180_repository)
        ),
        "v1.7.2" to listOf(
            stringResource(Res.string.cl_172_home),
            stringResource(Res.string.cl_172_fallbacks),
            stringResource(Res.string.cl_172_login),
            stringResource(Res.string.cl_172_refresh)
        ),
        "v1.7.1" to listOf(
            stringResource(Res.string.cl_171_weather),
            stringResource(Res.string.cl_171_home_refresh),
            stringResource(Res.string.cl_171_login_stable),
            stringResource(Res.string.cl_171_tomorrow_icons),
            stringResource(Res.string.cl_171_cache)
        ),
        "v1.7.0" to listOf(
            stringResource(Res.string.cl_170_local),
            stringResource(Res.string.cl_170_supabase),
            stringResource(Res.string.cl_170_sync),
            stringResource(Res.string.cl_170_providers),
            stringResource(Res.string.cl_170_cache_display),
            stringResource(Res.string.cl_170_fixes)
        ),
        "v1.6.0" to listOf(
            stringResource(Res.string.cl_160_modrinth)
        ),
        "v1.5.8" to listOf(
            stringResource(Res.string.cl_158_providers),
            stringResource(Res.string.cl_158_tiers),
            stringResource(Res.string.cl_158_loading),
            stringResource(Res.string.cl_158_account),
            stringResource(Res.string.cl_158_localization)
        ),
        "v1.5.7" to listOf(
            stringResource(Res.string.cl_157_strings),
            stringResource(Res.string.cl_157_localization),
            stringResource(Res.string.cl_157_versioning)
        ),
        "v1.5.6" to listOf(
            stringResource(Res.string.cl_156_auth),
            stringResource(Res.string.cl_156_bg_sync),
            stringResource(Res.string.cl_156_weather),
            stringResource(Res.string.cl_156_translations),
            stringResource(Res.string.cl_156_stability)
        ),
        "v1.5.5" to listOf(
            stringResource(Res.string.cl_155_cloud),
            stringResource(Res.string.cl_155_sync_btn),
            stringResource(Res.string.cl_155_auto_sync),
            stringResource(Res.string.cl_155_persistence)
        ),
        "v1.5.4" to listOf(
            stringResource(Res.string.cl_154_profile),
            stringResource(Res.string.cl_154_history),
            stringResource(Res.string.cl_154_openmeteo),
            stringResource(Res.string.cl_154_icons),
            stringResource(Res.string.cl_154_coil)
        ),
        "v1.5.3" to listOf(
            stringResource(Res.string.cl_153_github),
            stringResource(Res.string.cl_153_optional),
            stringResource(Res.string.cl_153_deeplink),
            stringResource(Res.string.cl_153_merge),
            stringResource(Res.string.cl_153_icons)
        ),
        "v1.5.2" to listOf(
            stringResource(Res.string.cl_152_auth),
            stringResource(Res.string.cl_152_login_setting),
            stringResource(Res.string.cl_152_history),
            stringResource(Res.string.cl_152_pro),
            stringResource(Res.string.cl_152_api),
            stringResource(Res.string.cl_152_charts),
            stringResource(Res.string.cl_152_workers)
        ),
        "v1.5.1" to listOf(
            stringResource(Res.string.cl_v143_fix_start)
        ),
        "v1.5.0" to listOf(
            stringResource(Res.string.cl_v142_added_api)
        ),
        "v1.4.1" to listOf(
            stringResource(Res.string.cl_v141_more_dons)
        ),
        "v1.4.0" to listOf(
            stringResource(Res.string.cl_v140_api)
        ),
        "v1.3.9" to listOf(
            stringResource(Res.string.cl_v139_fix_dons)
        ),
        "v1.3.8" to listOf(
            stringResource(Res.string.cl_v138_ca)
        ),
        "v1.3.7" to listOf(
            stringResource(Res.string.cl_v137_ci_cd),
            stringResource(Res.string.cl_v137_signing),
            stringResource(Res.string.cl_v137_wasm)
        ),
        "v1.3.6" to listOf(
            stringResource(Res.string.cl_v136_auto_open),
            stringResource(Res.string.cl_v136_default_location),
            stringResource(Res.string.cl_v136_current_location),
            stringResource(Res.string.cl_v136_degoogled),
            stringResource(Res.string.cl_v136_ui_ux),
            stringResource(Res.string.cl_v136_stability)
        ),
        "v1.3.5" to listOf(
            stringResource(Res.string.cl_135_donations),
            stringResource(Res.string.cl_135_system_theme)
        ),
        "v1.3.4" to listOf(
            stringResource(Res.string.cl_material_you),
            stringResource(Res.string.cl_unit_switching),
            stringResource(Res.string.cl_multi_service),
            stringResource(Res.string.cl_historical_data),
            stringResource(Res.string.cl_forecast_enhanced),
            stringResource(Res.string.cl_custom_notifs),
            stringResource(Res.string.cl_widget_refresh),
            stringResource(Res.string.cl_localization),
            stringResource(Res.string.cl_moon_phase)
        ),
        "v1.3.3" to listOf(
            stringResource(Res.string.cl_coordinate_search),
            stringResource(Res.string.cl_state_in_search),
            stringResource(Res.string.cl_material_you)
        ),
        "v1.3.2" to listOf(
            stringResource(Res.string.AddedMoreDons),
            stringResource(Res.string.HopefullyFixedFMSDK)
        ),
        "v1.3.1" to listOf(
            stringResource(Res.string.FixActivities),
            stringResource(Res.string.AddedMoonData),
            stringResource(Res.string.changelog_remove_api_key)
        ),
        "v1.3.0" to listOf(
            stringResource(Res.string.AddedMoonData),
            stringResource(Res.string.FixFMSDK),
            stringResource(Res.string.changelog_remove_coin)
        )
    )
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(Res.string.whats_new_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(Res.string.back_nav_desc))
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

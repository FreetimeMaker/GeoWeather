package com.freetime.geoweather.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.freetime.geoweather.R as Res
import com.freetime.geoweather.ui.glass.geoWeatherGlass

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChangeLogScreen(onBack: () -> Unit) {
    val releases = listOf(
        "v4.4.1" to listOf(
            stringResource(Res.string.cl_441_freetime_maven),
            stringResource(Res.string.cl_441_freetime_modules),
            stringResource(Res.string.cl_441_shared_glass),
            stringResource(Res.string.cl_441_shared_browser),
            stringResource(Res.string.cl_441_shared_donations),
            stringResource(Res.string.cl_441_backdrop_fix),
            stringResource(Res.string.cl_441_contrast),
            stringResource(Res.string.cl_441_fixes)
        ),
        "v4.3.0" to listOf(
            stringResource(Res.string.cl_430_liquid_glass),
            stringResource(Res.string.cl_430_dark_light),
            stringResource(Res.string.cl_430_forecast_16_days),
            stringResource(Res.string.cl_430_pull_refresh),
            stringResource(Res.string.cl_430_refresh_button),
            stringResource(Res.string.cl_430_location_sorting),
            stringResource(Res.string.cl_430_accounts_removed),
            stringResource(Res.string.cl_430_forecast_limits),
            stringResource(Res.string.cl_430_donation_fix),
            stringResource(Res.string.cl_430_weather_details),
            stringResource(Res.string.cl_430_notification_profiles),
            stringResource(Res.string.cl_430_quiet_hours),
            stringResource(Res.string.cl_430_onboarding),
            stringResource(Res.string.cl_430_diagnostics),
            stringResource(Res.string.cl_430_shortcuts),
            stringResource(Res.string.cl_430_deeplink),
            stringResource(Res.string.cl_430_whats_new),
            stringResource(Res.string.cl_430_freetime_maven),
            stringResource(Res.string.cl_430_freetime_modules),
            stringResource(Res.string.cl_430_shared_glass),
            stringResource(Res.string.cl_430_shared_browser),
            stringResource(Res.string.cl_430_shared_donations),
            stringResource(Res.string.cl_430_backdrop_fix),
            stringResource(Res.string.cl_430_fixes)
        ),
        "v4.2.0" to listOf(
            stringResource(Res.string.cl_420_briefings),
            stringResource(Res.string.cl_420_activity_windows),
            stringResource(Res.string.cl_420_travel),
            stringResource(Res.string.cl_420_location_groups),
            stringResource(Res.string.cl_420_accuracy),
            stringResource(Res.string.cl_420_provider_compare),
            stringResource(Res.string.cl_420_widgets),
            stringResource(Res.string.cl_420_sync_v3),
            stringResource(Res.string.cl_420_notifications),
            stringResource(Res.string.cl_420_quiet_hours),
            stringResource(Res.string.cl_420_onboarding),
            stringResource(Res.string.cl_420_diagnostics),
            stringResource(Res.string.cl_420_shortcuts),
            stringResource(Res.string.cl_420_deeplink),
            stringResource(Res.string.cl_420_whats_new)
        ),
        "v4.1.0" to listOf(
            stringResource(Res.string.cl_410_appwrite_auth),
            stringResource(Res.string.cl_410_oauth),
            stringResource(Res.string.cl_410_profile),
            stringResource(Res.string.cl_410_subscriptions),
            stringResource(Res.string.cl_410_codes),
            stringResource(Res.string.cl_410_plan_features),
            stringResource(Res.string.cl_410_plan_limits),
            stringResource(Res.string.cl_410_upgrade),
            stringResource(Res.string.cl_410_cloud_sync),
            stringResource(Res.string.cl_410_auto_sync),
            stringResource(Res.string.cl_410_sync_controls),
            stringResource(Res.string.cl_410_main_plan),
            stringResource(Res.string.cl_410_forecast_limits),
            stringResource(Res.string.cl_410_liquid),
            stringResource(Res.string.cl_410_sdk)
        ),
        "v4.0.3" to listOf(
            stringResource(Res.string.cl_401_fix_error),
            stringResource(Res.string.cl_403_new_liquid)
        ),
        "v4.0.2" to listOf(
            stringResource(Res.string.cl_401_fix_error)
        ),
        "v4.0.1" to listOf(
            stringResource(Res.string.cl_401_fix_error)
        ),
        "v4.0.0" to listOf(
            stringResource(Res.string.cl_400_liquid_glass),
            stringResource(Res.string.cl_400_weather_animations),
            stringResource(Res.string.cl_400_animation_settings),
            stringResource(Res.string.cl_400_auto_theme),
            stringResource(Res.string.cl_400_next_rain),
            stringResource(Res.string.cl_400_timeline),
            stringResource(Res.string.cl_400_sun_moon),
            stringResource(Res.string.cl_400_trip_forecast),
            stringResource(Res.string.cl_400_air_quality),
            stringResource(Res.string.cl_400_pollen),
            stringResource(Res.string.cl_400_uv),
            stringResource(Res.string.cl_400_compare),
            stringResource(Res.string.cl_400_share),
            stringResource(Res.string.cl_400_notifications),
            stringResource(Res.string.cl_400_notification_icon),
            stringResource(Res.string.cl_400_tile),
            stringResource(Res.string.cl_400_widget),
            stringResource(Res.string.cl_400_nav),
            stringResource(Res.string.cl_400_stars),
            stringResource(Res.string.cl_400_localization),
            stringResource(Res.string.cl_400_android),
            stringResource(Res.string.cl_400_stability)
        ),
        "v3.1.3" to listOf(
            stringResource(Res.string.cl_313_fix_linux)
        ),
        "v3.1.2" to listOf(
            stringResource(Res.string.cl_311_fix_fdroid)
        ),
        "v3.1.1" to listOf(
            stringResource(Res.string.cl_311_fix_fdroid)
        ),
        "v3.1.0" to listOf(
            stringResource(Res.string.cl_310_android_icon),
            stringResource(Res.string.cl_310_weather_time),
            stringResource(Res.string.cl_310_removed_sdk)
        ),
        "v3.0.0" to listOf(
            stringResource(Res.string.cl_300_kmp),
            stringResource(Res.string.cl_300_windows_linux),
            stringResource(Res.string.cl_300_room),
            stringResource(Res.string.cl_300_ktor),
            stringResource(Res.string.cl_300_compose),
            stringResource(Res.string.cl_300_app_icon),
            stringResource(Res.string.cl_300_feedback)
        ),
        "v2.3.0" to listOf(
            stringResource(Res.string.cl_230_widget),
            stringResource(Res.string.cl_230_sdk_safety),
            stringResource(Res.string.cl_230_sdk_reliability),
            stringResource(Res.string.cl_230_feedback_email),
            stringResource(Res.string.cl_230_dependencies)
        ),
        "v2.2.2" to listOf(
            stringResource(Res.string.cl_222_expandable_forecast),
            stringResource(Res.string.cl_222_radar),
            stringResource(Res.string.cl_222_details_grid),
            stringResource(Res.string.cl_222_ms_wind),
            stringResource(Res.string.cl_222_cardinal_direction)
        ),
        "v2.2.1" to listOf(
            stringResource(Res.string.cl_220_sdk),
            stringResource(Res.string.cl_220_tools),
            stringResource(Res.string.cl_220_lang),
            stringResource(Res.string.cl_220_trans),
            stringResource(Res.string.cl_220_fixes)
        ),
        "v2.1.2" to listOf(
            stringResource(Res.string.cl_212_feedback),
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
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                    .geoWeatherGlass(RoundedCornerShape(28.dp), interactive = false),
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent, titleContentColor = MaterialTheme.colorScheme.onSurface, navigationIconContentColor = MaterialTheme.colorScheme.onSurface, actionIconContentColor = MaterialTheme.colorScheme.onSurface),
                title = { Text(stringResource(Res.string.whats_new_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(Res.string.back_nav_desc))
                    }
                }
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            items(
                items = releases,
                key = { it.first }
            ) { (version, details) ->
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
        modifier = modifier.fillMaxWidth().geoWeatherGlass(RoundedCornerShape(24.dp), interactive = false),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent, contentColor = MaterialTheme.colorScheme.onSurface)
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

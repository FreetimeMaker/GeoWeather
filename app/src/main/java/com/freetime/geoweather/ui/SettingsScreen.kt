package com.freetime.geoweather.ui
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.clickable
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalContext
import android.content.Intent
import androidx.core.content.ContextCompat
import com.freetime.geoweather.WeatherForegroundService
import com.freetime.geoweather.data.AppSettings
import com.freetime.geoweather.data.BACKUP_FILE_NAME
import com.freetime.geoweather.data.BACKUP_MIME_TYPE
import com.freetime.geoweather.data.loadTextFile
import com.freetime.geoweather.data.saveTextFile
import com.freetime.geoweather.openUrl
import com.freetime.geoweather.R as Res
import com.freetime.design.liquidGlass
import com.freetime.design.liquidGlassCapsule
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: WeatherViewModel,
    appSettings: AppSettings,
    onBack: () -> Unit,
    onChangeLogClick: () -> Unit,
    onWebViewClick: (String, String) -> Unit,
    onDiagnosticsClick: () -> Unit,
    onShowDistributionNotice: () -> Unit
) {
    val tempUnit by appSettings.tempUnit.collectAsState()
    val windUnit by appSettings.windUnit.collectAsState()
    val pressureUnit by appSettings.pressureUnit.collectAsState()
    val persistentNotif by appSettings.persistentNotif.collectAsState()
    val tempThreshold by appSettings.tempThreshold.collectAsState()
    val windThreshold by appSettings.windThreshold.collectAsState()
    val disablePrivateView by appSettings.disablePrivateView.collectAsState()
    val openExternalBrowser by appSettings.openExternalBrowser.collectAsState()
    val weatherAnimations by appSettings.weatherAnimations.collectAsState()
    val notificationProfile by appSettings.notificationProfile.collectAsState()
    val quietHours by appSettings.quietHours.collectAsState()
    val smartRainAlert by appSettings.smartRainAlert.collectAsState()
    val smartWindAlert by appSettings.smartWindAlert.collectAsState()
    val smartFrostAlert by appSettings.smartFrostAlert.collectAsState()
    val smartUvAlert by appSettings.smartUvAlert.collectAsState()
    val dataSaver by appSettings.dataSaver.collectAsState()
    val offlinePacks by appSettings.offlinePacks.collectAsState()
    val offlinePacksWifiOnly by appSettings.offlinePacksWifiOnly.collectAsState()
    val liquidGlassEnabled by appSettings.liquidGlassEnabled.collectAsState()
    val reduceMotion by appSettings.reduceMotion.collectAsState()
    val reduceTransparency by appSettings.reduceTransparency.collectAsState()
    val highContrast by appSettings.highContrast.collectAsState()
    val savedLocations by viewModel.locations.collectAsState()
    var refreshingOfflinePacks by remember { mutableStateOf(false) }

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val exportSuccess = stringResource(Res.string.export_success)
    val exportFailed = stringResource(Res.string.export_failed)
    val importSuccess = stringResource(Res.string.import_success)
    val importFailed = stringResource(Res.string.import_failed)
    val context = LocalContext.current
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(Res.string.settings_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                    }
                },
                modifier = Modifier.liquidGlass(interactive = false)
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { scaffoldPadding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(scaffoldPadding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            item(key = "units") { SettingsGroup(stringResource(Res.string.unit_settings_title)) {
            Text(stringResource(Res.string.temperature_unit), style = MaterialTheme.typography.bodyLarge)
            UnitRadioRow(
                options = listOf(
                    "celsius" to stringResource(Res.string.unit_celsius),
                    "fahrenheit" to stringResource(Res.string.unit_fahrenheit)
                ),
                selected = tempUnit,
                onSelect = { appSettings.setTempUnit(it) }
            )
            Text(stringResource(Res.string.wind_speed_unit), style = MaterialTheme.typography.bodyLarge)
            UnitRadioRow(
                options = listOf(
                    "kmh" to stringResource(Res.string.unit_kmh),
                    "mph" to stringResource(Res.string.unit_mph),
                    "ms" to stringResource(Res.string.unit_ms)
                ),
                selected = windUnit,
                onSelect = { appSettings.setWindUnit(it) }
            )
            Text(stringResource(Res.string.pressure_unit), style = MaterialTheme.typography.bodyLarge)
            UnitRadioRow(
                options = listOf(
                    "hpa" to stringResource(Res.string.unit_hpa),
                    "mmhg" to stringResource(Res.string.unit_mmhg)
                ),
                selected = pressureUnit,
                onSelect = { appSettings.setPressureUnit(it) }
            )

            } }
            item(key = "appearance") { SettingsGroup(stringResource(Res.string.appearance_accessibility_title)) {
                SettingsToggle(
                    stringResource(Res.string.liquid_glass_title),
                    stringResource(Res.string.liquid_glass_desc),
                    liquidGlassEnabled
                ) { appSettings.setLiquidGlassEnabled(it) }
                SettingsToggle(
                    stringResource(Res.string.reduce_motion_title),
                    stringResource(Res.string.reduce_motion_desc),
                    reduceMotion
                ) { appSettings.setReduceMotion(it) }
                SettingsToggle(
                    stringResource(Res.string.reduce_transparency_title),
                    stringResource(Res.string.reduce_transparency_desc),
                    reduceTransparency
                ) { appSettings.setReduceTransparency(it) }
                SettingsToggle(
                    stringResource(Res.string.high_contrast_title),
                    stringResource(Res.string.high_contrast_desc),
                    highContrast
                ) { appSettings.setHighContrast(it) }
            } }
            item(key = "animations") { SettingsGroup(stringResource(Res.string.weather_animations_title)) {
            Text(stringResource(Res.string.animation_intensity), style = MaterialTheme.typography.bodyLarge)
            UnitRadioRow(
                options = listOf(
                    "full" to stringResource(Res.string.animation_full),
                    "reduced" to stringResource(Res.string.animation_reduced),
                    "off" to stringResource(Res.string.animation_off)
                ),
                selected = weatherAnimations,
                onSelect = { appSettings.setWeatherAnimations(it) }
            )

} }
            item(key = "notifications") { SettingsGroup(stringResource(Res.string.notification_settings_title)) {
            Text(stringResource(Res.string.notification_profile), style = MaterialTheme.typography.bodyLarge)
            UnitRadioRow(
                options = listOf(
                    "normal" to stringResource(Res.string.profile_normal),
                    "outdoor" to stringResource(Res.string.profile_outdoor),
                    "travel" to stringResource(Res.string.profile_travel)
                ),
                selected = notificationProfile,
                onSelect = { appSettings.setNotificationProfile(it) }
            )
            SettingsToggle(
                stringResource(Res.string.quiet_hours),
                stringResource(Res.string.quiet_hours_desc),
                quietHours
            ) { appSettings.setQuietHours(it) }
            SettingsToggle(
                stringResource(Res.string.persistent_notif_title),
                stringResource(Res.string.persistent_notif_subtitle),
                persistentNotif
            ) { enabled ->
                appSettings.setPersistentNotif(enabled)
                val serviceIntent = Intent(context, WeatherForegroundService::class.java)
                if (enabled) {
                    ContextCompat.startForegroundService(context, serviceIntent)
                } else {
                    context.stopService(serviceIntent)
                }
            }
            ThresholdField(
                label = stringResource(Res.string.temp_threshold_label, tempThreshold),
                value = tempThreshold,
                onValueChange = { appSettings.setTempThreshold(it) }
            )
            ThresholdField(
                label = stringResource(Res.string.wind_threshold_label, windThreshold),
                value = windThreshold,
                onValueChange = { appSettings.setWindThreshold(it) }
            )
            Text(stringResource(Res.string.smart_alerts_title), style = MaterialTheme.typography.titleMedium)
            SettingsToggle(stringResource(Res.string.smart_rain_alert_title), stringResource(Res.string.smart_rain_alert_desc), smartRainAlert) { appSettings.setSmartRainAlert(it) }
            SettingsToggle(stringResource(Res.string.smart_wind_alert_title), stringResource(Res.string.smart_wind_alert_desc), smartWindAlert) { appSettings.setSmartWindAlert(it) }
            SettingsToggle(stringResource(Res.string.smart_frost_alert_title), stringResource(Res.string.smart_frost_alert_desc), smartFrostAlert) { appSettings.setSmartFrostAlert(it) }
            SettingsToggle(stringResource(Res.string.smart_uv_alert_title), stringResource(Res.string.smart_uv_alert_desc), smartUvAlert) { appSettings.setSmartUvAlert(it) }

            } }
            item(key = "offline") { SettingsGroup(stringResource(Res.string.data_offline_title)) {
                SettingsToggle(
                    stringResource(Res.string.data_saver_title),
                    stringResource(Res.string.data_saver_desc),
                    dataSaver
                ) { appSettings.setDataSaver(it) }
                SettingsToggle(
                    stringResource(Res.string.offline_packs_title),
                    stringResource(Res.string.offline_packs_desc),
                    offlinePacks
                ) { appSettings.setOfflinePacks(it) }
                SettingsToggle(
                    stringResource(Res.string.offline_wifi_only),
                    stringResource(Res.string.offline_wifi_only_desc),
                    offlinePacksWifiOnly
                ) { appSettings.setOfflinePacksWifiOnly(it) }
                Text(
                    stringResource(Res.string.offline_packs_note),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (offlinePacks) {
                    Text(
                        stringResource(Res.string.offline_saved_locations),
                        style = MaterialTheme.typography.titleMedium
                    )
                    if (savedLocations.isEmpty()) {
                        Text(
                            stringResource(Res.string.offline_no_saved_locations),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        savedLocations.forEach { location ->
                            FreetimeSwitchSetting(
                                title = location.name,
                                description = stringResource(Res.string.offline_city_pack_desc),
                                checked = location.offlinePackEnabled,
                                onCheckedChange = { enabled ->
                                    viewModel.setOfflinePackEnabled(location, enabled)
                                }
                            )
                            val ageMs = (System.currentTimeMillis() - location.lastUpdated).coerceAtLeast(0L)
                            val ageText = when {
                                location.lastUpdated <= 0L -> stringResource(Res.string.offline_never_updated)
                                ageMs < 60L * 60L * 1000L -> stringResource(Res.string.offline_updated_minutes, ageMs / 60000L)
                                ageMs < 24L * 60L * 60L * 1000L -> stringResource(Res.string.offline_updated_hours, ageMs / 3600000L)
                                else -> stringResource(Res.string.offline_updated_days, ageMs / 86400000L)
                            }
                            val freshness = when {
                                location.weatherData == null -> stringResource(Res.string.offline_status_missing)
                                ageMs <= 3L * 60L * 60L * 1000L -> stringResource(Res.string.offline_status_fresh)
                                ageMs <= 12L * 60L * 60L * 1000L -> stringResource(Res.string.offline_status_aging)
                                else -> stringResource(Res.string.offline_status_stale)
                            }
                            val bytes = location.weatherData?.toByteArray(Charsets.UTF_8)?.size ?: 0
                            val sizeText = when {
                                bytes == 0 -> "0 KB"
                                bytes < 1024 * 1024 -> String.format(java.util.Locale.getDefault(), "%.1f KB", bytes / 1024.0)
                                else -> String.format(java.util.Locale.getDefault(), "%.1f MB", bytes / (1024.0 * 1024.0))
                            }
                            Text(
                                stringResource(Res.string.offline_pack_status, freshness, ageText, sizeText),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            if (location.weatherData != null) {
                                Button(
                                    onClick = { viewModel.clearOfflinePackCache(location) },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(stringResource(Res.string.offline_clear_cache))
                                }
                            }
                        }
                    }
                    Button(
                        onClick = {
                            if (!refreshingOfflinePacks) {
                                scope.launch {
                                    refreshingOfflinePacks = true
                                    viewModel.refreshAllLocations(offlinePacksOnly = true)
                                    refreshingOfflinePacks = false
                                    snackbarHostState.showSnackbar(context.getString(Res.string.offline_refresh_complete))
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth().liquidGlassCapsule()
                    ) {
                        Text(
                            stringResource(
                                if (refreshingOfflinePacks) Res.string.offline_refreshing
                                else Res.string.offline_refresh_all
                            )
                        )
                    }
                }
            } }
            item(key = "sources") { SettingsGroup(stringResource(Res.string.data_sources_privacy_title)) {
                Text(stringResource(Res.string.data_source_forecast), style = MaterialTheme.typography.bodyMedium)
                Text(stringResource(Res.string.data_source_map), style = MaterialTheme.typography.bodyMedium)
                Text(stringResource(Res.string.data_source_radar), style = MaterialTheme.typography.bodyMedium)
                Text(
                    stringResource(Res.string.privacy_local_note),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } }
            item(key = "webview") { SettingsGroup(stringResource(Res.string.webview_settings_title)) {
            SettingsToggle(
                stringResource(Res.string.disable_private_view),
                stringResource(Res.string.disable_private_view_subtitle),
                disablePrivateView
            ) { appSettings.setDisablePrivateView(it) }
            SettingsToggle(
                stringResource(Res.string.open_external_browser),
                stringResource(Res.string.open_external_browser_subtitle),
                openExternalBrowser
            ) { appSettings.setOpenExternalBrowser(it) }

            } }
            item(key = "backup") { SettingsGroup(stringResource(Res.string.backup_restore_title)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = {
                        scope.launch {
                            val json = viewModel.buildBackupJson()
                            val ok = json != null && saveTextFile(BACKUP_FILE_NAME, BACKUP_MIME_TYPE, json)
                            snackbarHostState.showSnackbar(if (ok) exportSuccess else exportFailed)
                        }
                    },
                    modifier = Modifier.weight(1f).liquidGlassCapsule()
                ) {
                    Text(stringResource(Res.string.export_locations))
                }
                Button(
                    onClick = {
                        scope.launch {
                            val content = loadTextFile(arrayOf(BACKUP_MIME_TYPE))
                            if (content == null) return@launch
                            val ok = viewModel.importBackupJson(content)
                            snackbarHostState.showSnackbar(if (ok) importSuccess else importFailed)
                        }
                    },
                    modifier = Modifier.weight(1f).liquidGlassCapsule()
                ) {
                    Text(stringResource(Res.string.import_locations))
                }
            }

            } }
            item(key = "open-android") {
            SettingsGroup("Open Android") {
                Text(
                    "Review GeoWeather's Android distribution notice and learn more about keeping Android open.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Button(
                    onClick = onShowDistributionNotice,
                    modifier = Modifier.fillMaxWidth().liquidGlassCapsule()
                ) { Text(stringResource(Res.string.distribution_notice_show)) }
                Button(
                    onClick = { onWebViewClick("https://keepandroidopen.org", "Keep Android Open") },
                    modifier = Modifier.fillMaxWidth().liquidGlassCapsule()
                ) { Text("Keep Android Open") }
            } }
            item(key = "actions") { Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Spacer(Modifier.height(24.dp))
            Button(onClick = onDiagnosticsClick, modifier = Modifier.fillMaxWidth().liquidGlassCapsule()) { Text(stringResource(Res.string.diagnostics_title)) }
            Spacer(Modifier.height(8.dp))
            Button(onClick = onChangeLogClick, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(Res.string.open_change_log))
            }
            Spacer(Modifier.height(8.dp))
            Text(
                text = stringResource(Res.string.feedback_alternative_hint),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            Button(
                onClick = { openUrl("mailto:FreetimeMaker@proton.me?subject=GeoWeather Feedback") },
                modifier = Modifier.fillMaxWidth().liquidGlassCapsule()
            ) {
                Text(stringResource(Res.string.feedback_btn))
            }
            Spacer(Modifier.height(8.dp))
            Button(
                onClick = { onWebViewClick("https://github.com/FreetimeMaker/GeoWeather/issues", "GitHub Issues") },
                modifier = Modifier.fillMaxWidth().liquidGlassCapsule()
            ) {
                Text(stringResource(Res.string.feedback_github_btn))
            }
            } }
        }
    }
}

@Composable
private fun SettingsGroup(title: String, content: @Composable ColumnScope.() -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp).liquidGlass(interactive = false)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            content()
        }
    }
}

@Composable
fun SettingsToggle(label: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    SettingsToggle(label, "", checked, onCheckedChange)
}

@Composable
fun SettingsToggle(label: String, subtitle: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable { onCheckedChange(!checked) }.padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(label, style = MaterialTheme.typography.bodyLarge)
            if (subtitle.isNotBlank()) {
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
fun UnitRadioRow(options: List<Pair<String, String>>, selected: String, onSelect: (String) -> Unit) {
    Column(Modifier.fillMaxWidth()) {
        options.forEach { (value, label) ->
            Row(
                modifier = Modifier.fillMaxWidth().selectable(
                    selected = value == selected,
                    onClick = { onSelect(value) },
                    role = Role.RadioButton
                ).padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                RadioButton(selected = value == selected, onClick = null)
                Text(label, modifier = Modifier.padding(start = 8.dp))
            }
        }
    }
}

@Composable
fun ThresholdField(label: String, value: Int, onValueChange: (Int) -> Unit) {
    var text by remember(value) { mutableStateOf(value.toString()) }
    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Text(label, style = MaterialTheme.typography.labelMedium, modifier = Modifier.padding(start = 8.dp, bottom = 6.dp))
        OutlinedTextField(
            value = text,
            onValueChange = {
                text = it.filter(Char::isDigit)
                text.toIntOrNull()?.let { parsed -> onValueChange(parsed) }
            },
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text(label) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            singleLine = true
        )
    }
}

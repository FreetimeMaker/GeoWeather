package com.freetime.geoweather.ui
import com.freetime.design.FreetimeIconButton
import com.freetime.design.FreetimeText
import com.freetime.design.LocalFreetimePreferencesController
import com.freetime.design.FreetimeScaffold
import com.freetime.design.FreetimeSnackbar
import com.freetime.design.rememberFreetimeMessageHostState
import com.freetime.design.FreetimeDesign
import com.freetime.design.FreetimeChoiceSetting
import com.freetime.design.FreetimeOptionGroup
import com.freetime.design.FreetimeGlassText
import com.freetime.design.FreetimeSwitchSetting
import com.freetime.design.FreetimeSettingsGroup
import com.freetime.design.FreetimeGlassTopBar
import com.freetime.design.FreetimeGlassAction
import com.freetime.design.FreetimeTextField
import com.freetime.design.freetimeGlassCapsule
import com.freetime.design.freetimeGlass

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalContext
import com.freetime.geoweather.data.AppSettings
import com.freetime.geoweather.data.BACKUP_FILE_NAME
import com.freetime.geoweather.data.BACKUP_MIME_TYPE
import com.freetime.geoweather.data.loadTextFile
import com.freetime.geoweather.data.saveTextFile
import com.freetime.geoweather.openUrl
import com.freetime.geoweather.R as Res
import com.freetime.design.freetimeGlass
import com.freetime.design.FreetimeGlassAction
import com.freetime.design.FreetimeGlassTopBar
import com.freetime.design.FreetimeTextField
import com.freetime.design.freetimeGlassCapsule
import kotlinx.coroutines.launch

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

    val snackbarHostState = rememberFreetimeMessageHostState()
    val scope = rememberCoroutineScope()
    val exportSuccess = stringResource(Res.string.export_success)
    val exportFailed = stringResource(Res.string.export_failed)
    val importSuccess = stringResource(Res.string.import_success)
    val importFailed = stringResource(Res.string.import_failed)
    val context = LocalContext.current
    FreetimeScaffold(
        topBar = {
            FreetimeGlassTopBar(
                title = stringResource(Res.string.settings_title),
                navigation = { FreetimeIconButton(icon = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null, onClick = onBack) }
            )
        }
    ) {
        Column(
            modifier = Modifier
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {

            FreetimeSettingsGroup(stringResource(Res.string.unit_settings_title)) {
            FreetimeText(stringResource(Res.string.temperature_unit), style = FreetimeDesign.typography.bodyLarge)
            UnitRadioRow(
                options = listOf(
                    "celsius" to stringResource(Res.string.unit_celsius),
                    "fahrenheit" to stringResource(Res.string.unit_fahrenheit)
                ),
                selected = tempUnit,
                onSelect = { appSettings.setTempUnit(it) }
            )
            FreetimeText(stringResource(Res.string.wind_speed_unit), style = FreetimeDesign.typography.bodyLarge)
            UnitRadioRow(
                options = listOf(
                    "kmh" to stringResource(Res.string.unit_kmh),
                    "mph" to stringResource(Res.string.unit_mph),
                    "ms" to stringResource(Res.string.unit_ms)
                ),
                selected = windUnit,
                onSelect = { appSettings.setWindUnit(it) }
            )
            FreetimeText(stringResource(Res.string.pressure_unit), style = FreetimeDesign.typography.bodyLarge)
            UnitRadioRow(
                options = listOf(
                    "hpa" to stringResource(Res.string.unit_hpa),
                    "mmhg" to stringResource(Res.string.unit_mmhg)
                ),
                selected = pressureUnit,
                onSelect = { appSettings.setPressureUnit(it) }
            )

            }
            LocalFreetimePreferencesController.current?.let { controller ->
                val appearance = controller.state
                FreetimeSettingsGroup("Liquid Glass & accessibility") {
                    FreetimeSwitchSetting(
                        title = "Liquid Glass",
                        description = "Uses the Freetime Core 1.10 live backdrop, luminance, refraction and interactive glass renderer.",
                        checked = appearance.liquidGlassEnabled,
                        onCheckedChange = { enabled ->
                            controller.update { it.copy(liquidGlassEnabled = enabled) }
                        }
                    )
                    FreetimeSwitchSetting(
                        title = "Reduce motion",
                        description = "Reduces decorative motion and Liquid Glass interaction animations.",
                        checked = appearance.reduceMotion,
                        onCheckedChange = { enabled ->
                            controller.update { it.copy(reduceMotion = enabled) }
                        }
                    )
                    FreetimeSwitchSetting(
                        title = "Reduce transparency",
                        description = "Uses more opaque surfaces and disables glass refraction.",
                        checked = appearance.reduceTransparency,
                        onCheckedChange = { enabled ->
                            controller.update { it.copy(reduceTransparency = enabled) }
                        }
                    )
                    FreetimeSwitchSetting(
                        title = "High contrast",
                        description = "Strengthens glass edges, scrims and content separation.",
                        checked = appearance.highContrast,
                        onCheckedChange = { enabled ->
                            controller.update { it.copy(highContrast = enabled) }
                        }
                    )
                }
            }

            FreetimeSettingsGroup(stringResource(Res.string.weather_animations_title)) {
            FreetimeText(stringResource(Res.string.animation_intensity), style = FreetimeDesign.typography.bodyLarge)
            UnitRadioRow(
                options = listOf(
                    "full" to stringResource(Res.string.animation_full),
                    "reduced" to stringResource(Res.string.animation_reduced),
                    "off" to stringResource(Res.string.animation_off)
                ),
                selected = weatherAnimations,
                onSelect = { appSettings.setWeatherAnimations(it) }
            )

}
            FreetimeSettingsGroup(stringResource(Res.string.notification_settings_title)) {
            FreetimeText(stringResource(Res.string.notification_profile), style = FreetimeDesign.typography.bodyLarge)
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
            ) { appSettings.setPersistentNotif(it) }
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
            FreetimeText("Smart alerts", style = FreetimeDesign.typography.titleMedium)
            SettingsToggle("Rain alerts", "Notify when rain is likely in the next few hours.", smartRainAlert) { appSettings.setSmartRainAlert(it) }
            SettingsToggle("Strong wind alerts", "Notify when forecast gusts become strong.", smartWindAlert) { appSettings.setSmartWindAlert(it) }
            SettingsToggle("Frost alerts", "Notify when forecast temperature reaches freezing.", smartFrostAlert) { appSettings.setSmartFrostAlert(it) }
            SettingsToggle("High UV alerts", "Notify when the UV index reaches a high level.", smartUvAlert) { appSettings.setSmartUvAlert(it) }

            }
            FreetimeSettingsGroup(stringResource(Res.string.webview_settings_title)) {
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

            }
            FreetimeSettingsGroup(stringResource(Res.string.backup_restore_title)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FreetimeGlassAction(
                    onClick = {
                        scope.launch {
                            val json = viewModel.buildBackupJson()
                            val ok = json != null && saveTextFile(BACKUP_FILE_NAME, BACKUP_MIME_TYPE, json)
                            snackbarHostState.show(if (ok) exportSuccess else exportFailed)
                        }
                    },
                    modifier = Modifier.weight(1f).freetimeGlass(RoundedCornerShape(20.dp))
                ) {
                    FreetimeText(stringResource(Res.string.export_locations))
                }
                FreetimeGlassAction(
                    onClick = {
                        scope.launch {
                            val content = loadTextFile(arrayOf(BACKUP_MIME_TYPE))
                            if (content == null) return@launch
                            val ok = viewModel.importBackupJson(content)
                            snackbarHostState.show(if (ok) importSuccess else importFailed)
                        }
                    },
                    modifier = Modifier.weight(1f).freetimeGlass(RoundedCornerShape(20.dp))
                ) {
                    FreetimeText(stringResource(Res.string.import_locations))
                }
            }

            }
            Spacer(Modifier.height(FreetimeDesign.spacing.lg))
            FreetimeSettingsGroup("Open Android") {
                FreetimeText(
                    "Review GeoWeather's Android distribution notice and learn more about keeping Android open.",
                    style = FreetimeDesign.typography.bodyMedium,
                    color = FreetimeDesign.palette.contentMuted
                )
                FreetimeGlassAction(
                    onClick = onShowDistributionNotice,
                    modifier = Modifier.fillMaxWidth().freetimeGlass(RoundedCornerShape(22.dp))
                ) { FreetimeText("Show distribution notice") }
                FreetimeGlassAction(
                    onClick = { onWebViewClick("https://keepandroidopen.org", "Keep Android Open") },
                    modifier = Modifier.fillMaxWidth().freetimeGlass(RoundedCornerShape(22.dp))
                ) { FreetimeText("Keep Android Open") }
            }
            Spacer(Modifier.height(FreetimeDesign.spacing.lg))
            FreetimeGlassAction(onClick = onDiagnosticsClick, modifier = Modifier.fillMaxWidth().freetimeGlass(RoundedCornerShape(22.dp))) { FreetimeText(stringResource(Res.string.diagnostics_title)) }
            Spacer(Modifier.height(8.dp))
            FreetimeGlassAction(onClick = onChangeLogClick, modifier = Modifier.fillMaxWidth()) {
                FreetimeText(stringResource(Res.string.open_change_log))
            }
            Spacer(Modifier.height(8.dp))
            FreetimeText(
                text = stringResource(Res.string.feedback_alternative_hint),
                style = FreetimeDesign.typography.bodySmall,
                color = FreetimeDesign.palette.contentMuted,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            FreetimeGlassAction(
                onClick = { openUrl("mailto:FreetimeMaker@proton.me?subject=GeoWeather Feedback") },
                modifier = Modifier.fillMaxWidth().freetimeGlass(RoundedCornerShape(22.dp))
            ) {
                FreetimeText(stringResource(Res.string.feedback_btn))
            }
            Spacer(Modifier.height(8.dp))
            FreetimeGlassAction(
                onClick = { onWebViewClick("https://github.com/FreetimeMaker/GeoWeather/issues", "GitHub Issues") },
                modifier = Modifier.fillMaxWidth().freetimeGlass(RoundedCornerShape(22.dp))
            ) {
                FreetimeText(stringResource(Res.string.feedback_github_btn))
            }
        }
    }
}

@Composable
fun SettingsToggle(label: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    FreetimeSwitchSetting(title = label, checked = checked, onCheckedChange = onCheckedChange)
}

@Composable
fun SettingsToggle(label: String, subtitle: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    FreetimeSwitchSetting(title = label, description = subtitle, checked = checked, onCheckedChange = onCheckedChange)
}

@Composable
fun UnitRadioRow(options: List<Pair<String, String>>, selected: String, onSelect: (String) -> Unit) {
    FreetimeOptionGroup(
        options = options,
        selected = selected,
        onSelect = onSelect,
        modifier = Modifier.fillMaxWidth()
    )
}

@Composable
fun ThresholdField(label: String, value: Int, onValueChange: (Int) -> Unit) {
    var text by remember(value) { mutableStateOf(value.toString()) }
    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        FreetimeText(label, style = FreetimeDesign.typography.labelMedium, modifier = Modifier.padding(start = 8.dp, bottom = 6.dp))
        FreetimeTextField(
            value = text,
            onValueChange = {
                text = it.filter(Char::isDigit)
                text.toIntOrNull()?.let { parsed -> onValueChange(parsed) }
            },
            modifier = Modifier.fillMaxWidth(),
            placeholder = label
        )
    }
}

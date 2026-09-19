package com.freetime.geoweather.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
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
import coil.compose.AsyncImage
import com.freetime.geoweather.AppwriteData
import com.freetime.geoweather.GeoWeatherAccount
import com.freetime.geoweather.AppwriteSync
import com.freetime.geoweather.data.AppSettings
import com.freetime.geoweather.data.BACKUP_FILE_NAME
import com.freetime.geoweather.data.BACKUP_MIME_TYPE
import com.freetime.geoweather.data.loadTextFile
import com.freetime.geoweather.data.saveTextFile
import com.freetime.geoweather.openUrl
import com.freetime.geoweather.R as Res
import com.freetime.geoweather.ui.glass.geoWeatherGlass
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: WeatherViewModel,
    appSettings: AppSettings,
    onBack: () -> Unit,
    onChangeLogClick: () -> Unit,
    onWebViewClick: (String, String) -> Unit
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

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val exportSuccess = stringResource(Res.string.export_success)
    val exportFailed = stringResource(Res.string.export_failed)
    val importSuccess = stringResource(Res.string.import_success)
    val importFailed = stringResource(Res.string.import_failed)
    val context = LocalContext.current
    var account by remember { mutableStateOf<GeoWeatherAccount?>(null) }
    var code by remember { mutableStateOf("") }
    var codeBusy by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        account = runCatching { AppwriteData.account(context) }.getOrNull()
    }

    Scaffold(
        containerColor = Color.Transparent,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                    .geoWeatherGlass(RoundedCornerShape(28.dp), interactive = false),
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
                title = { Text(stringResource(Res.string.settings_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(Res.string.back_nav_desc))
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {

            SettingsSection(stringResource(Res.string.account_title))
            account?.let { profile ->
                Row(
                    Modifier.fillMaxWidth().geoWeatherGlass(RoundedCornerShape(24.dp), interactive = false).padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    if (!profile.avatarUrl.isNullOrBlank()) {
                        AsyncImage(model = profile.avatarUrl, contentDescription = null, modifier = Modifier.size(56.dp))
                    }
                    Column(Modifier.weight(1f)) {
                        Text(profile.name, style = MaterialTheme.typography.titleMedium)
                        Text(profile.email, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(stringResource(Res.string.subscription_current, profile.subscription.uppercase()), style = MaterialTheme.typography.labelLarge)
                    }
                }
            }

            SettingsSection(stringResource(Res.string.subscription_title))
            OutlinedTextField(
                value = code,
                onValueChange = { code = it },
                label = { Text(stringResource(Res.string.redeem_code_hint)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().geoWeatherGlass(RoundedCornerShape(22.dp)),
                colors = OutlinedTextFieldDefaults.colors(focusedContainerColor = Color.Transparent, unfocusedContainerColor = Color.Transparent)
            )
            Button(
                enabled = code.isNotBlank() && !codeBusy,
                onClick = {
                    scope.launch {
                        codeBusy = true
                        val subscription = runCatching { AppwriteData.redeemCode(context, code) }.getOrNull()
                        if (subscription != null) {
                            account = runCatching { AppwriteData.account(context) }.getOrNull()
                            code = ""
                            snackbarHostState.showSnackbar(context.getString(Res.string.redeem_code_success, subscription.uppercase()))
                        } else {
                            snackbarHostState.showSnackbar(context.getString(Res.string.redeem_code_invalid))
                        }
                        codeBusy = false
                    }
                },
                modifier = Modifier.fillMaxWidth().geoWeatherGlass(RoundedCornerShape(22.dp)),
                colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent)
            ) {
                if (codeBusy) CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                else Text(stringResource(Res.string.redeem_code))
            }

            SettingsSection(stringResource(Res.string.sync_title))
            Text(
                stringResource(Res.string.sync_description),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Button(
                    onClick = {
                        scope.launch {
                            val ok = runCatching {
                                AppwriteSync.push(context, viewModel.repositoryForSync(), appSettings)
                            }.isSuccess
                            snackbarHostState.showSnackbar(context.getString(if (ok) Res.string.sync_success else Res.string.sync_failed))
                        }
                    },
                    modifier = Modifier.weight(1f).geoWeatherGlass(RoundedCornerShape(22.dp)),
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent)
                ) { Text(stringResource(Res.string.sync_now)) }
                OutlinedButton(
                    onClick = {
                        scope.launch {
                            val ok = runCatching {
                                AppwriteSync.pull(context, viewModel.repositoryForSync(), appSettings)
                            }.getOrDefault(false)
                            snackbarHostState.showSnackbar(context.getString(if (ok) Res.string.sync_success else Res.string.sync_failed))
                        }
                    },
                    modifier = Modifier.weight(1f).geoWeatherGlass(RoundedCornerShape(22.dp)),
                    colors = ButtonDefaults.outlinedButtonColors(containerColor = Color.Transparent),
                    border = null
                ) { Text(stringResource(Res.string.sync_restore)) }
            }

            SettingsSection(stringResource(Res.string.unit_settings_title))
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

            SettingsSection("Weather animations")
            Text("Animation intensity", style = MaterialTheme.typography.bodyLarge)
            UnitRadioRow(
                options = listOf(
                    "full" to "Full",
                    "reduced" to "Reduced",
                    "off" to "Off"
                ),
                selected = weatherAnimations,
                onSelect = { appSettings.setWeatherAnimations(it) }
            )

                        SettingsSection(stringResource(Res.string.notification_settings_title))
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

            SettingsSection(stringResource(Res.string.webview_settings_title))
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

            SettingsSection(stringResource(Res.string.backup_restore_title))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = {
                        scope.launch {
                            val json = viewModel.buildBackupJson()
                            val ok = json != null && saveTextFile(BACKUP_FILE_NAME, BACKUP_MIME_TYPE, json)
                            snackbarHostState.showSnackbar(if (ok) exportSuccess else exportFailed)
                        }
                    },
                    modifier = Modifier.weight(1f).geoWeatherGlass(RoundedCornerShape(20.dp)),
                    colors = ButtonDefaults.outlinedButtonColors(containerColor = Color.Transparent),
                    border = null
                ) {
                    Text(stringResource(Res.string.export_locations))
                }
                OutlinedButton(
                    onClick = {
                        scope.launch {
                            val content = loadTextFile(arrayOf(BACKUP_MIME_TYPE))
                            if (content == null) return@launch
                            val ok = viewModel.importBackupJson(content)
                            snackbarHostState.showSnackbar(if (ok) importSuccess else importFailed)
                        }
                    },
                    modifier = Modifier.weight(1f).geoWeatherGlass(RoundedCornerShape(20.dp)),
                    colors = ButtonDefaults.outlinedButtonColors(containerColor = Color.Transparent),
                    border = null
                ) {
                    Text(stringResource(Res.string.import_locations))
                }
            }

            Spacer(Modifier.height(16.dp))
            Button(onClick = onChangeLogClick, modifier = Modifier.fillMaxWidth().geoWeatherGlass(RoundedCornerShape(22.dp)), colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent), elevation = ButtonDefaults.buttonElevation(0.dp, 0.dp, 0.dp, 0.dp, 0.dp)) {
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
            OutlinedButton(
                onClick = { openUrl("mailto:FreetimeMaker@proton.me?subject=GeoWeather Feedback") },
                modifier = Modifier.fillMaxWidth().geoWeatherGlass(RoundedCornerShape(22.dp)),
                colors = ButtonDefaults.outlinedButtonColors(containerColor = Color.Transparent),
                border = null
            ) {
                Text(stringResource(Res.string.feedback_btn))
            }
            Spacer(Modifier.height(8.dp))
            OutlinedButton(
                onClick = { onWebViewClick("https://github.com/FreetimeMaker/GeoWeather/issues", "GitHub Issues") },
                modifier = Modifier.fillMaxWidth().geoWeatherGlass(RoundedCornerShape(22.dp)),
                colors = ButtonDefaults.outlinedButtonColors(containerColor = Color.Transparent),
                border = null
            ) {
                Text(stringResource(Res.string.feedback_github_btn))
            }
        }
    }
}

@Composable
fun SettingsSection(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(top = 16.dp, bottom = 4.dp)
    )
}

@Composable
fun SettingsToggle(label: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label)
        Spacer(Modifier.weight(1f))
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
fun SettingsToggle(label: String, subtitle: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f).geoWeatherGlass(RoundedCornerShape(20.dp))) {
            Text(label)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
fun UnitRadioRow(options: List<Pair<String, String>>, selected: String, onSelect: (String) -> Unit) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Start) {
        options.forEach { (value, label) ->
            Row(
                modifier = Modifier
                    .selectable(
                        selected = selected == value,
                        onClick = { onSelect(value) },
                        role = Role.RadioButton
                    )
                    .padding(horizontal = 8.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                RadioButton(selected = selected == value, onClick = null)
                Spacer(Modifier.width(4.dp))
                Text(label, style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}

@Composable
fun ThresholdField(label: String, value: Int, onValueChange: (Int) -> Unit) {
    var text by remember(value) { mutableStateOf(value.toString()) }
    OutlinedTextField(
        value = text,
        onValueChange = {
            text = it
            it.toIntOrNull()?.let { parsed -> if (parsed >= 0) onValueChange(parsed) }
        },
        label = { Text(label) },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        singleLine = true,
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp).geoWeatherGlass(RoundedCornerShape(22.dp)),
        colors = OutlinedTextFieldDefaults.colors(focusedContainerColor = Color.Transparent, unfocusedContainerColor = Color.Transparent)
    )
}

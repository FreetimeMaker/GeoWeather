package com.freetime.geoweather.ui

import android.content.Intent
import android.os.Build
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.freetime.core.FreetimeCore
import com.freetime.design.liquidGlass
import com.freetime.geoweather.*
import com.freetime.geoweather.R as Res
import com.freetime.geoweather.data.DependencyManager

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DiagnosticsScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    var locations by remember { mutableIntStateOf(0) }
    LaunchedEffect(Unit) {
        locations = DependencyManager.getRepository().getAllLocationsSync().size
    }
    val network = if (isNetworkAvailable()) "online" else "offline"
    val packageInfo = remember { runCatching { context.packageManager.getPackageInfo(context.packageName, 0) }.getOrNull() }
    val versionName = packageInfo?.versionName ?: "unknown"
    val versionCode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) packageInfo?.longVersionCode ?: 0L else @Suppress("DEPRECATION") (packageInfo?.versionCode?.toLong() ?: 0L)
    val appName = remember { FreetimeCore.appName(context) }
    val notificationPermission = if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
        androidx.core.content.ContextCompat.checkSelfPermission(context, android.Manifest.permission.POST_NOTIFICATIONS) == android.content.pm.PackageManager.PERMISSION_GRANTED
    ) "granted" else "not granted"
    val locationPermission = if (
        androidx.core.content.ContextCompat.checkSelfPermission(context, android.Manifest.permission.ACCESS_COARSE_LOCATION) == android.content.pm.PackageManager.PERMISSION_GRANTED ||
        androidx.core.content.ContextCompat.checkSelfPermission(context, android.Manifest.permission.ACCESS_FINE_LOCATION) == android.content.pm.PackageManager.PERMISSION_GRANTED
    ) "granted" else "not granted"
    val cacheMb = remember { context.cacheDir.walkTopDown().filter { it.isFile }.sumOf { it.length() } / (1024.0 * 1024.0) }
    val report = "$appName $versionName ($versionCode)\n" +
        "Android ${Build.VERSION.RELEASE} / API ${Build.VERSION.SDK_INT}\n" +
        "Device: ${Build.MANUFACTURER} ${Build.MODEL}\n" +
        "Network: $network\nSaved locations: $locations\n" +
        "Location permission: $locationPermission\nNotifications: $notificationPermission\n" +
        "Cache: ${String.format(java.util.Locale.getDefault(), "%.1f", cacheMb)} MB\n"

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(Res.string.diagnostics_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                    }
                },
                modifier = Modifier.liquidGlass(interactive = false)
            )
        }
    ) { padding ->
        Column(
            Modifier.padding(padding).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Card(
                modifier = Modifier.fillMaxWidth().liquidGlass(interactive = false)
            ) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(stringResource(Res.string.diagnostics_title), style = MaterialTheme.typography.titleMedium)
                    Text(report, style = MaterialTheme.typography.bodyMedium)
                }
            }
            Button(
                onClick = {
                    val intent = Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(Intent.EXTRA_SUBJECT, "GeoWeather diagnostics")
                        putExtra(Intent.EXTRA_TEXT, report)
                    }
                    context.startActivity(Intent.createChooser(intent, context.getString(Res.string.export_diagnostics)))
                },
                modifier = Modifier.fillMaxWidth().liquidGlass()
            ) {
                Text(stringResource(Res.string.export_diagnostics))
            }
        }
    }
}

package com.freetime.geoweather.ui

import android.content.Intent
import android.os.Build
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.freetime.geoweather.*
import com.freetime.geoweather.data.DependencyManager
import com.freetime.geoweather.R as Res
import com.freetime.geoweather.ui.glass.geoWeatherGlass
import me.free_time.core.FreetimeCore

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
    val report = "$appName $versionName ($versionCode)\n" +
        "Android ${Build.VERSION.RELEASE} / API ${Build.VERSION.SDK_INT}\n" +
        "Device: ${Build.MANUFACTURER} ${Build.MODEL}\n" +
        "Network: $network\nSaved locations: $locations"
    Scaffold(containerColor = Color.Transparent, topBar = {
        TopAppBar(
            modifier = Modifier.padding(14.dp).geoWeatherGlass(RoundedCornerShape(28.dp), interactive = false),
            colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent, titleContentColor = MaterialTheme.colorScheme.onSurface, navigationIconContentColor = MaterialTheme.colorScheme.onSurface, actionIconContentColor = MaterialTheme.colorScheme.onSurface),
            title = { Text(stringResource(Res.string.diagnostics_title)) },
            navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(Res.string.back_nav_desc)) } }
        )
    }) { padding ->
        Column(Modifier.padding(padding).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Card(Modifier.fillMaxWidth().geoWeatherGlass(RoundedCornerShape(24.dp), interactive = false), colors = CardDefaults.cardColors(containerColor = Color.Transparent, contentColor = MaterialTheme.colorScheme.onSurface)) {
                Text(report, Modifier.padding(16.dp))
            }
            Button(onClick = {
                val intent = Intent(Intent.ACTION_SEND).apply { type = "text/plain"; putExtra(Intent.EXTRA_SUBJECT, "GeoWeather diagnostics"); putExtra(Intent.EXTRA_TEXT, report) }
                context.startActivity(Intent.createChooser(intent, context.getString(Res.string.export_diagnostics)))
            }, modifier = Modifier.fillMaxWidth().geoWeatherGlass(RoundedCornerShape(22.dp)), colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent, contentColor = MaterialTheme.colorScheme.onSurface)) {
                Text(stringResource(Res.string.export_diagnostics))
            }
        }
    }
}

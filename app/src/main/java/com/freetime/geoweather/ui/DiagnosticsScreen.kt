package com.freetime.geoweather.ui
import com.freetime.design.FreetimeIconButton
import com.freetime.design.FreetimeInfoCard
import com.freetime.design.FreetimeText
import com.freetime.design.freetimeGlass
import com.freetime.design.FreetimeGlassTopBar
import com.freetime.design.FreetimeGlassAction
import com.freetime.design.FreetimeGlassPanel

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
import com.freetime.design.freetimeGlass
import com.freetime.design.FreetimeGlassTopBar
import com.freetime.design.FreetimeGlassAction
import com.freetime.design.FreetimeGlassPanel
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
        "Cache: ${String.format(java.util.Locale.US, "%.1f", cacheMb)} MB\n" +
        "Accounts: none\nWeather cache: local Room database\nShare cards: temporary app cache"
    Scaffold(containerColor = Color.Transparent, topBar = {
        FreetimeGlassTopBar(
            title = stringResource(Res.string.diagnostics_title),
            navigation = { FreetimeIconButton(icon = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null, onClick = onBack) }
        )
    }) { padding ->
        Column(Modifier.padding(padding).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            FreetimeInfoCard(
                title = stringResource(Res.string.diagnostics_title),
                modifier = Modifier.fillMaxWidth()
            ) {
                FreetimeText(report)
            }
            FreetimeGlassAction(onClick = {
                val intent = Intent(Intent.ACTION_SEND).apply { type = "text/plain"; putExtra(Intent.EXTRA_SUBJECT, "GeoWeather diagnostics"); putExtra(Intent.EXTRA_TEXT, report) }
                context.startActivity(Intent.createChooser(intent, context.getString(Res.string.export_diagnostics)))
            }, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(Res.string.export_diagnostics))
            }
        }
    }
}

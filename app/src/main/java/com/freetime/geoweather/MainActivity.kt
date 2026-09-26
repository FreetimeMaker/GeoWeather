package com.freetime.geoweather

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.content.Intent
import android.content.pm.ShortcutInfo
import android.content.pm.ShortcutManager
import android.graphics.drawable.Icon
import androidx.activity.ComponentActivity
import androidx.activity.addCallback
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import com.freetime.geoweather.data.DependencyManager
import com.freetime.geoweather.data.onCreateDocumentResult
import com.freetime.geoweather.data.onOpenDocumentResult
import com.freetime.geoweather.data.registerFilePickers

class MainActivity : ComponentActivity() {
    private val requestPermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { _ -> }
    private val requestLocationPermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { _ -> }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        checkNotificationPermission()
        checkLocationPermission()
        installShortcuts()

        val createDocument = registerForActivityResult(ActivityResultContracts.CreateDocument("application/json"), ::onCreateDocumentResult)
        val openDocument = registerForActivityResult(ActivityResultContracts.OpenDocument(), ::onOpenDocumentResult)
        registerFilePickers(createDocument, openDocument)

        onBackPressedDispatcher.addCallback(this) {
            if (systemBackHandler?.invoke() != true) finish()
        }

        setContent {
            WeatherApp(
                database = DependencyManager.getDatabase(),
                appSettings = DependencyManager.getAppSettings()
            )
        }
    }

    private fun installShortcuts() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N_MR1) return
        val manager = getSystemService(ShortcutManager::class.java)
        fun shortcut(id: String, label: String, action: String, icon: Int) =
            ShortcutInfo.Builder(this, id)
                .setShortLabel(label)
                .setIcon(Icon.createWithResource(this, icon))
                .setIntent(Intent(this, MainActivity::class.java).setAction(action))
                .build()
        manager.dynamicShortcuts = listOf(
            shortcut("current", getString(R.string.current_location), "com.freetime.geoweather.CURRENT", R.drawable.ic_notification),
            shortcut("search", getString(R.string.search_title), "com.freetime.geoweather.SEARCH", R.drawable.ic_notification),
            shortcut("settings", getString(R.string.settings_title), "com.freetime.geoweather.SETTINGS", R.drawable.ic_notification)
        )
    }

    private fun checkNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    private fun checkLocationPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            requestLocationPermissionLauncher.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION))
        }
    }
}

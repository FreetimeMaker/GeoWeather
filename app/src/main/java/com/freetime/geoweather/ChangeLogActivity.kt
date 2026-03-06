package com.freetime.geoweather

import android.os.Bundle
import android.view.View
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalContext
import com.freetime.geoweather.ui.theme.GeoWeatherTheme

class ChangeLogActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        hideSystemUI()
        setContent {
            GeoWeatherTheme {
                ChangeLogScreen(onBack = { finish() })
            }
        }
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) {
            hideSystemUI()
        }
    }

    private fun hideSystemUI() {
        window.decorView.systemUiVisibility = (
                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                        or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        or View.SYSTEM_UI_FLAG_FULLSCREEN
                )
    }
}

data class ReleaseNotes(
    val version: String,
    val details: List<String>
)

@Composable
fun ReleaseCard(version: String, modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Card(
        modifier = modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = version,
                style = MaterialTheme.typography.headlineMedium
            )
            Spacer(modifier = Modifier.height(8.dp))
            content()
        }
    }
}

@Composable
fun ChangeLogScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    // `versionName` can be nullable; compute a non-nullable String explicitly
    val version: String = try {
        val vn = context.packageManager.getPackageInfo(context.packageName, 0).versionName
        vn ?: "?"
    } catch (e: Exception) {
        "?"
    }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // navigation header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Start,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Button(onClick = onBack) {
                Text(stringResource(R.string.back_btn))
            }
        }

        Text(
            text = stringResource(R.string.whats_new_title),
            style = MaterialTheme.typography.headlineMedium
        )

        // ---- inline-defined cards --------------------------------
        ReleaseCard(version = version) {
                Text("🛠️ ${stringResource(R.string.added_label)}")
                Text(stringResource(R.string.changelog_moon_data))
                Text("🛠️ ${stringResource(R.string.fixed_label)}")
                Text(stringResource(R.string.changelog_ui_improvements))
            }
        Spacer(modifier = Modifier.weight(1f))
    }
}
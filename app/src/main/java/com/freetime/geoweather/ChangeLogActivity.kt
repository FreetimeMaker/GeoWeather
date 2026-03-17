package com.freetime.geoweather

import android.os.Bundle
import android.view.View
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
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
fun ReleaseCard(
    notes: ReleaseNotes,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally // <-- Zentriert
        ) {
            Text(
                text = notes.version,
                style = MaterialTheme.typography.headlineMedium
            )
            Spacer(modifier = Modifier.height(8.dp))

            notes.details.forEach { line ->
                Text(
                    text = line,
                    style = MaterialTheme.typography.bodyLarge
                )
            }
        }
    }
}

@Composable
fun ChangeLogScreen(onBack: () -> Unit) {
    val context = LocalContext.current

    // 🔥 Hier fügst du einfach neue Versionen hinzu
    val releases = listOf(
        ReleaseNotes(
            version = "v1.3.0",
            details = listOf(
                "🛠️ ${stringResource(R.string.fixed_label)}",
                stringResource(R.string.FixFMSDK),
                "🛠️ ${stringResource(R.string.removed_label)}",
                stringResource(R.string.changelog_remove_coin)
            )
        ),
        ReleaseNotes(
            version = "v1.2.9",
            details = listOf(
                "🛠️ ${stringResource(R.string.added_label)}",
                stringResource(R.string.changelog_add_translate),
                "🛠️ ${stringResource(R.string.fixed_label)}",
                stringResource(R.string.changelog_update_deps),
                stringResource(R.string.changelog_fix_moon)
            )
        )
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()) // <-- ScrollView
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
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

        // 🔥 Automatisch mehrere ReleaseCards
        releases.forEach { release ->
            ReleaseCard(notes = release)
        }

        Spacer(modifier = Modifier.weight(1f))
    }
}
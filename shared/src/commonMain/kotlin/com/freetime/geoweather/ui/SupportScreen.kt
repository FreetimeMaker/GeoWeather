package com.freetime.geoweather.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.freetime.geoweather.Platform
import geoweather.shared.generated.resources.Res
import geoweather.shared.generated.resources.back_nav_desc
import org.jetbrains.compose.resources.stringResource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SupportScreen(platform: Platform, onBack: () -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Support GeoWeather") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(Res.string.back_nav_desc)
                        )
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Keep GeoWeather free", style = MaterialTheme.typography.titleLarge)
                    Text(
                        "GeoWeather is open source, ad-free, and does not require a guest account.",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
            }
            Button(
                onClick = { platform.openUrl("https://github.com/sponsors/FreetimeMaker") },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("GitHub Sponsors")
            }
            Button(
                onClick = { platform.openUrl("https://nowpayments.io/donation/GeoWeather") },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Donate with crypto")
            }
            Button(
                onClick = { platform.openUrl("https://github.com/FreetimeMaker/GeoWeather") },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Open project")
            }
        }
    }
}

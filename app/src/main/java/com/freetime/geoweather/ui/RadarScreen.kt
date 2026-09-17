package com.freetime.geoweather.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.freetime.geoweather.shared.R as Res

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RadarScreen(
    lat: Double,
    lon: Double,
    onBack: () -> Unit
) {
    val url = "https://www.windy.com/?$lat,$lon,8"
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(Res.string.radar_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(Res.string.back_nav_desc))
                    }
                }
            )
        }
    ) { padding ->
        PlatformWebView(
            url = url,
            modifier = Modifier.fillMaxSize().padding(padding)
        )
    }
}

package com.freetime.geoweather.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.freetime.geoweather.R as Res
import com.freetime.geoweather.ui.glass.geoWeatherGlass

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RadarScreen(
    lat: Double,
    lon: Double,
    onBack: () -> Unit
) {
    val url = "https://www.windy.com/?$lat,$lon,8"
    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                    .geoWeatherGlass(RoundedCornerShape(28.dp), interactive = false),
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent, titleContentColor = MaterialTheme.colorScheme.onSurface, navigationIconContentColor = MaterialTheme.colorScheme.onSurface, actionIconContentColor = MaterialTheme.colorScheme.onSurface),
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

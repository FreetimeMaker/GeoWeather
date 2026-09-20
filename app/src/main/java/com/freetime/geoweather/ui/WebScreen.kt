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
import com.freetime.geoweather.ui.glass.GeoWeatherGlassTopBar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WebScreen(
    url: String,
    title: String,
    onBack: () -> Unit
) {
    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            GeoWeatherGlassTopBar(
                title = title,
                onBack = onBack
            )
        }
    ) { padding ->
        PlatformWebView(
            url = url,
            modifier = Modifier.fillMaxSize().padding(padding)
        )
    }
}

package com.freetime.geoweather.ui
import me.free_time.design.FreetimeIconButton
import me.free_time.design.freetimeGlass
import me.free_time.design.FreetimeGlassTopBar

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
import me.free_time.design.freetimeGlass
import me.free_time.design.FreetimeGlassTopBar

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
            FreetimeGlassTopBar(
                title = stringResource(Res.string.radar_title),
                navigation = { FreetimeIconButton(icon = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null, onClick = onBack) }
            )
        }
    ) { padding ->
        PlatformWebView(
            url = url,
            modifier = Modifier.fillMaxSize().padding(padding)
        )
    }
}

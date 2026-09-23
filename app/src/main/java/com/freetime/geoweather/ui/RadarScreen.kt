package com.freetime.geoweather.ui
import com.freetime.design.FreetimeIconButton
import com.freetime.design.FreetimeScaffold
import com.freetime.design.freetimeGlass
import com.freetime.design.FreetimeGlassTopBar

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Pause
import androidx.compose.runtime.*
import androidx.compose.foundation.clickable
import androidx.compose.ui.Alignment
import com.freetime.design.FreetimeText
import com.freetime.design.FreetimeCard
import com.freetime.design.FreetimeDesign
import kotlinx.coroutines.delay
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.freetime.geoweather.R as Res
import com.freetime.design.freetimeGlass
import com.freetime.design.FreetimeGlassTopBar

@Composable
fun RadarScreen(
    lat: Double,
    lon: Double,
    onBack: () -> Unit
) {
    var playing by remember { mutableStateOf(false) }
    var frame by remember { mutableIntStateOf(0) }
    val frames = listOf("-120", "-90", "-60", "-30", "0")
    LaunchedEffect(playing) {
        while (playing) {
            delay(1200)
            frame = (frame + 1) % frames.size
        }
    }
    val url = "https://www.windy.com/?$lat,$lon,8"
    FreetimeScaffold(
        topBar = {
            FreetimeGlassTopBar(
                title = stringResource(Res.string.radar_title),
                navigation = { FreetimeIconButton(icon = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null, onClick = onBack) }
            )
        }
    ) {
        Box(Modifier.fillMaxSize()) {
            PlatformWebView(
                url = url,
                modifier = Modifier.fillMaxSize()
            )
            FreetimeCard(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
                    .padding(16.dp)
                    .fillMaxWidth()
            ) {
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FreetimeIconButton(
                        icon = if (playing) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = if (playing) "Pause" else "Play",
                        onClick = { playing = !playing }
                    )
                    frames.forEachIndexed { index, minutes ->
                        Column(
                            modifier = Modifier.weight(1f).clickable { frame = index },
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            FreetimeText(if (minutes == "0") "Now" else minutes + "m", style = FreetimeDesign.typography.labelSmall)
                            if (index == frame) FreetimeText("●", style = FreetimeDesign.typography.labelSmall)
                        }
                    }
                }
            }
        }
    }
}

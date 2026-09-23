package com.freetime.geoweather.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.freetime.design.FreetimeCard
import com.freetime.design.FreetimeDesign
import com.freetime.design.FreetimeGlassTopBar
import com.freetime.design.FreetimeIconButton
import com.freetime.design.FreetimeScaffold
import com.freetime.design.FreetimeText
import com.freetime.geoweather.R as Res
import kotlinx.coroutines.delay

@Composable
fun WeatherMapPreview(lat: Double, lon: Double, modifier: Modifier = Modifier) {
    val html = remember(lat, lon) { rainViewerHtml(lat, lon, 11) }
    PlatformWebView(
        url = "about:blank",
        html = html,
        modifier = modifier
    )
}

@Composable
fun RadarScreen(lat: Double, lon: Double, onBack: () -> Unit) {
    var playing by remember { mutableStateOf(true) }
    var frameIndex by remember { mutableIntStateOf(0) }

    LaunchedEffect(playing) {
        while (playing) {
            delay(1200)
            frameIndex = (frameIndex + 1) % 12
        }
    }

    val html = remember(lat, lon, frameIndex) { rainViewerHtml(lat, lon, frameIndex) }

    FreetimeScaffold(
        topBar = {
            FreetimeGlassTopBar(
                title = stringResource(Res.string.radar_title),
                navigation = { FreetimeIconButton(Icons.AutoMirrored.Filled.ArrowBack, null, onClick = onBack) }
            )
        }
    ) {
        Box(Modifier.fillMaxSize()) {
            PlatformWebView("about:blank", Modifier.fillMaxSize(), html = html)
            FreetimeCard(
                modifier = Modifier.align(Alignment.BottomCenter).navigationBarsPadding().padding(16.dp).fillMaxWidth()
            ) {
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    FreetimeIconButton(
                        icon = if (playing) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = if (playing) "Pause radar" else "Play radar",
                        onClick = { playing = !playing }
                    )
                    Column(Modifier.weight(1f)) {
                        FreetimeText(
                            if (frameIndex == 11) "Latest radar" else ((11 - frameIndex) * 10).toString() + " min ago",
                            style = FreetimeDesign.typography.labelLarge
                        )
                        FreetimeText(
                            "Past 2 hours · 10 minute frames · Weather data by RainViewer",
                            style = FreetimeDesign.typography.labelSmall,
                            color = FreetimeDesign.palette.contentMuted
                        )
                    }
                }
            }
        }
    }
}

private fun rainViewerHtml(lat: Double, lon: Double, frameIndex: Int): String {
    val safeIndex = frameIndex.coerceIn(0, 11)
    return """
<!doctype html>
<html><head>
<meta name="viewport" content="width=device-width,initial-scale=1,maximum-scale=1,user-scalable=no">
<link rel="stylesheet" href="https://unpkg.com/leaflet@1.9.4/dist/leaflet.css">
<style>html,body,#map{height:100%;margin:0;background:#111}.leaflet-control-attribution{font-size:10px}</style>
</head><body><div id="map"></div>
<script src="https://unpkg.com/leaflet@1.9.4/dist/leaflet.js"></script>
<script>
const map=L.map('map').setView([LAT,LON],7);
L.tileLayer('https://tile.openstreetmap.org/{z}/{x}/{y}.png',{maxZoom:19,attribution:'© OpenStreetMap contributors'}).addTo(map);
L.marker([LAT,LON]).addTo(map);
fetch('https://api.rainviewer.com/public/weather-maps.json').then(r=>r.json()).then(data=>{
 const frames=(data.radar&&data.radar.past)||[];
 if(!frames.length)return;
 const frame=frames[Math.min(INDEX,frames.length-1)];
 L.tileLayer(data.host+frame.path+'/256/{z}/{x}/{y}/2/1_1.png',{
   tileSize:256,opacity:.72,maxZoom:7,attribution:'Weather data © RainViewer'
 }).addTo(map);
});
</script></body></html>
""".trimIndent()
        .replace("LAT", lat.toString())
        .replace("LON", lon.toString())
        .replace("INDEX", safeIndex.toString())
}

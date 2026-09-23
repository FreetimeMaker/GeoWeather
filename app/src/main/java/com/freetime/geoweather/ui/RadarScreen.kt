package com.freetime.geoweather.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.freetime.design.FreetimeCard
import com.freetime.design.FreetimeDesign
import com.freetime.design.FreetimeGlassTopBar
import com.freetime.design.FreetimeIconButton
import com.freetime.design.FreetimeScaffold
import com.freetime.design.FreetimeText
import com.freetime.geoweather.R as Res
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.MapTileProviderBasic
import org.osmdroid.tileprovider.tilesource.OnlineTileSourceBase
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.TilesOverlay
import java.net.URL

private data class RadarFrames(val host: String, val paths: List<String>)

@Composable
fun WeatherMapPreview(lat: Double, lon: Double, modifier: Modifier = Modifier) {
    NativeWeatherMap(lat = lat, lon = lon, frameIndex = 11, modifier = modifier)
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

    FreetimeScaffold(
        topBar = {
            FreetimeGlassTopBar(
                title = stringResource(Res.string.radar_title),
                navigation = {
                    FreetimeIconButton(
                        icon = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = null,
                        onClick = onBack
                    )
                }
            )
        }
    ) {
        Box(Modifier.fillMaxSize()) {
            NativeWeatherMap(lat, lon, frameIndex, Modifier.fillMaxSize())
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

@Composable
private fun NativeWeatherMap(
    lat: Double,
    lon: Double,
    frameIndex: Int,
    modifier: Modifier
) {
    val context = LocalContext.current
    var frames by remember { mutableStateOf<RadarFrames?>(null) }

    LaunchedEffect(Unit) {
        frames = withContext(Dispatchers.IO) {
            runCatching {
                val raw = URL("https://api.rainviewer.com/public/weather-maps.json").readText()
                val root = Json.parseToJsonElement(raw).jsonObject
                val host = root["host"]?.jsonPrimitive?.content ?: return@runCatching null
                val paths = root["radar"]?.jsonObject?.get("past")?.jsonArray
                    ?.mapNotNull { it.jsonObject["path"]?.jsonPrimitive?.content }
                    .orEmpty()
                RadarFrames(host, paths)
            }.getOrNull()
        }
    }

    AndroidView(
        modifier = modifier,
        factory = {
            Configuration.getInstance().userAgentValue = context.packageName
            MapView(context).apply {
                setTileSource(TileSourceFactory.MAPNIK)
                setMultiTouchControls(true)
                controller.setZoom(7.0)
                controller.setCenter(GeoPoint(lat, lon))
                overlays.add(Marker(this).apply {
                    position = GeoPoint(lat, lon)
                    setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                })
            }
        },
        update = { map ->
            map.controller.setCenter(GeoPoint(lat, lon))
            map.overlays.removeAll { it is TilesOverlay }
            val data = frames
            if (data != null && data.paths.isNotEmpty()) {
                val path = data.paths[frameIndex.coerceIn(0, data.paths.lastIndex)]
                val source = object : OnlineTileSourceBase(
                    "RainViewer",
                    0, 7, 256, ".png",
                    arrayOf(data.host)
                ) {
                    override fun getTileURLString(pMapTileIndex: Long): String {
                        val z = org.osmdroid.util.MapTileIndex.getZoom(pMapTileIndex)
                        val x = org.osmdroid.util.MapTileIndex.getX(pMapTileIndex)
                        val y = org.osmdroid.util.MapTileIndex.getY(pMapTileIndex)
                        return baseUrl + path + "/256/" + z + "/" + x + "/" + y + "/2/1_1.png"
                    }
                }
                val provider = MapTileProviderBasic(context, source)
                map.overlays.add(TilesOverlay(provider, context))
            }
            map.invalidate()
        }
    )
}

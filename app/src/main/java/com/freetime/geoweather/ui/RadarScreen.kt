package com.freetime.geoweather.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.clickable
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
import org.osmdroid.tileprovider.modules.SqlTileWriter
import org.osmdroid.tileprovider.tilesource.OnlineTileSourceBase
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.TilesOverlay
import java.net.URL

private data class RadarFrame(val path: String, val time: Long)\nprivate data class RadarFrames(val host: String, val frames: List<RadarFrame>)

@Composable
fun WeatherMapPreview(lat: Double, lon: Double, modifier: Modifier = Modifier, dataSaver: Boolean = false) {
    NativeWeatherMap(lat = lat, lon = lon, frameIndex = 11, radarVisible = !dataSaver, modifier = modifier)
}

@Composable
fun RadarScreen(lat: Double, lon: Double, onBack: () -> Unit, dataSaver: Boolean = false) {
    var playing by remember { mutableStateOf(!dataSaver) }
    var frameIndex by remember { mutableIntStateOf(0) }
    var radarVisible by remember { mutableStateOf(!dataSaver) }
    var baseMapVisible by remember { mutableStateOf(true) }
    var locationVisible by remember { mutableStateOf(true) }\n    var availableFrames by remember { mutableStateOf<List<RadarFrame>>(emptyList()) }\n    var radarLoadFailed by remember { mutableStateOf(false) }

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
            NativeWeatherMap(lat, lon, frameIndex, radarVisible, Modifier.fillMaxSize(), baseMapVisible, locationVisible) { frames, failed ->\n                availableFrames = frames\n                radarLoadFailed = failed\n                if (frames.isNotEmpty()) frameIndex = frameIndex.coerceIn(0, frames.lastIndex)\n            }
            if (dataSaver && !radarVisible) {
                FreetimeCard(modifier = Modifier.align(Alignment.Center).padding(24.dp).clickable { radarVisible = true }) {
                    FreetimeText(stringResource(Res.string.radar_data_saver_hint), modifier = Modifier.padding(14.dp))
                }
            }
            Row(
                modifier = Modifier.align(Alignment.TopCenter).padding(12.dp).horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FreetimeCard(modifier = Modifier.clickable { radarVisible = !radarVisible }) {
                    FreetimeText(if (radarVisible) stringResource(Res.string.radar_layer_on) else stringResource(Res.string.radar_layer_off), modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp))
                }
                FreetimeCard(modifier = Modifier.clickable { baseMapVisible = !baseMapVisible }) {
                    FreetimeText(
                        stringResource(if (baseMapVisible) Res.string.map_layer_osm_on else Res.string.map_layer_osm_off),
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                    )
                }
                FreetimeCard(modifier = Modifier.clickable { locationVisible = !locationVisible }) {
                    FreetimeText(
                        stringResource(if (locationVisible) Res.string.map_layer_location_on else Res.string.map_layer_location_off),
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                    )
                }
            }
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
                        contentDescription = if (playing) stringResource(Res.string.pause_radar) else stringResource(Res.string.play_radar),
                        onClick = { playing = !playing }
                    )
                    Column(Modifier.weight(1f)) {
                        FreetimeText(
                            if (frameIndex == 11) stringResource(Res.string.latest_radar) else stringResource(Res.string.radar_minutes_ago, (11 - frameIndex) * 10),
                            style = FreetimeDesign.typography.labelLarge
                        )
                        FreetimeText(
                            stringResource(Res.string.radar_attribution),
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
    radarVisible: Boolean,
    modifier: Modifier,
    baseMapVisible: Boolean = true,
    locationVisible: Boolean = true
) {
    val context = LocalContext.current
    var frames by remember { mutableStateOf<RadarFrames?>(null) }
    val radarOverlay = remember { mutableStateOf<TilesOverlay?>(null) }
    val radarPath = remember { mutableStateOf<String?>(null) }
    val locationMarker = remember { mutableStateOf<Marker?>(null) }

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
                val marker = Marker(this).apply {
                    position = GeoPoint(lat, lon)
                    setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                }
                overlays.add(marker)
                locationMarker.value = marker
            }
        },
        update = { map ->
            // Keep the MapView, viewport and base-map provider alive across
            // radar animation frames. Only mutate state that actually changed.
            map.overlayManager.tilesOverlay.isEnabled = baseMapVisible

            val marker = locationMarker.value ?: Marker(map).also {
                it.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                locationMarker.value = it
            }
            marker.position = GeoPoint(lat, lon)
            if (locationVisible) {
                if (!map.overlays.contains(marker)) map.overlays.add(marker)
            } else {
                map.overlays.remove(marker)
            }

            val data = frames
            val desiredPath = if (radarVisible && data != null && data.paths.isNotEmpty()) {
                data.paths[frameIndex.coerceIn(0, data.paths.lastIndex)]
            } else null

            if (desiredPath != radarPath.value) {
                radarOverlay.value?.let { old ->
                    map.overlays.remove(old)
                    old.onDetach(map)
                }
                radarOverlay.value = null
                radarPath.value = desiredPath
            }

            if (desiredPath != null && radarOverlay.value == null && data != null) {
                val path = desiredPath
                val source = object : OnlineTileSourceBase(
                    "RainViewer-" + path.hashCode(),
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
                val overlay = TilesOverlay(MapTileProviderBasic(context, source), context)
                radarOverlay.value = overlay
                map.overlays.add(overlay)
            }
            map.invalidate()
        }
    )
}


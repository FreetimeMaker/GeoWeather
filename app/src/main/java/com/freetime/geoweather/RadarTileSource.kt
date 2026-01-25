package com.freetime.geoweather

// MapLibre-Abhängigkeiten sind nicht verfügbar - diese Klasse wird derzeit nicht verwendet
// Wenn MapLibre benötigt wird, füge die Dependency hinzu:
// implementation("org.maplibre.gl:android-sdk:10.0.0")

/*
import org.maplibre.android.maps.Style
import org.maplibre.android.style.layers.PropertyFactory
import org.maplibre.android.style.layers.RasterLayer
import org.maplibre.android.style.sources.RasterSource
import org.maplibre.android.style.sources.TileSet

object RadarTileSource {
    fun addRadarLayer(style: Style) {
        // Falls du MeteoSwiss Open Data Radar willst:
        val tileUrl = "https://data.geo.admin.ch/ch.meteoschweiz.meteo-radar/{z}/{x}/{y}.png"

        val tileSet = TileSet("tileset", tileUrl)

        val radarSource = RasterSource(
            "radar-source",
            tileSet,
            256
        )

        style.addSource(radarSource)

        val radarLayer = RasterLayer("radar-layer", "radar-source")
        radarLayer.setProperties(
            PropertyFactory.rasterOpacity(0.6f)
        )

        style.addLayerAbove(radarLayer, "osm-layer")
    }
}
*/

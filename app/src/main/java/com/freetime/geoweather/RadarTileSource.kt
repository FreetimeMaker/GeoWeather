package com.freetime.geoweather;

import org.maplibre.android.style.layers.RasterLayer;
import org.maplibre.android.style.sources.RasterSource;
import org.maplibre.android.style.sources.TileSet;
import org.maplibre.android.maps.Style;

public class RadarTileSource {

    public static void addRadarLayer(Style style) {
        // Falls du MeteoSwiss Open Data Radar willst:
        String tileUrl = "https://data.geo.admin.ch/ch.meteoschweiz.meteo-radar/{z}/{x}/{y}.png";

        TileSet tileSet = new TileSet("tileset", tileUrl);

        RasterSource radarSource = new RasterSource(
                "radar-source",
                tileSet,
                256
        );

        style.addSource(radarSource);

        RasterLayer radarLayer = new RasterLayer("radar-layer", "radar-source");
        radarLayer.setProperties(
                org.maplibre.android.style.layers.PropertyFactory.rasterOpacity(0.6f)
        );

        style.addLayerAbove(radarLayer, "osm-layer");
    }
}

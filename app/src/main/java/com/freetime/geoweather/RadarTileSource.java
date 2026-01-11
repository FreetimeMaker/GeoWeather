package com.freetime.geoweather;

import com.mapbox.mapboxsdk.style.layers.RasterLayer;
import com.mapbox.mapboxsdk.style.sources.RasterSource;
import com.mapbox.mapboxsdk.style.sources.TileSet;
import com.mapbox.mapboxsdk.maps.Style;

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
                com.mapbox.mapboxsdk.style.layers.PropertyFactory.rasterOpacity(0.6f)
        );

        style.addLayerAbove(radarLayer, "osm-layer");
    }
}

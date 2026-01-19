package com.freetime.geoweather;

import android.content.Context;

import org.maplibre.android.MapLibre;

public class MapboxMapInitializer {

    private static boolean initialized = false;

    public static void init(Context ctx) {
        if (!initialized) {
            // MapLibre benötigt KEINEN API-Key
            MapLibre.getInstance(ctx);
            initialized = true;
        }
    }
}

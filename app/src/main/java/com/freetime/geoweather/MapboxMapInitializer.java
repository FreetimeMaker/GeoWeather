package com.freetime.geoweather;

import android.content.Context;

import com.mapbox.mapboxsdk.Mapbox;

public class MapboxMapInitializer {

    private static boolean initialized = false;

    public static void init(Context ctx) {
        if (!initialized) {
            // MapLibre benötigt KEINEN API-Key
            Mapbox.getInstance(ctx);
            initialized = true;
        }
    }
}

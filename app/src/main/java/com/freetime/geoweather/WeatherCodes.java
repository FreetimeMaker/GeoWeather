package com.freetime.geoweather;

import java.util.HashMap;
import java.util.Map;

public class WeatherCodes {

    private static final Map<Integer, String> CODES = new HashMap<>();

    static {
        CODES.put(0, "Clear sky");
        CODES.put(1, "Mainly clear");
        CODES.put(2, "Partly cloudy");
        CODES.put(3, "Overcast");
        CODES.put(45, "Fog");
        CODES.put(48, "Depositing rime fog");
        CODES.put(51, "Light drizzle");
        CODES.put(53, "Moderate drizzle");
        CODES.put(55, "Dense drizzle");
        CODES.put(61, "Slight rain");
        CODES.put(63, "Moderate rain");
        CODES.put(65, "Heavy rain");
        CODES.put(71, "Slight snow");
        CODES.put(73, "Moderate snow");
        CODES.put(75, "Heavy snow");
        CODES.put(80, "Rain showers");
        CODES.put(81, "Moderate rain showers");
        CODES.put(82, "Violent rain showers");
        CODES.put(95, "Thunderstorm");
        CODES.put(96, "Thunderstorm with slight hail");
        CODES.put(99, "Thunderstorm with heavy hail");
    }

    public static String getDescription(int code) {
        String d = CODES.get(code);
        return d != null ? d : "Unknown";
    }
}

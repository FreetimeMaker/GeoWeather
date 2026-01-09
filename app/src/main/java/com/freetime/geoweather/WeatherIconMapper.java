package com.freetime.geoweather;

import android.os.Build;

import java.time.LocalDateTime;

public class WeatherIconMapper {

    private static LocalDateTime sunrise;
    private static LocalDateTime sunset;

    public static void setSunTimes(String sunriseStr, String sunsetStr) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            sunrise = LocalDateTime.parse(sunriseStr);
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            sunset  = LocalDateTime.parse(sunsetStr);
        }
    }

    private static boolean isNight() {
        if (sunrise == null || sunset == null) {
            // Fallback: Nacht zwischen 20:00 und 06:00
            int hour = 0;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                hour = LocalDateTime.now().getHour();
            }
            return hour >= 20 || hour < 6;
        }

        LocalDateTime now = null;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            now = LocalDateTime.now();
        }
        return now.isBefore(sunrise) || now.isAfter(sunset);
    }

    public static int getWeatherIcon(int code) {
        boolean night = isNight();

        switch (code) {

            case 0:
                return night ? R.drawable.google_clear_night : R.drawable.google_clear_day;

            case 1:
                return night ? R.drawable.google_mostly_clear_night : R.drawable.google_mostly_clear_day;

            case 2:
                return night ? R.drawable.google_partly_cloudy_night : R.drawable.google_partly_cloudy_day;

            case 3:
                return night ? R.drawable.google_cloudy : R.drawable.google_cloudy;

            case 45:
            case 48:
                return night ? R.drawable.google_fog : R.drawable.google_fog;

            case 51:
            case 53:
            case 55:
                return night ? R.drawable.google_drizzle : R.drawable.google_drizzle;

            case 61:
            case 63:
            case 65:
                return night ? R.drawable.google_rain_with_sunny_dark : R.drawable.google_rain_with_sunny_light;

            case 71:
            case 73:
            case 75:
                return night ? R.drawable.google_snow_with_sunny_dark : R.drawable.google_snow_with_sunny_light;

            default:
                return night ? R.drawable.google_cloudy_with_sunny_dark : R.drawable.google_cloudy_with_sunny_light;
        }
    }
}

package com.freetime.geoweather;

import android.os.Build;
import android.text.TextUtils;

import com.freetime.geoweather.R;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;

public class WeatherIconMapper {

    private static ZonedDateTime sunriseTime;
    private static ZonedDateTime sunsetTime;

    public static void setSunTimes(String sunriseIso, String sunsetIso) {
        try {
            if (!TextUtils.isEmpty(sunriseIso)) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    sunriseTime = ZonedDateTime.parse(sunriseIso + ":00Z")
                            .withZoneSameInstant(ZoneId.systemDefault());
                }
            }
            if (!TextUtils.isEmpty(sunsetIso)) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    sunsetTime = ZonedDateTime.parse(sunsetIso + ":00Z")
                            .withZoneSameInstant(ZoneId.systemDefault());
                }
            }
        } catch (Exception e) {
            sunriseTime = null;
            sunsetTime = null;
        }
    }

    private static boolean isDaytime() {
        if (sunriseTime == null || sunsetTime == null) return true;
        ZonedDateTime now = null;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            now = ZonedDateTime.now(ZoneId.systemDefault());
        }
        return now.isAfter(sunriseTime) && now.isBefore(sunsetTime);
    }

    public static int getWeatherIcon(int code) {
        boolean night = isDaytime();

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

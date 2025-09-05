package com.freetime.geoweather;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

public class UpdateManager {

    private static final String GEO_WEATHER_API =
            "https://api.github.com/repos/FreetimeMaker/GeoWeather/releases/latest";
    private static final String APPSTORE_API =
            "https://api.github.com/repos/FreetimeMaker/Freetime-App-Store/releases/latest";
    private static final String UPDATER_PACKAGE = "com.freetime.appstore";

    public static void checkForUpdate(Context context) {
        new Thread(() -> {
            try {
                // 1. Check GeoWeather latest release
                JSONObject geoWeatherRelease = fetchJson(GEO_WEATHER_API);
                String latestTag = geoWeatherRelease.getString("tag_name"); // e.g., "v1.2.3"

                if (isNewerVersion(latestTag, BuildConfig.VERSION_NAME)) {
                    // 2. If newer, launch updater or direct-download APK
                    ((android.app.Activity) context).runOnUiThread(() ->
                            launchUpdaterOrDirectDownload(context)
                    );
                }

            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

    private static JSONObject fetchJson(String apiUrl) throws Exception {
        URL url = new URL(apiUrl);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");

        BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
        StringBuilder response = new StringBuilder();
        String line;
        while ((line = in.readLine()) != null) {
            response.append(line);
        }
        in.close();

        return new JSONObject(response.toString());
    }

    private static boolean isNewerVersion(String latest, String current) {
        latest = latest.startsWith("v") ? latest.substring(1) : latest;
        return !latest.equals(current);
    }

    private static void launchUpdaterOrDirectDownload(Context context) {
        if (isAppInstalled(context, UPDATER_PACKAGE)) {
            // Launch Freetime App Store
            Intent launchIntent = context.getPackageManager()
                    .getLaunchIntentForPackage(UPDATER_PACKAGE);
            if (launchIntent != null) {
                context.startActivity(launchIntent);
            }
        } else {
            // Fetch latest APK from Freetime App Store GitHub Releases
            new Thread(() -> {
                try {
                    JSONObject appStoreRelease = fetchJson(APPSTORE_API);
                    JSONArray assets = appStoreRelease.getJSONArray("assets");
                    if (assets.length() > 0) {
                        String apkUrl = assets.getJSONObject(0)
                                .getString("browser_download_url");

                        Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(apkUrl));
                        context.startActivity(browserIntent);
                    } else {
                        // Fallback to release page if no assets found
                        Intent browserIntent = new Intent(Intent.ACTION_VIEW,
                                Uri.parse("https://github.com/FreetimeMaker/Freetime-App-Store/releases/latest"));
                        context.startActivity(browserIntent);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }).start();
        }
    }

    private static boolean isAppInstalled(Context context, String packageName) {
        try {
            context.getPackageManager().getPackageInfo(packageName, 0);
            return true;
        } catch (PackageManager.NameNotFoundException e) {
            return false;
        }
    }
}

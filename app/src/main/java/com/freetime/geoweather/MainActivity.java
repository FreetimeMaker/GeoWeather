package com.freetime.geoweather;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.FileProvider;
import androidx.preference.PreferenceManager;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;

public class MainActivity extends AppCompatActivity implements LocationListener {

    private static final int REQ_CODE_LOCATION = 1001;

    private LocationManager locationManager;
    private EditText editCity;
    private TextView txtLocation, txtTemperature, txtDescription, txtWind, txtHumidity;
    private Button btnGetWeather, btnDonate;

    private static final Map<Integer, String> WEATHER_CODES = Map.ofEntries(
            Map.entry(0, "Clear sky"),
            Map.entry(1, "Mostly clear"),
            Map.entry(2, "Partly cloudy"),
            Map.entry(3, "Overcast"),
            Map.entry(45, "Fog"),
            Map.entry(48, "Depositing rime fog"),
            Map.entry(51, "Light drizzle"),
            Map.entry(53, "Moderate drizzle"),
            Map.entry(55, "Dense drizzle"),
            Map.entry(61, "Slight rain"),
            Map.entry(63, "Moderate rain"),
            Map.entry(65, "Heavy rain"),
            Map.entry(71, "Slight snow"),
            Map.entry(73, "Moderate snow"),
            Map.entry(75, "Heavy snow")
    );

    @Override
    public void onLocationChanged(Location location) {
        double lat = location.getLatitude();
        double lon = location.getLongitude();
        txtLocation.setText("Lat: " + lat + ", Lon: " + lon);
        // You can call helper methods from here
        requestLocationPermissionAndStart();
    }

    private void requestLocationPermissionAndStart() {
        if (ActivityCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, REQ_CODE_LOCATION);
        } else {
            startLocationUpdates();
        }
    }

    private void startLocationUpdates() {
        try {
            if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                locationManager.requestLocationUpdates(
                        LocationManager.GPS_PROVIDER, 5000, 10, this);
            }
            if (locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
                locationManager.requestLocationUpdates(
                        LocationManager.NETWORK_PROVIDER, 10000, 50, this);
            }
        } catch (SecurityException e) {
            Log.e("Location", "Permission error: " + e.getMessage());
        }
    }

    @Override
    public void onProviderEnabled(String provider) {
        // Called when GPS/network is turned ON
    }

    @Override
    public void onProviderDisabled(String provider) {
        // Called when GPS/network is turned OFF
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        editCity = findViewById(R.id.editCity);
        txtLocation = findViewById(R.id.txtLocation);
        txtTemperature = findViewById(R.id.txtTemperature);
        txtDescription = findViewById(R.id.txtDescription);
        txtWind = findViewById(R.id.txtWind);
        txtHumidity = findViewById(R.id.txtHumidity);
        btnGetWeather = findViewById(R.id.btnGetWeather);
        btnDonate = findViewById(R.id.btnOpenDonate);

        locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);

        btnGetWeather.setOnClickListener(v -> {
            String city = editCity.getText().toString().trim();
            if (!city.isEmpty()) {
                fetchWeatherByCity(city);
            }
        });

        btnDonate.setOnClickListener(v ->
                startActivity(new Intent(MainActivity.this, DonateActivity.class))
        );

        // Automatically check GitHub for a newer release
        checkAndUpdateFromGitHub();

    }

    private void checkAndUpdateFromGitHub() {
        new Thread(() -> {
            try {
                // Replace with your actual username/repo
                String apiUrl = "https://api.github.com/repos/FreetimeMaker/GeoWeather/releases/latest";
                String json = httpGet(apiUrl, "GeoWeatherApp");

                JSONObject release = new JSONObject(json);
                String latestVersion = release.getString("tag_name").replace("v", "");
                PackageInfo pInfo = getPackageManager()
                        .getPackageInfo(getPackageName(), 0);
                String currentVersion = pInfo.versionName;

                if (latestVersion.equals(currentVersion)) {
                    Log.d("Update", "Already up to date");
                    return;
                }

                JSONArray assets = release.getJSONArray("assets");
                if (assets.length() == 0) {
                    Log.w("Update", "No APK asset found in latest release");
                    return;
                }

                String apkUrl = assets.getJSONObject(0).getString("browser_download_url");

                runOnUiThread(() -> Toast.makeText(this,
                        "Downloading update " + latestVersion, Toast.LENGTH_SHORT).show());

                File apkFile = downloadApk(apkUrl, "update.apk");
                if (apkFile != null) {
                    runOnUiThread(() -> installApk(apkFile));
                }

            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

    private File downloadApk(String urlStr, String fileName) {
        try {
            URL url = new URL(urlStr);
            HttpURLConnection c = (HttpURLConnection) url.openConnection();
            c.connect();
            File file = new File(getExternalFilesDir(null), fileName);
            try (InputStream in = c.getInputStream();
                 FileOutputStream out = new FileOutputStream(file)) {
                byte[] buf = new byte[4096];
                int len;
                while ((len = in.read(buf)) > 0) {
                    out.write(buf, 0, len);
                }
            }
            return file;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private void installApk(File apkFile) {
        Uri apkUri = FileProvider.getUriForFile(
                this,
                getPackageName() + ".fileprovider",
                apkFile
        );
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setDataAndType(apkUri, "application/vnd.android.package-archive");
        intent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
    }

    private void fetchWeatherByCity(String city) {
        new Thread(() -> {
            try {
                // Step 1: Geocode the city to lat/lon
                String geoUrl = null;
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    geoUrl = "https://geocoding-api.open-meteo.com/v1/search?name="
                            + URLEncoder.encode(city, StandardCharsets.UTF_8)
                            + "&count=1&language=en&format=json";
                }
                String geoJson = httpGet(geoUrl, "GeoWeatherApp");
                JSONObject geoObj = new JSONObject(geoJson);
                JSONArray results = geoObj.optJSONArray("results");
                if (results == null || results.length() == 0) {
                    runOnUiThread(() ->
                            Toast.makeText(this, "City not found", Toast.LENGTH_SHORT).show()
                    );
                    return;
                }
                JSONObject first = results.getJSONObject(0);
                double lat = first.getDouble("latitude");
                double lon = first.getDouble("longitude");
                String resolvedName = first.getString("name");

            } catch (Exception e) {
                e.printStackTrace();
                runOnUiThread(() ->
                        Toast.makeText(this, "Error getting city location", Toast.LENGTH_SHORT).show()
                );
            }
        }).start();
    }

    private void parseAndDisplayOpenMeteo(String json, String locationLabel) {
        try {
            JSONObject obj = new JSONObject(json).getJSONObject("current_weather");

            double temp = obj.getDouble("temperature");
            double windSpeed = obj.getDouble("windspeed");
            int weatherCode = obj.getInt("weathercode");

            txtLocation.setText(locationLabel);
            txtTemperature.setText(String.format("%.1f°C", temp));
            txtWind.setText(windSpeed + " km/h");
            txtHumidity.setText("—"); // Humidity not in current_weather; needs extra param
            txtDescription.setText(WEATHER_CODES.getOrDefault(weatherCode, "Unknown"));

        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "Error reading weather data", Toast.LENGTH_SHORT).show();
        }
    }


    private void showError(String cityNotFound) {
    }

    private String httpGet(String urlString, String userAgent) throws Exception {
        URL url = new URL(urlString);
        HttpURLConnection c = (HttpURLConnection) url.openConnection();
        c.setRequestProperty("User-Agent", userAgent);
        c.setConnectTimeout(12000);
        c.setReadTimeout(12000);

        try (BufferedReader in = new BufferedReader(
                new InputStreamReader(c.getInputStream(), StandardCharsets.UTF_8))) {

            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = in.readLine()) != null) {
                sb.append(line);
            }
            return sb.toString();
        }
    }
}
package com.freetime.geoweather;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.FileProvider;
import androidx.preference.PreferenceManager;

import org.json.JSONArray;
import org.json.JSONObject;
import org.osmdroid.config.Configuration;
import org.osmdroid.events.MapEventsReceiver;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.MapEventsOverlay;
import org.osmdroid.views.overlay.Marker;

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
    private MapView map;

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
        map = findViewById(R.id.map);

        locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);

        btnGetWeather.setOnClickListener(v -> {
            String city = editCity.getText().toString().trim();
            if (!city.isEmpty()) {
                fetchWeatherByCity(city);
            } else {
                requestLocationPermissionAndStart();
            }
        });

        btnDonate.setOnClickListener(v ->
                startActivity(new Intent(MainActivity.this, DonateActivity.class))
        );

        requestLocationPermissionAndStart();

        // --- MAP INIT ---
        Configuration.getInstance().load(getApplicationContext(),
                PreferenceManager.getDefaultSharedPreferences(getApplicationContext()));

        map.setMultiTouchControls(true);
        map.getController().setZoom(5.0);
        map.getController().setCenter(new GeoPoint(47.0, 8.0));

        // Tap-to-select overlay
        MapEventsReceiver tapReceiver = new MapEventsReceiver() {
            @Override
            public boolean singleTapConfirmedHelper(GeoPoint p) {
                Marker marker = new Marker(map);
                marker.setPosition(p);
                marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
                map.getOverlays().clear();
                map.getOverlays().add(new MapEventsOverlay(this)); // keep listener
                map.getOverlays().add(marker);
                map.invalidate();

                fetchWeatherByCoords(p.getLatitude(), p.getLongitude(),
                        String.format("Lat %.4f, Lon %.4f", p.getLatitude(), p.getLongitude()));
                return true;
            }

            @Override
            public boolean longPressHelper(GeoPoint p) {
                return false;
            }
        };
        map.getOverlays().add(new MapEventsOverlay(tapReceiver));

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
                locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER,
                        5000, 10, this);
            }
            if (locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
                locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER,
                        10000, 50, this);
            }
            Location lastKnown = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
            if (lastKnown == null) {
                lastKnown = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
            }
            if (lastKnown != null) {
                handleLocation(lastKnown);
            }
        } catch (SecurityException e) {
            Log.e("Location", "Permission error: " + e.getMessage());
        }
    }

    @Override
    public void onLocationChanged(@NonNull Location location) {
        handleLocation(location);
        // Also move the map to the new location
        map.getController().setZoom(10.0);
        map.getController().setCenter(new GeoPoint(location.getLatitude(), location.getLongitude()));
    }

    private void handleLocation(Location location) {
        fetchWeatherByCoords(location.getLatitude(), location.getLongitude(), "Current location");
    }

    private void fetchWeatherByCity(String city) {
        new Thread(() -> {
            try {
                String q = URLEncoder.encode(city, StandardCharsets.UTF_8.name());
                String geoUrl = "https://nominatim.openstreetmap.org/search?q=" + q +
                        "&format=json&limit=1";
                String geoResp = httpGet(geoUrl, "GeoWeatherApp");
                JSONArray geoArr = new JSONArray(geoResp);
                if (geoArr.length() == 0) {
                    showError("City not found");
                    return;
                }
                JSONObject geo = geoArr.getJSONObject(0);
                double lat = Double.parseDouble(geo.getString("lat"));
                double lon = Double.parseDouble(geo.getString("lon"));

                fetchWeatherByCoords(lat, lon, city);
            } catch (Exception e) {
                e.printStackTrace();
                showError("Error with geocoding");
            }
        }).start();
    }

    private void showError(String cityNotFound) {
    }

    private void fetchWeatherByCoords(double lat, double lon, String locationName) {

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
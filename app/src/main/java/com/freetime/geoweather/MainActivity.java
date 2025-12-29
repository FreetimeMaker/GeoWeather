package com.freetime.geoweather;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.ViewModelProvider;
import androidx.preference.PreferenceManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.freetime.geoweather.ui.LocationsAdapter;
import com.freetime.geoweather.ui.LocationsViewModel;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;

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
        if (txtLocation != null) {
        txtLocation.setText("Lat: " + lat + ", Lon: " + lon);
        }
        // You can call helper methods from here
        requestLocationPermissionAndStart();
    }

    private void requestLocationPermissionAndStart() {
        if (ContextCompat.checkSelfPermission(this,
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

    private void hideSystemUI() {
        View decorView = getWindow().getDecorView();
        decorView.setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_FULLSCREEN);
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) {
            hideSystemUI();
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // Fullscreen-Modus aktivieren
        hideSystemUI();
        
        setContentView(R.layout.activity_main);

        // Note: These views don't exist in the current layout (activity_main.xml)
        // They are commented out to avoid compilation errors
        // If you need these views, add them to the layout file
        // editCity = findViewById(R.id.editCity);
        // txtLocation = findViewById(R.id.txtLocation);
        // txtTemperature = findViewById(R.id.txtTemperature);
        // txtDescription = findViewById(R.id.txtDescription);
        // txtWind = findViewById(R.id.txtWind);
        // txtHumidity = findViewById(R.id.txtHumidity);
        // btnGetWeather = findViewById(R.id.btnGetWeather);
        // btnDonate = findViewById(R.id.btnOpenDonate);

        locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);

        if (btnGetWeather != null) {
        btnGetWeather.setOnClickListener(v -> {
                if (editCity != null) {
            String city = editCity.getText().toString().trim();
            if (!city.isEmpty()) {
                fetchWeatherByCity(city);
                    }
            }
        });
        }

        if (btnDonate != null) {
        btnDonate.setOnClickListener(v ->
                startActivity(new Intent(MainActivity.this, DonateActivity.class))
        );
        }

        // Commented out update check - methods not implemented
        // int currentVersionCode = BuildConfig.VERSION_CODE;
        // int latestVersionCode = fetchLatestVersionCodeFromServer();
        // if (latestVersionCode > currentVersionCode) {
        //     launchUpdaterOrBrowser();
        // }
        // UpdateManager.checkForUpdate(this);

        // RecyclerView für Orte
        RecyclerView rv = findViewById(R.id.rvLocations);
        final LocationsAdapter adapter = new LocationsAdapter();
        rv.setLayoutManager(new LinearLayoutManager(this));
        rv.setAdapter(adapter);

        // ViewModel
        LocationsViewModel vm = new ViewModelProvider(this).get(LocationsViewModel.class);
        vm.locations.observe(this, adapter::setItems);

        // Listener für das Löschen von Orten
        adapter.setOnItemDeleteListener(vm::deleteLocation);

        // Listener für das Klicken auf einen Ort, um das Wetter anzuzeigen
        adapter.setOnItemClickListener(location -> {
            Intent intent = new Intent(MainActivity.this, WeatherDetailActivity.class);
            intent.putExtra("location_name", location.getName());
            intent.putExtra("latitude", location.getLatitude());
            intent.putExtra("longitude", location.getLongitude());
            startActivity(intent);
        });

        // FloatingActionButton zum Hinzufügen
        findViewById(R.id.fabAdd).setOnClickListener(v -> {
            showAddLocationDialog(vm);
        });
    }

    // Einfache Klasse für Suchergebnisse
    private static class LocationSearchResult {
        String name;
        String country;
        String admin1; // Bundesland/Region
        double latitude;
        double longitude;

        LocationSearchResult(String name, String country, String admin1, double lat, double lon) {
            this.name = name;
            this.country = country;
            this.admin1 = admin1;
            this.latitude = lat;
            this.longitude = lon;
        }

        String getDisplayName() {
            if (admin1 != null && !admin1.isEmpty()) {
                return name + ", " + admin1 + ", " + country;
            }
            return name + ", " + country;
        }
    }

    private void showAddLocationDialog(LocationsViewModel vm) {
        // Layout für den Dialog erstellen
        LinearLayout dialogLayout = new LinearLayout(this);
        dialogLayout.setOrientation(LinearLayout.VERTICAL);
        dialogLayout.setPadding(32, 16, 32, 16);

        // EditText für die Suche
        final EditText inputSearch = new EditText(this);
        inputSearch.setHint("Ortsname eingeben (z.B. Berlin, Paris, New York)");
        inputSearch.setMinHeight(120);
        dialogLayout.addView(inputSearch);

        // RecyclerView für Ergebnisse
        RecyclerView resultsRecyclerView = new RecyclerView(this);
        resultsRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        resultsRecyclerView.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 600));
        dialogLayout.addView(resultsRecyclerView);

        // ProgressBar
        ProgressBar progressBar = new ProgressBar(this);
        progressBar.setVisibility(View.GONE);
        dialogLayout.addView(progressBar);

        // Adapter für Suchergebnisse
        List<LocationSearchResult> searchResults = new ArrayList<>();
        
        // Dialog-Variable für späteren Zugriff
        final androidx.appcompat.app.AlertDialog[] dialogRef = new androidx.appcompat.app.AlertDialog[1];
        
        androidx.recyclerview.widget.RecyclerView.Adapter<LocationSearchViewHolder> resultsAdapter = 
            new androidx.recyclerview.widget.RecyclerView.Adapter<LocationSearchViewHolder>() {
            @Override
            public LocationSearchViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
                View view = LayoutInflater.from(parent.getContext())
                        .inflate(android.R.layout.simple_list_item_2, parent, false);
                return new LocationSearchViewHolder(view);
            }

            @Override
            public void onBindViewHolder(LocationSearchViewHolder holder, int position) {
                LocationSearchResult result = searchResults.get(position);
                holder.text1.setText(result.name);
                String details = String.format("%s, %s (%.4f, %.4f)", 
                    result.country, 
                    result.admin1 != null ? result.admin1 : "",
                    result.latitude, 
                    result.longitude);
                holder.text2.setText(details);
                holder.itemView.setOnClickListener(v -> {
                    vm.addLocation(result.name, result.latitude, result.longitude);
                    if (dialogRef[0] != null) {
                        dialogRef[0].dismiss();
                    }
                    Toast.makeText(MainActivity.this, 
                        result.getDisplayName() + " hinzugefügt", 
                        Toast.LENGTH_SHORT).show();
                });
            }

            @Override
            public int getItemCount() {
                return searchResults.size();
            }
        };
        resultsRecyclerView.setAdapter(resultsAdapter);

        // Button zum Suchen
        Button searchButton = new Button(this);
        searchButton.setText("Suchen");
        dialogLayout.addView(searchButton);

        // Suchfunktion
        searchButton.setOnClickListener(v -> {
            String searchQuery = inputSearch.getText().toString().trim();
            if (searchQuery.isEmpty()) {
                Toast.makeText(MainActivity.this, "Bitte einen Ortsnamen eingeben", Toast.LENGTH_SHORT).show();
                return;
            }

            progressBar.setVisibility(View.VISIBLE);
            searchResults.clear();
            resultsAdapter.notifyDataSetChanged();

            new Thread(() -> {
                try {
                    String geoUrl = "https://geocoding-api.open-meteo.com/v1/search?name="
                            + URLEncoder.encode(searchQuery, StandardCharsets.UTF_8)
                            + "&count=20&language=de&format=json";
                    
                    String geoJson = httpGet(geoUrl, "GeoWeatherApp");
                    JSONObject geoObj = new JSONObject(geoJson);
                    JSONArray results = geoObj.optJSONArray("results");

                    runOnUiThread(() -> {
                        progressBar.setVisibility(View.GONE);
                        
                        if (results == null || results.length() == 0) {
                            Toast.makeText(MainActivity.this, 
                                "Keine Orte gefunden", 
                                Toast.LENGTH_SHORT).show();
                            return;
                        }

                        try {
                            searchResults.clear();
                            for (int i = 0; i < results.length(); i++) {
                                JSONObject item = results.getJSONObject(i);
                                String name = item.getString("name");
                                String country = item.optString("country", "");
                                String admin1 = item.optString("admin1", "");
                                double lat = item.getDouble("latitude");
                                double lon = item.getDouble("longitude");
                                searchResults.add(new LocationSearchResult(name, country, admin1, lat, lon));
                            }
                            resultsAdapter.notifyDataSetChanged();
                        } catch (org.json.JSONException e) {
                            Toast.makeText(MainActivity.this, 
                                "Fehler beim Verarbeiten der Ergebnisse: " + e.getMessage(), 
                                Toast.LENGTH_SHORT).show();
                            Log.e("LocationSearch", "Error parsing JSON results", e);
                        }
                    });
                } catch (Exception e) {
                    runOnUiThread(() -> {
                        progressBar.setVisibility(View.GONE);
                        Toast.makeText(MainActivity.this, 
                            "Fehler bei der Suche: " + e.getMessage(), 
                            Toast.LENGTH_SHORT).show();
                        Log.e("LocationSearch", "Error searching locations", e);
                    });
                }
            }).start();
        });

        // Dialog erstellen
        dialogRef[0] = new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Ort suchen und hinzufügen")
                .setView(dialogLayout)
                .setNegativeButton("Abbrechen", null)
                .create();
        dialogRef[0].show();
    }

    // ViewHolder für Suchergebnisse
    private static class LocationSearchViewHolder extends RecyclerView.ViewHolder {
        TextView text1, text2;

        LocationSearchViewHolder(View itemView) {
            super(itemView);
            text1 = itemView.findViewById(android.R.id.text1);
            text2 = itemView.findViewById(android.R.id.text2);
        }
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

            if (txtLocation != null) txtLocation.setText(locationLabel);
            if (txtTemperature != null) txtTemperature.setText(String.format("%.1f°C", temp));
            if (txtWind != null) txtWind.setText(windSpeed + " km/h");
            if (txtHumidity != null) txtHumidity.setText("—"); // Humidity not in current_weather; needs extra param
            if (txtDescription != null) txtDescription.setText(WEATHER_CODES.getOrDefault(weatherCode, "Unknown"));

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
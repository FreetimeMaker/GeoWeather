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

import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.widget.ProgressBar;

public class MainActivity extends AppCompatActivity implements LocationListener {

    private static final int REQ_CODE_LOCATION = 1001;

    private LocationManager locationManager;

    @Override
    public void onLocationChanged(Location location) {
        double lat = location.getLatitude();
        double lon = location.getLongitude();

        // Direkt zur Detailseite springen
        Intent intent = new Intent(MainActivity.this, WeatherDetailActivity.class);
        intent.putExtra("location_name", "Your Location");
        intent.putExtra("latitude", lat);
        intent.putExtra("longitude", lon);
        startActivity(intent);
    }

    private void requestLocationPermissionAndStart() {
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    REQ_CODE_LOCATION);

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
    public void onProviderEnabled(String provider) {}
    @Override
    public void onProviderDisabled(String provider) {}

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
        if (hasFocus) hideSystemUI();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        hideSystemUI();
        setContentView(R.layout.activity_main);

        locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);

        // RecyclerView für gespeicherte Orte
        RecyclerView rv = findViewById(R.id.rvLocations);
        final LocationsAdapter adapter = new LocationsAdapter();
        rv.setLayoutManager(new LinearLayoutManager(this));
        rv.setAdapter(adapter);

        // ViewModel
        LocationsViewModel vm = new ViewModelProvider(this).get(LocationsViewModel.class);
        vm.locations.observe(this, adapter::setItems);

        // Löschen
        adapter.setOnItemDeleteListener(vm::deleteLocation);

        // Klick → WeatherDetailActivity
        adapter.setOnItemClickListener(location -> {
            Intent intent = new Intent(MainActivity.this, WeatherDetailActivity.class);
            intent.putExtra("location_name", location.getName());
            intent.putExtra("latitude", location.getLatitude());
            intent.putExtra("longitude", location.getLongitude());
            startActivity(intent);
        });

        // FAB: Ort hinzufügen
        findViewById(R.id.fabAdd).setOnClickListener(v -> showAddLocationDialog(vm));

        // FAB: Spenden
        findViewById(R.id.fabDono).setOnClickListener(v ->
                startActivity(new Intent(MainActivity.this, DonateActivity.class))
        );

        }

    // -----------------------------
    // LOCATION SEARCH DIALOG
    // -----------------------------

    private static class LocationSearchResult {
        String name, country, admin1;
        double latitude, longitude;

        LocationSearchResult(String name, String country, String admin1, double lat, double lon) {
            this.name = name;
            this.country = country;
            this.admin1 = admin1;
            this.latitude = lat;
            this.longitude = lon;
        }

        String getDisplayName() {
            if (admin1 != null && !admin1.isEmpty())
                return name + ", " + admin1 + ", " + country;
            return name + ", " + country;
        }
    }

    private void showAddLocationDialog(LocationsViewModel vm) {

        LinearLayout dialogLayout = new LinearLayout(this);
        dialogLayout.setOrientation(LinearLayout.VERTICAL);
        dialogLayout.setPadding(32, 16, 32, 16);

        EditText inputSearch = new EditText(this);
        inputSearch.setHint("Enter City (Berlin, Paris, New York)");
        inputSearch.setMinHeight(120);
        dialogLayout.addView(inputSearch);

        RecyclerView resultsRecyclerView = new RecyclerView(this);
        resultsRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        resultsRecyclerView.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 600));
        dialogLayout.addView(resultsRecyclerView);

        ProgressBar progressBar = new ProgressBar(this);
        progressBar.setVisibility(View.GONE);
        dialogLayout.addView(progressBar);

        List<LocationSearchResult> searchResults = new ArrayList<>();

        final androidx.appcompat.app.AlertDialog[] dialogRef = new androidx.appcompat.app.AlertDialog[1];

        RecyclerView.Adapter<LocationSearchViewHolder> resultsAdapter =
                new RecyclerView.Adapter<LocationSearchViewHolder>() {

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
                        holder.text2.setText(
                                String.format("%s, %s (%.4f, %.4f)",
                                        result.country,
                                        result.admin1 != null ? result.admin1 : "",
                                        result.latitude,
                                        result.longitude)
                        );

                        holder.itemView.setOnClickListener(v -> {
                            vm.addLocation(result.getDisplayName(), result.latitude, result.longitude);
                            if (dialogRef[0] != null) dialogRef[0].dismiss();
                            Toast.makeText(MainActivity.this,
                                    result.getDisplayName() + " added",
                                    Toast.LENGTH_SHORT).show();
                        });
                    }

                    @Override
                    public int getItemCount() {
                        return searchResults.size();
                    }
                };

        resultsRecyclerView.setAdapter(resultsAdapter);

        Button searchButton = new Button(this);
        searchButton.setText("Search");
        dialogLayout.addView(searchButton);

        searchButton.setOnClickListener(v -> {
            String searchQuery = inputSearch.getText().toString().trim();
            if (searchQuery.isEmpty()) {
                Toast.makeText(MainActivity.this, "Please enter a City", Toast.LENGTH_SHORT).show();
                return;
            }

            progressBar.setVisibility(View.VISIBLE);
            searchResults.clear();
            resultsAdapter.notifyDataSetChanged();

            new Thread(() -> {
                try {
                    String geoUrl =
                            null;
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        geoUrl = "https://geocoding-api.open-meteo.com/v1/search?name="
                                + URLEncoder.encode(searchQuery, StandardCharsets.UTF_8)
                                + "&count=20&language=de&format=json";
                    }

                    String geoJson = httpGet(geoUrl, "GeoWeatherApp");
                    JSONObject geoObj = new JSONObject(geoJson);
                    JSONArray results = geoObj.optJSONArray("results");

                    runOnUiThread(() -> {
                        progressBar.setVisibility(View.GONE);

                        if (results == null || results.length() == 0) {
                            Toast.makeText(MainActivity.this, "No Cities found", Toast.LENGTH_SHORT).show();
                            return;
                        }

                        try {
                            searchResults.clear();
                            for (int i = 0; i < results.length(); i++) {
                                JSONObject item = results.getJSONObject(i);

                                searchResults.add(new LocationSearchResult(
                                        item.getString("name"),
                                        item.optString("country", ""),
                                        item.optString("admin1", ""),
                                        item.getDouble("latitude"),
                                        item.getDouble("longitude")
                                ));
                            }
                            resultsAdapter.notifyDataSetChanged();

                        } catch (Exception e) {
                            Toast.makeText(MainActivity.this,
                                    "Error processing results: " + e.getMessage(),
                                    Toast.LENGTH_SHORT).show();
                        }
                    });

                } catch (Exception e) {
                    runOnUiThread(() -> {
                        progressBar.setVisibility(View.GONE);
                        Toast.makeText(MainActivity.this,
                                "Error searching locations: " + e.getMessage(),
                                Toast.LENGTH_SHORT).show();
                    });
                }
            }).start();
        });

        dialogRef[0] = new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Search for a City and Add it")
                .setView(dialogLayout)
                .setNegativeButton("Cancel", null)
                .create();

        dialogRef[0].show();
    }

    private static class LocationSearchViewHolder extends RecyclerView.ViewHolder {
        TextView text1, text2;

        LocationSearchViewHolder(View itemView) {
            super(itemView);
            text1 = itemView.findViewById(android.R.id.text1);
            text2 = itemView.findViewById(android.R.id.text2);
        }
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
            while ((line = in.readLine()) != null) sb.append(line);
            return sb.toString();
        }
    }
}

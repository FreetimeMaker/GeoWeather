package com.freetime.geoweather;

import static androidx.core.content.ContextCompat.startActivity;

import android.Manifest;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.freetime.geoweather.data.LocationDatabase;
import com.freetime.geoweather.ui.LocationsAdapter;
import com.freetime.geoweather.ui.LocationsViewModel;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

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
import java.util.Locale;

import android.view.LayoutInflater;
import android.view.ViewGroup;

public class MainActivity extends AppCompatActivity implements LocationListener
{
    private static final int REQ_CODE_LOCATION = 1001;
    private LocationManager locationManager;
    private LocationsViewModel vm;

    @Override
    public void onLocationChanged(Location location) {
        if (vm == null) {
            return;
        }
        double lat = location.getLatitude();
        double lon = location.getLongitude();

        // Reverse-Geocoding
        String displayName = reverseGeocode(lat, lon);

        // On first start, automatically save
        if (isFirstStart()) {
            vm.addLocation(displayName, lat, lon);
            setFirstStartDone();
        }

        // Open detail page
        Intent intent = new Intent(MainActivity.this, WeatherDetailActivity.class);
        intent.putExtra("location_name", displayName);
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

    private boolean isFirstStart() {
        return getSharedPreferences("app_prefs", MODE_PRIVATE)
                .getBoolean("first_start", true);
    }

    private void setFirstStartDone() {
        getSharedPreferences("app_prefs", MODE_PRIVATE)
                .edit()
                .putBoolean("first_start", false)
                .apply();
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
        loadLocale(); // Load language
        hideSystemUI();
        setContentView(R.layout.activity_main);

        locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);

        // Initialize ViewModel
        vm = new ViewModelProvider(this).get(LocationsViewModel.class);

        // Check if locations are already saved; only request GPS on the very first start (no locations)
        LocationDatabase.databaseWriteExecutor.execute(() -> {
            int count = LocationDatabase.getDatabase(getApplication()).locationDao().getCount();
            if (count == 0 && isFirstStart()) {
                runOnUiThread(this::requestLocationPermissionAndStart);
            } else if (isFirstStart()) {
                // If locations are already present, mark as done so it doesn't run again
                setFirstStartDone();
            }
        });

        // RecyclerView for saved locations
        RecyclerView rv = findViewById(R.id.rvLocations);
        final LocationsAdapter adapter = new LocationsAdapter();
        rv.setLayoutManager(new LinearLayoutManager(this));
        rv.setAdapter(adapter);

        // ViewModel
        vm.locations.observe(this, adapter::setItems);

        // Delete
        adapter.setOnItemDeleteListener(vm::deleteLocation);

        // Click -> WeatherDetailActivity
        adapter.setOnItemClickListener(location -> {
            Intent intent = new Intent(MainActivity.this, WeatherDetailActivity.class);
            intent.putExtra("location_name", location.getName());
            intent.putExtra("latitude", location.getLatitude());
            intent.putExtra("longitude", location.getLongitude());
            startActivity(intent);
        });

        // FAB: Add location
        findViewById(R.id.fabAdd).setOnClickListener(v -> showAddLocationDialog(vm));

        // FAB: Donate
        findViewById(R.id.fabDono).setOnClickListener(v ->
                startActivity(new Intent(MainActivity.this, DonateActivity.class))
        );

        // FAB: Change Language
        FloatingActionButton fabLanguage = findViewById(R.id.fabLanguage);
        fabLanguage.setOnClickListener(v -> showLanguageDialog());
    }

    private void showLanguageDialog() {
        final String[] languages = getResources().getStringArray(R.array.languages_array);
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Choose Language");
        builder.setItems(languages, (dialog, which) -> {
            String language = languages[which];
            if (language.equals("English")) {
                setLocale("en");
            } else if (language.equals("German")) {
                setLocale("de");
            } else if (language.equals("Spanish")) {
                setLocale("es");
            }
        });
        builder.show();
    }

    private void setLocale(String lang) {
        Locale myLocale = new Locale(lang);
        Resources res = getResources();
        DisplayMetrics dm = res.getDisplayMetrics();
        Configuration conf = res.getConfiguration();
        conf.locale = myLocale;
        res.updateConfiguration(conf, dm);
        SharedPreferences.Editor editor = getSharedPreferences("Settings", MODE_PRIVATE).edit();
        editor.putString("My_Lang", lang);
        editor.apply();
        Intent refresh = new Intent(this, MainActivity.class);
        finish();
        startActivity(refresh);
    }

    public void loadLocale() {
        SharedPreferences prefs = getSharedPreferences("Settings", MODE_PRIVATE);
        String language = prefs.getString("My_Lang", "");
        if (!language.isEmpty()) {
            //setLocale(language);
        }
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

    private String reverseGeocode(double lat, double lon) {
        try {
            String url = "https://geocoding-api.open-meteo.com/v1/reverse?latitude="
                    + lat + "&longitude=" + lon;

            String json = httpGet(url, "GeoWeatherApp");
            JSONObject obj = new JSONObject(json);

            JSONArray results = obj.optJSONArray("results");
            if (results != null && results.length() > 0) {
                JSONObject item = results.getJSONObject(0);

                String name = item.optString("name", "");
                String admin1 = item.optString("admin1", "");
                String country = item.optString("country", "");

                if (!admin1.isEmpty())
                    return name + ", " + admin1 + ", " + country;
                else
                    return name + ", " + country;
            }

        } catch (Exception e) {
            Log.e("ReverseGeocode", "Error: " + e.getMessage());
        }

        return "Unknown Location";
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
                    String geoUrl = "https://geocoding-api.open-meteo.com/v1/search?name="
                            + URLEncoder.encode(searchQuery, "UTF-8")
                            + "&count=20&language=" + Locale.getDefault().getLanguage() + "&format=json";

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
package com.freetime.geoweather;

import static com.freetime.geoweather.RadarTileSource.addRadarLayer;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.freetime.geoweather.data.LocationDatabase;
import com.freetime.geoweather.data.LocationEntity;
import com.freetime.geoweather.ui.DailyAdapter;
import com.freetime.geoweather.ui.HourlyAdapter;

import org.json.JSONArray;
import org.json.JSONObject;
import org.maplibre.android.camera.CameraUpdateFactory;
import org.maplibre.android.geometry.LatLng;
import org.maplibre.android.maps.MapView;
import org.maplibre.android.maps.MapLibreMap;
import org.maplibre.android.maps.Style;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Locale;

public class WeatherDetailActivity extends AppCompatActivity {

    private TextView txtLocationDetail, txtTemperatureDetail, txtDescriptionDetail,
            txtWindDetail, txtHumidityDetail, txtSunrise, txtSunset;

    private ImageView imgWeatherIcon;
    private ImageButton btnBack;

    private RecyclerView rvHourly, rvDaily;

    private HourlyAdapter hourlyAdapter;
    private DailyAdapter dailyAdapter;

    // MAP
    private MapView mapView;
    private MapLibreMap mapLibreMap;

    private LocationEntity locationEntity;

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

        MapboxMapInitializer.init(this); // wichtig für MapLibre

        setContentView(R.layout.activity_weather_detail);

        btnBack = findViewById(R.id.btnBack);
        txtLocationDetail = findViewById(R.id.txtLocationDetail);
        txtTemperatureDetail = findViewById(R.id.txtTemperatureDetail);
        txtDescriptionDetail = findViewById(R.id.txtDescriptionDetail);
        txtWindDetail = findViewById(R.id.txtWindDetail);
        txtHumidityDetail = findViewById(R.id.txtHumidityDetail);
        txtSunrise = findViewById(R.id.txtSunrise);
        txtSunset = findViewById(R.id.txtSunset);
        imgWeatherIcon = findViewById(R.id.imgWeatherIcon);

        rvHourly = findViewById(R.id.rvForecastHourly);
        rvDaily = findViewById(R.id.rvForecastDaily);

        rvHourly.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        rvDaily.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));

        hourlyAdapter = new HourlyAdapter();
        dailyAdapter = new DailyAdapter();

        rvHourly.setAdapter(hourlyAdapter);
        rvDaily.setAdapter(dailyAdapter);

        rvDaily.setNestedScrollingEnabled(false);
        rvDaily.setHasFixedSize(false);
        rvHourly.setNestedScrollingEnabled(false);
        rvHourly.setHasFixedSize(false);

        btnBack.setOnClickListener(v -> finish());

        String name = getIntent().getStringExtra("location_name");
        double lat = getIntent().getDoubleExtra("latitude", 0);
        double lon = getIntent().getDoubleExtra("longitude", 0);

        if (name == null || name.trim().isEmpty()) {
            name = "Unknown Location";
        }

        if (lat == 0.0 && lon == 0.0) {
            Toast.makeText(this, "Invalid location coordinates", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        txtLocationDetail.setText(name);

        mapView = findViewById(R.id.mapView);
        if (mapView != null) {
            try {
                mapView.onCreate(savedInstanceState);
                mapView.getMapAsync(map -> {
                    mapLibreMap = map;
                    mapLibreMap.setStyle(new Style.Builder().fromUri("asset://map_style.json"), style -> {
                        try {
                            mapLibreMap.animateCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(lat, lon), 8));
                            addRadarLayer(style);
                        } catch (Exception ignored) {
                        }
                    });
                });
            } catch (Exception e) {
                Log.e("WeatherDetail", "Map init failed", e);
            }
        }

        loadWeather(lat, lon);
    }

    private void loadWeather(double lat, double lon) {
        LocationDatabase.databaseWriteExecutor.execute(() -> {
            locationEntity = LocationDatabase.getDatabase(getApplicationContext()).locationDao().findByCoordinates(lat, lon);

            runOnUiThread(() -> {
                if (locationEntity != null && locationEntity.getWeatherData() != null && !isCacheExpired(locationEntity.getLastUpdated())) {
                    parseAndDisplay(locationEntity.getWeatherData());
                    Toast.makeText(WeatherDetailActivity.this, "Loaded from cache", Toast.LENGTH_SHORT).show();
                } else if (isNetworkAvailable()) {
                    fetchWeather(lat, lon);
                } else {
                    Toast.makeText(WeatherDetailActivity.this, "No internet connection and no cache available", Toast.LENGTH_LONG).show();
                }
            });
        });
    }

    private void fetchWeather(double lat, double lon) {
        new Thread(() -> {
            try {
                String url =
                        "https://api.open-meteo.com/v1/forecast"
                                + "?latitude=" + lat
                                + "&longitude=" + lon
                                + "&current_weather=true"
                                + "&daily=temperature_2m_max,temperature_2m_min,weathercode,sunrise,sunset"
                                + "&hourly=temperature_2m,weathercode"
                                + "&timezone=auto";

                String json = httpGet(url);

                LocationDatabase.databaseWriteExecutor.execute(() -> {
                    if (locationEntity != null) {
                        locationEntity.setWeatherData(json);
                        locationEntity.setLastUpdated(System.currentTimeMillis());
                        LocationDatabase.getDatabase(getApplicationContext()).locationDao().updateLocation(locationEntity);
                    }
                });

                runOnUiThread(() -> parseAndDisplay(json));

            } catch (Exception e) {
                runOnUiThread(() ->
                        Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show()
                );
            }
        }).start();
    }

    private void parseAndDisplay(String json) {
        try {
            JSONObject root = new JSONObject(json);
            JSONObject current = root.getJSONObject("current_weather");

            double temp = current.getDouble("temperature");
            double wind = current.getDouble("windspeed");
            int code = current.getInt("weathercode");

            txtTemperatureDetail.setText(String.format(Locale.getDefault(), "%.1f°C", temp));
            txtWindDetail.setText("Wind: " + wind + " km/h");
            txtHumidityDetail.setText("Humidity: —");

            txtDescriptionDetail.setText(WeatherCodes.getDescription(code));
            imgWeatherIcon.setImageResource(WeatherIconMapper.getWeatherIcon(code));

            JSONObject daily = root.getJSONObject("daily");
            parseDaily(daily);

            JSONObject hourly = root.getJSONObject("hourly");
            parseHourly(hourly);

            String sunrise = daily.getJSONArray("sunrise").getString(0);
            String sunset = daily.getJSONArray("sunset").getString(0);

            txtSunrise.setText("Sunrise: " + sunrise.replace("T", " "));
            txtSunset.setText("Sunset: " + sunset.replace("T", " "));

            WeatherIconMapper.setSunTimes(sunrise, sunset);

        } catch (Exception e) {
            Log.e("WeatherDetail", "parse error", e);
        }
    }

    private void parseDaily(JSONObject daily) {
        try {
            JSONArray dates = daily.getJSONArray("time");
            JSONArray max = daily.getJSONArray("temperature_2m_max");
            JSONArray min = daily.getJSONArray("temperature_2m_min");
            JSONArray codes = daily.getJSONArray("weathercode");

            ArrayList<DailyAdapter.DailyForecast> list = new ArrayList<>();

            for (int i = 0; i < dates.length(); i++) {
                DailyAdapter.DailyForecast f = new DailyAdapter.DailyForecast();
                f.date = dates.getString(i);
                f.tempMax = max.getDouble(i);
                f.tempMin = min.getDouble(i);
                f.weatherCode = codes.getInt(i);
                list.add(f);
            }

            dailyAdapter.setItems(list);

        } catch (Exception e) {
            Log.e("WeatherDetail", "daily parse error", e);
        }
    }

    private void parseHourly(JSONObject hourly) {
        try {
            JSONArray times = hourly.getJSONArray("time");
            JSONArray temps = hourly.getJSONArray("temperature_2m");
            JSONArray codes = hourly.getJSONArray("weathercode");

            ArrayList<HourlyAdapter.HourlyForecast> list = new ArrayList<>();

            for (int i = 0; i < times.length(); i++) {
                HourlyAdapter.HourlyForecast h = new HourlyAdapter.HourlyForecast();
                h.time = times.getString(i);
                h.temperature = temps.getDouble(i);
                h.weatherCode = codes.getInt(i);
                list.add(h);
            }

            hourlyAdapter.setItems(list);

        } catch (Exception e) {
            Log.e("WeatherDetail", "hourly parse error", e);
        }
    }

    private String httpGet(String urlString) throws Exception {
        URL url = new URL(urlString);
        HttpURLConnection c = (HttpURLConnection) url.openConnection();
        c.setRequestMethod("GET");
        c.setConnectTimeout(12000);
        c.setReadTimeout(12000);
        c.connect();

        InputStream stream = c.getInputStream();
        BufferedReader in = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8));

        StringBuilder sb = new StringBuilder();
        String line;

        while ((line = in.readLine()) != null) sb.append(line);

        c.disconnect();
        return sb.toString();
    }

    private boolean isNetworkAvailable() {
        ConnectivityManager connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        return activeNetworkInfo != null && activeNetworkInfo.isConnected();
    }

    private boolean isCacheExpired(long lastUpdated) {
        // Cache expires after 1 hour
        return (System.currentTimeMillis() - lastUpdated) > 3600 * 1000;
    }

    // MAP LIFECYCLE
    @Override protected void onStart() { super.onStart(); if(mapView != null) mapView.onStart(); }
    @Override protected void onResume() { super.onResume(); if(mapView != null) mapView.onResume(); }
    @Override protected void onPause() { super.onPause(); if(mapView != null) mapView.onPause(); }
    @Override protected void onStop() { super.onStop(); if(mapView != null) mapView.onStop(); }
    @Override protected void onDestroy() { super.onDestroy(); if(mapView != null) mapView.onDestroy(); }
    @Override public void onLowMemory() { super.onLowMemory(); if(mapView != null) mapView.onLowMemory(); }
}

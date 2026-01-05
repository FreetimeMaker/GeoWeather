package com.freetime.geoweather;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class WeatherDetailActivity extends AppCompatActivity {

    private TextView txtLocationDetail, txtTemperatureDetail, txtDescriptionDetail, txtWindDetail, txtHumidityDetail;
    private ImageButton btnBack;
    private LinearLayout forecastLayout;

    private static final Map<Integer, String> WEATHER_CODES = createWeatherCodes();

    private static Map<Integer, String> createWeatherCodes() {
        HashMap<Integer, String> m = new HashMap<>();
        m.put(0, "Clear sky");
        m.put(1, "Mostly clear");
        m.put(2, "Partly cloudy");
        m.put(3, "Overcast");
        m.put(45, "Fog");
        m.put(48, "Depositing rime fog");
        m.put(51, "Light drizzle");
        m.put(53, "Moderate drizzle");
        m.put(55, "Dense drizzle");
        m.put(61, "Slight rain");
        m.put(63, "Moderate rain");
        m.put(65, "Heavy rain");
        m.put(71, "Slight snow");
        m.put(73, "Moderate snow");
        m.put(75, "Heavy snow");
        return Collections.unmodifiableMap(m);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // Fullscreen-Modus aktivieren
        hideSystemUI();
        
        setContentView(R.layout.activity_weather_detail);

        btnBack = findViewById(R.id.btnBack);
        txtLocationDetail = findViewById(R.id.txtLocationDetail);
        txtTemperatureDetail = findViewById(R.id.txtTemperatureDetail);
        txtDescriptionDetail = findViewById(R.id.txtDescriptionDetail);
        txtWindDetail = findViewById(R.id.txtWindDetail);
        txtHumidityDetail = findViewById(R.id.txtHumidityDetail);
        forecastLayout = findViewById(R.id.forecastLayout);

        btnBack.setOnClickListener(v -> finish());

        double lat = getIntent().getDoubleExtra("latitude", 0);
        double lon = getIntent().getDoubleExtra("longitude", 0);
        String name = getIntent().getStringExtra("name");

        if (name != null && !name.isEmpty()) {
            fetchWeather(lat, lon, name);
        } else {
            Toast.makeText(this, "Location data not available", Toast.LENGTH_SHORT).show();
        }
    }

    private void fetchWeather(double latitude, double longitude, String locationName) {
        new Thread(() -> {
            try {
                String weatherUrl = "https://api.open-meteo.com/v1/forecast?latitude="
                        + latitude + "&longitude=" + longitude + "&current_weather=true&timezone=auto&daily=weathercode,temperature_2m_max,temperature_2m_min";

                final String weatherJson = httpGet(weatherUrl, "GeoWeatherApp");

                runOnUiThread(() -> parseAndDisplayOpenMeteo(weatherJson, locationName));

            } catch (final Exception e) {
                Log.e("WeatherDetailActivity", "fetchWeather failed", e);
                runOnUiThread(() ->
                        Toast.makeText(WeatherDetailActivity.this, "Error fetching weather: " + e.getMessage(), Toast.LENGTH_SHORT).show()
                );
            }
        }).start();
    }

    private void parseAndDisplayOpenMeteo(String json, String locationLabel) {
        try {
            if (json == null || json.isEmpty()) {
                throw new Exception("Empty JSON response");
            }

            JSONObject root = new JSONObject(json);
            if (!root.has("current_weather")) {
                throw new Exception("No current_weather in response");
            }

            JSONObject current = root.getJSONObject("current_weather");

            double temp = current.getDouble("temperature");
            double windSpeed = current.getDouble("windspeed");
            int weatherCode = current.getInt("weathercode");

            if (txtLocationDetail != null) txtLocationDetail.setText(locationLabel);
            if (txtTemperatureDetail != null) txtTemperatureDetail.setText(String.format(Locale.getDefault(), "%.1f°C", temp));
            if (txtDescriptionDetail != null) txtDescriptionDetail.setText(WEATHER_CODES.getOrDefault(weatherCode, "Unknown"));
            if (txtWindDetail != null) txtWindDetail.setText(String.format(Locale.getDefault(), "Wind: %.1f km/h", windSpeed));

            // Parse and display 5-day forecast
            if (root.has("daily")) {
                JSONObject daily = root.getJSONObject("daily");
                JSONArray time = daily.getJSONArray("time");
                JSONArray weathercode = daily.getJSONArray("weathercode");
                JSONArray tempMax = daily.getJSONArray("temperature_2m_max");
                JSONArray tempMin = daily.getJSONArray("temperature_2m_min");

                forecastLayout.removeAllViews(); // Clear previous forecast views

                for (int i = 1; i < time.length() && i <= 5; i++) { // Start from 1 for next days
                    String date = time.getString(i);
                    String code = WEATHER_CODES.getOrDefault(weathercode.getInt(i), "N/A");
                    String max = String.format(Locale.getDefault(), "%.1f°C", tempMax.getDouble(i));
                    String min = String.format(Locale.getDefault(), "%.1f°C", tempMin.getDouble(i));

                    LayoutInflater inflater = LayoutInflater.from(this);
                    View forecastItemView = inflater.inflate(R.layout.forecast_item, forecastLayout, false);

                    TextView tvDate = forecastItemView.findViewById(R.id.tvDate);
                    TextView tvDescription = forecastItemView.findViewById(R.id.tvDescription);
                    TextView tvTemp = forecastItemView.findViewById(R.id.tvTemp);

                    tvDate.setText(date);
                    tvDescription.setText(code);
                    tvTemp.setText(String.format("%s / %s", max, min));

                    forecastLayout.addView(forecastItemView);
                }
            }

        } catch (Exception e) {
            Log.e("WeatherDetailActivity", "JSON parsing failed", e);
            Toast.makeText(this, "Error parsing weather data: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private String httpGet(String urlString, String userAgent) throws Exception {
        URL url = new URL(urlString);
        HttpURLConnection c = (HttpURLConnection) url.openConnection();
        c.setRequestMethod("GET");
        c.setRequestProperty("User-Agent", userAgent);
        c.setConnectTimeout(5000);
        c.setReadTimeout(10000);

        int code = c.getResponseCode();
        InputStream stream = (code >= 200 && code < 400) ? c.getInputStream() : c.getErrorStream();

        try (BufferedReader in = new BufferedReader(
                new InputStreamReader(stream, StandardCharsets.UTF_8))) {

            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = in.readLine()) != null) {
                sb.append(line);
            }
            return sb.toString();
        } finally {
            c.disconnect();
        }
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
}
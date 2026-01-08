package com.freetime.geoweather;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
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

    private TextView txtLocationDetail, txtTemperatureDetail, txtDescriptionDetail,
            txtWindDetail, txtHumidityDetail, txtForecastDetail;
    private ImageButton btnBack;

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
        hideSystemUI();
        setContentView(R.layout.activity_weather_detail);

        btnBack = findViewById(R.id.btnBack);
        txtLocationDetail = findViewById(R.id.txtLocationDetail);
        txtTemperatureDetail = findViewById(R.id.txtTemperatureDetail);
        txtDescriptionDetail = findViewById(R.id.txtDescriptionDetail);
        txtWindDetail = findViewById(R.id.txtWindDetail);
        txtHumidityDetail = findViewById(R.id.txtHumidityDetail);

        // NEW: Forecast TextView
        txtForecastDetail = findViewById(R.id.txtForecastDetail);

        if (btnBack != null) {
            btnBack.setOnClickListener(v -> finish());
        }

        String locationName = getIntent().getStringExtra("location_name");
        double latitude = getIntent().getDoubleExtra("latitude", 0.0);
        double longitude = getIntent().getDoubleExtra("longitude", 0.0);

        if (locationName == null || locationName.isEmpty()) {
            Toast.makeText(this, "Invalid City", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        if (latitude == 0.0 && longitude == 0.0) {
            Toast.makeText(this, "Invalid Coordinates", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        txtLocationDetail.setText(locationName);

        fetchWeather(latitude, longitude, locationName);
    }

    private void fetchWeather(double latitude, double longitude, String locationName) {
        new Thread(() -> {
            try {
                String weatherUrl =
                        "https://api.open-meteo.com/v1/forecast?latitude=" + latitude +
                        "&longitude=" + longitude +
                        "&current_weather=true" +
                        "&daily=temperature_2m_max,temperature_2m_min,weathercode" +
                        "&timezone=auto";

                final String weatherJson = httpGet(weatherUrl, "GeoWeatherApp");

                runOnUiThread(() -> parseAndDisplayOpenMeteo(weatherJson, locationName));

            } catch (final Exception e) {
                Log.e("WeatherDetailActivity", "fetchWeather failed", e);
                runOnUiThread(() ->
                        Toast.makeText(WeatherDetailActivity.this,
                                "Error fetching weather: " + e.getMessage(),
                                Toast.LENGTH_SHORT).show()
                );
            }
        }).start();
    }

    private void parseAndDisplayOpenMeteo(String json, String locationLabel) {
        try {
            JSONObject root = new JSONObject(json);

            // --- CURRENT WEATHER ---
            JSONObject obj = root.getJSONObject("current_weather");

            double temp = obj.getDouble("temperature");
            double windSpeed = obj.getDouble("windspeed");
            int weatherCode = obj.getInt("weathercode");

            txtLocationDetail.setText(locationLabel);
            txtTemperatureDetail.setText(String.format(Locale.getDefault(), "%.1f°C", temp));
            txtWindDetail.setText(String.format(Locale.getDefault(), "Wind: %.1f km/h", windSpeed));
            txtHumidityDetail.setText("Humidity: —");
            txtDescriptionDetail.setText(WEATHER_CODES.getOrDefault(weatherCode, "Unknown"));

            // --- FORECAST ---
            parseForecast(root);

        } catch (Exception e) {
            Log.e("WeatherDetailActivity", "parse error", e);
            Toast.makeText(this, "Error reading weather data: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void parseForecast(JSONObject root) {
        try {
            if (!root.has("daily")) {
                txtForecastDetail.setText("No forecast available");
                return;
            }

            JSONObject daily = root.getJSONObject("daily");

            JSONArray dates = daily.getJSONArray("time");
            JSONArray max = daily.getJSONArray("temperature_2m_max");
            JSONArray min = daily.getJSONArray("temperature_2m_min");
            JSONArray codes = daily.getJSONArray("weathercode");

            StringBuilder sb = new StringBuilder();
            sb.append("Forecast:\n\n");

            for (int i = 0; i < dates.length(); i++) {
                String date = dates.getString(i);
                double tMax = max.getDouble(i);
                double tMin = min.getDouble(i);
                int code = codes.getInt(i);

                sb.append(date)
                        .append(" → ")
                        .append(String.format(Locale.getDefault(), "%.1f° / %.1f°", tMin, tMax))
                        .append("  ")
                        .append(WEATHER_CODES.getOrDefault(code, "Unknown"))
                        .append("\n");
            }

            txtForecastDetail.setText(sb.toString());

        } catch (Exception e) {
            Log.e("WeatherDetailActivity", "Forecast parse error", e);
            txtForecastDetail.setText("Error loading forecast");
        }
    }

    private String httpGet(String urlString, String userAgent) throws Exception {
        URL url = new URL(urlString);
        HttpURLConnection c = (HttpURLConnection) url.openConnection();
        c.setRequestMethod("GET");
        c.setRequestProperty("User-Agent", userAgent);
        c.setConnectTimeout(12000);
        c.setReadTimeout(12000);
        c.connect();

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
        if (hasFocus) hideSystemUI();
    }
}

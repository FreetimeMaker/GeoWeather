package com.freetime.geoweather;

import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

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
        setContentView(R.layout.activity_weather_detail);

        txtLocationDetail = findViewById(R.id.txtLocationDetail);
        txtTemperatureDetail = findViewById(R.id.txtTemperatureDetail);
        txtDescriptionDetail = findViewById(R.id.txtDescriptionDetail);
        txtWindDetail = findViewById(R.id.txtWindDetail);
        txtHumidityDetail = findViewById(R.id.txtHumidityDetail);

        String locationName = getIntent().getStringExtra("location_name");
        double latitude = getIntent().getDoubleExtra("latitude", 0.0);
        double longitude = getIntent().getDoubleExtra("longitude", 0.0);

        txtLocationDetail.setText(locationName);

        fetchWeather(latitude, longitude, locationName);
    }

    private void fetchWeather(double latitude, double longitude, String locationName) {
        new Thread(() -> {
            try {
                String weatherUrl = "https://api.open-meteo.com/v1/forecast?latitude="
                        + latitude + "&longitude=" + longitude + "&current_weather=true&timezone=auto";

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
            JSONObject root = new JSONObject(json);
            JSONObject obj = root.getJSONObject("current_weather");

            double temp = obj.getDouble("temperature");
            double windSpeed = obj.getDouble("windspeed");
            int weatherCode = obj.getInt("weathercode");

            txtLocationDetail.setText(locationLabel);
            txtTemperatureDetail.setText(String.format(Locale.getDefault(), "%.1f°C", temp));
            txtWindDetail.setText(String.format(Locale.getDefault(), "Wind: %.1f km/h", windSpeed));
            txtHumidityDetail.setText("Humidity: —"); // Humidity not in current_weather; needs extra param
            txtDescriptionDetail.setText(WEATHER_CODES.getOrDefault(weatherCode, "Unknown"));

        } catch (Exception e) {
            Log.e("WeatherDetailActivity", "parseAndDisplayOpenMeteo failed", e);
            Toast.makeText(this, "Error reading weather data", Toast.LENGTH_SHORT).show();
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
}
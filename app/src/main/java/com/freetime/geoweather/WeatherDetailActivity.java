package com.freetime.geoweather;

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

import com.freetime.geoweather.ui.ForecastAdapter;
import com.freetime.geoweather.ui.HourlyAdapter;
import com.freetime.geoweather.ui.DailyAdapter;

import org.json.JSONArray;
import org.json.JSONObject;

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

    private RecyclerView rvForecastDaily;
    private RecyclerView rvForecastHourly;

    private ForecastAdapter forecastAdapter;
    private HourlyAdapter hourlyAdapter;
    private DailyAdapter dailyAdapter;

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
        txtSunrise = findViewById(R.id.txtSunrise);
        txtSunset = findViewById(R.id.txtSunset);
        imgWeatherIcon = findViewById(R.id.imgWeatherIcon);

        rvForecastDaily = findViewById(R.id.rvForecastDaily);
        rvForecastHourly = findViewById(R.id.rvForecastHourly);

        rvForecastDaily.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        rvForecastHourly.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));

        forecastAdapter = new ForecastAdapter();
        dailyAdapter = new DailyAdapter();
        hourlyAdapter = new HourlyAdapter();

        rvForecastDaily.setAdapter(dailyAdapter);
        rvForecastHourly.setAdapter(hourlyAdapter);

        btnBack.setOnClickListener(v -> finish());

        String locationName = getIntent().getStringExtra("location_name");
        double latitude = getIntent().getDoubleExtra("latitude", 0.0);
        double longitude = getIntent().getDoubleExtra("longitude", 0.0);

        txtLocationDetail.setText(locationName);

        fetchWeather(latitude, longitude);
    }

    private void fetchWeather(double latitude, double longitude) {
        new Thread(() -> {
            try {
                String weatherUrl =
                        "https://api.open-meteo.com/v1/forecast"
                                + "?latitude=" + latitude
                                + "&longitude=" + longitude
                                + "&current_weather=true"
                                + "&daily=temperature_2m_max,temperature_2m_min,weathercode,sunrise,sunset"
                                + "&hourly=temperature_2m,weathercode"
                                + "&timezone=auto";

                final String weatherJson = httpGet(weatherUrl, "GeoWeatherApp");

                runOnUiThread(() -> parseAndDisplay(weatherJson));

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

    private void parseAndDisplay(String json) {
        try {
            JSONObject root = new JSONObject(json);

            // CURRENT WEATHER
            JSONObject current = root.getJSONObject("current_weather");

            double temp = current.getDouble("temperature");
            double windSpeed = current.getDouble("windspeed");
            int weatherCode = current.getInt("weathercode");

            txtTemperatureDetail.setText(String.format(Locale.getDefault(), "%.1f°C", temp));
            txtWindDetail.setText(String.format(Locale.getDefault(), "Wind: %.1f km/h", windSpeed));
            txtHumidityDetail.setText("Humidity: —");

            txtDescriptionDetail.setText(
                    WeatherCodes.getDescription(weatherCode)
            );

            imgWeatherIcon.setImageResource(
                    WeatherIconMapper.getWeatherIcon(weatherCode)
            );

            // DAILY FORECAST
            JSONObject daily = root.getJSONObject("daily");
            parseDaily(daily);

            // HOURLY FORECAST
            JSONObject hourly = root.getJSONObject("hourly");
            parseHourly(hourly);

            // SUNRISE / SUNSET
            String sunrise = daily.getJSONArray("sunrise").getString(0);
            String sunset = daily.getJSONArray("sunset").getString(0);

            txtSunrise.setText("Sunrise: " + sunrise.replace("T", " "));
            txtSunset.setText("Sunset: " + sunset.replace("T", " "));

            WeatherIconMapper.setSunTimes(sunrise, sunset);

        } catch (Exception e) {
            Log.e("WeatherDetailActivity", "parse error", e);
            Toast.makeText(this, "Error reading weather data: " + e.getMessage(), Toast.LENGTH_SHORT).show();
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
            Log.e("WeatherDetailActivity", "Daily parse error", e);
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
            Log.e("WeatherDetailActivity", "Hourly parse error", e);
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

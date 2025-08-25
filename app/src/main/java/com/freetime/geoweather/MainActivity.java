package com.freetime.geoweather;

import androidx.appcompat.app.AppCompatActivity;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.View;
import android.widget.*;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;

public class MainActivity extends AppCompatActivity {

    EditText editCity;
    Button btnGetWeather;
    TextView txtResult;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        editCity = findViewById(R.id.editCity);
        btnGetWeather = findViewById(R.id.btnGetWeather);
        txtResult = findViewById(R.id.txtResult);

        btnGetWeather.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String city = editCity.getText().toString().trim();
                if (!city.isEmpty()) {
                    new GetCoordinatesTask().execute(city);
                } else {
                    txtResult.setText("Bitte einen Ort eingeben");
                }
            }
        });
    }

    // 1. Schritt: Koordinaten über Geocoding API abrufen
    private class GetCoordinatesTask extends AsyncTask<String, Void, double[]> {
        @Override
        protected double[] doInBackground(String... params) {
            String city = params[0];
            String urlString = "https://geocoding-api.open-meteo.com/v1/search?name=" + city;
            try {
                String json = downloadUrl(urlString);
                JSONObject obj = new JSONObject(json);
                JSONArray results = obj.getJSONArray("results");
                if (results.length() > 0) {
                    JSONObject first = results.getJSONObject(0);
                    double lat = first.getDouble("latitude");
                    double lon = first.getDouble("longitude");
                    return new double[]{lat, lon};
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            return null;
        }

        @Override
        protected void onPostExecute(double[] coords) {
            if (coords != null) {
                new GetWeatherTask().execute(coords[0], coords[1]);
            } else {
                txtResult.setText("Ort nicht gefunden.");
            }
        }
    }

    // 2. Schritt: Wetterdaten für Koordinaten abrufen
    private class GetWeatherTask extends AsyncTask<Double, Void, String> {
        @Override
        protected String doInBackground(Double... params) {
            double lat = params[0];
            double lon = params[1];
            String urlString = "https://api.open-meteo.com/v1/forecast?latitude=" + lat + "&longitude=" + lon + "&current_weather=true";
            try {
                String json = downloadUrl(urlString);
                JSONObject obj = new JSONObject(json);
                JSONObject current = obj.getJSONObject("current_weather");
                double temp = current.getDouble("temperature");
                double wind = current.getDouble("windspeed");
                return "Temperatur: " + temp + "°C\nWind: " + wind + " km/h";
            } catch (Exception e) {
                e.printStackTrace();
                return "Fehler beim Abrufen der Wetterdaten.";
            }
        }

        @Override
        protected void onPostExecute(String result) {
            txtResult.setText(result);
        }
    }

    // Hilfsmethode für HTTP-Request
    private String downloadUrl(String urlString) throws IOException {
        URL url = new URL(urlString);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setReadTimeout(10000);
        conn.setConnectTimeout(15000);
        conn.setRequestMethod("GET");
        conn.connect();
        InputStream in = conn.getInputStream();
        BufferedReader reader = new BufferedReader(new InputStreamReader(in));
        StringBuilder result = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) {
            result.append(line);
        }
        reader.close();
        return result.toString();
    }
}

package com.freetime.geoweather

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.action.ActionParameters
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.freetime.geoweather.data.LocationDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.nio.charset.StandardCharsets

class WeatherWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val db = LocationDatabase.getDatabase(context)
        val sharedPreferences = context.getSharedPreferences("geo_weather_prefs", Context.MODE_PRIVATE)
        
        val location = withContext(Dispatchers.IO) {
            db.locationDao().getSelectedLocation()
        }
        
        val tempUnit = sharedPreferences.getString("temp_unit", "celsius") ?: "celsius"
        val weatherProvider = sharedPreferences.getString("weather_provider", "open_meteo") ?: "open_meteo"
        val weatherApiKey = sharedPreferences.getString("weather_api_key", "") ?: ""

        var weatherInfo = context.getString(R.string.widget_loading)
        var tempString = ""
        var locationName = location?.name ?: context.getString(R.string.no_location_selected)

        if (location != null) {
            try {
                val url = if (weatherProvider == "weatherapi" && weatherApiKey.isNotEmpty()) {
                    "https://api.weatherapi.com/v1/current.json?key=$weatherApiKey&q=${location.latitude},${location.longitude}"
                } else {
                    "https://api.open-meteo.com/v1/forecast?latitude=${location.latitude}&longitude=${location.longitude}&current_weather=true&timezone=auto"
                }

                val response = withContext(Dispatchers.IO) {
                    httpGet(url)
                }
                val json = JSONObject(response)
                
                val (t, code) = if (weatherProvider == "weatherapi") {
                    val current = json.getJSONObject("current")
                    current.getDouble("temp_c") to 0
                } else {
                    val current = json.getJSONObject("current_weather")
                    current.getDouble("temperature") to current.getInt("weathercode")
                }
                
                val displayTemp = if (tempUnit == "fahrenheit") (t * 9/5 + 32).toInt() else t.toInt()
                val tempSuffix = if (tempUnit == "fahrenheit") "°F" else "°C"
                
                tempString = "$displayTemp$tempSuffix"
                weatherInfo = WeatherCodes.getDescription(code, context)
            } catch (e: Exception) {
                weatherInfo = context.getString(R.string.error_connection)
            }
        } else {
            weatherInfo = context.getString(R.string.select_city_msg)
        }

        provideContent {
            WeatherWidgetContent(locationName, tempString, weatherInfo)
        }
    }

    private fun httpGet(urlString: String): String {
        val url = URL(urlString)
        val c = url.openConnection() as HttpURLConnection
        c.setRequestProperty("User-Agent", "GeoWeatherApp")
        c.connectTimeout = 10000
        c.readTimeout = 10000
        BufferedReader(InputStreamReader(c.inputStream, StandardCharsets.UTF_8)).use { reader ->
            val sb = StringBuilder()
            var line: String?
            while (reader.readLine().also { line = it } != null) sb.append(line)
            return sb.toString()
        }
    }

    @Composable
    private fun WeatherWidgetContent(name: String, temp: String, info: String) {
        Row(
            modifier = GlanceModifier
                .fillMaxSize()
                .background(ColorProvider(Color(0xFF2196F3)))
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Column(
                modifier = GlanceModifier.defaultWeight(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalAlignment = Alignment.Start
            ) {
                Text(
                    text = name,
                    maxLines = 1,
                    style = TextStyle(
                        color = ColorProvider(Color.White),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                )
                Spacer(GlanceModifier.size(2.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (temp.isNotEmpty()) {
                        Text(
                            text = temp,
                            style = TextStyle(
                                color = ColorProvider(Color.White),
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold
                            )
                        )
                        Spacer(GlanceModifier.width(4.dp))
                    }
                    Text(
                        text = info,
                        maxLines = 1,
                        style = TextStyle(
                            color = ColorProvider(Color.White.copy(alpha = 0.8f)),
                            fontSize = 12.sp
                        )
                    )
                }
            }
            
            // Refresh Button
            Image(
                provider = ImageProvider(android.R.drawable.ic_menu_rotate),
                contentDescription = "Refresh",
                modifier = GlanceModifier
                    .size(24.dp)
                    .clickable(actionRunCallback<RefreshActionCallback>())
            )
        }
    }
}

class RefreshActionCallback : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters
    ) {
        WeatherWidget().update(context, glanceId)
    }
}

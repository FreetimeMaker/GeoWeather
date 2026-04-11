package com.freetime.geoweather

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.appwidget.GlanceAppWidget
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
import org.json.JSONObject
import java.net.URL

class WeatherWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val db = LocationDatabase.getDatabase(context)
        val location = db.locationDao().getSelectedLocation()
        val sharedPreferences = context.getSharedPreferences("geo_weather_prefs", Context.MODE_PRIVATE)
        val tempUnit = sharedPreferences.getString("temp_unit", "celsius") ?: "celsius"

        var weatherInfo = context.getString(R.string.widget_loading)
        var temp = ""
        var locationName = location?.name ?: "No Location"

        if (location != null) {
            try {
                val url = "https://api.open-meteo.com/v1/forecast?latitude=${location.latitude}&longitude=${location.longitude}&current_weather=true&timezone=auto"
                val response = URL(url).readText()
                val json = JSONObject(response)
                val current = json.getJSONObject("current_weather")
                val t = current.getDouble("temperature")
                val code = current.getInt("weathercode")
                
                val displayTemp = if (tempUnit == "fahrenheit") (t * 9/5 + 32).toInt() else t.toInt()
                val tempSuffix = if (tempUnit == "fahrenheit") "°F" else "°C"
                
                temp = "$displayTemp$tempSuffix"
                weatherInfo = WeatherCodes.getDescription(code, context)
            } catch (e: Exception) {
                weatherInfo = "Error"
            }
        }

        provideContent {
            WeatherWidgetContent(locationName, temp, weatherInfo)
        }
    }

    @Composable
    private fun WeatherWidgetContent(name: String, temp: String, info: String) {
        Column(
            modifier = GlanceModifier
                .fillMaxSize()
                .background(ColorProvider(Color(0xFF2196F3)))
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = name,
                style = TextStyle(
                    color = ColorProvider(Color.White),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            )
            Spacer(GlanceModifier.size(4.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = temp,
                    style = TextStyle(
                        color = ColorProvider(Color.White),
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold
                    )
                )
                Spacer(GlanceModifier.width(8.dp))
                Text(
                    text = info,
                    style = TextStyle(
                        color = ColorProvider(Color.White.copy(alpha = 0.8f)),
                        fontSize = 14.sp
                    )
                )
            }
        }
    }
}

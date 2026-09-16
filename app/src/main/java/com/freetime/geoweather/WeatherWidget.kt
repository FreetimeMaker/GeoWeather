package com.freetime.geoweather

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.Button
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.LocalSize
import androidx.glance.action.ActionParameters
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxHeight
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.freetime.geoweather.R
import com.freetime.geoweather.data.LocationDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.nio.charset.StandardCharsets
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class WeatherWidget : GlanceAppWidget() {

    companion object {
        private val SMALL_RECT = DpSize(120.dp, 60.dp)
        private val MEDIUM_RECT = DpSize(240.dp, 60.dp)
        private val LARGE_RECT = DpSize(240.dp, 120.dp)
    }

    override val sizeMode = SizeMode.Responsive(
        setOf(SMALL_RECT, MEDIUM_RECT, LARGE_RECT)
    )

    data class HourlyForecast(val time: String, val temp: Int, val iconRes: Int)

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val db = LocationDatabase.getDatabase(context)
        val sharedPreferences = context.getSharedPreferences("geo_weather_prefs", Context.MODE_PRIVATE)
        
        val location = withContext(Dispatchers.IO) {
            db.locationDao().getSelectedLocation()
        }
        
        val tempUnit = sharedPreferences.getString("temp_unit", "celsius") ?: "celsius"

        var weatherInfo = context.getString(R.string.widget_loading)
        var tempString = ""
        var locationName = location?.name ?: context.getString(R.string.no_location_selected)
        var hourlyList = mutableListOf<HourlyForecast>()

        if (location != null) {
            try {
                val url = "https://api.open-meteo.com/v1/forecast?latitude=${location.latitude}&longitude=${location.longitude}&current=temperature_2m,weather_code&hourly=temperature_2m,weather_code&timezone=auto"

                val response = withContext(Dispatchers.IO) {
                    httpGet(url)
                }
                val json = JSONObject(response)
                
                // Current
                val current = json.getJSONObject("current")
                val code = current.getInt("weather_code")
                val t = current.getDouble("temperature_2m")
                
                val displayTemp = if (tempUnit == "fahrenheit") (t * 9/5 + 32).toInt() else t.toInt()
                val tempSuffix = if (tempUnit == "fahrenheit") "°F" else "°C"
                
                tempString = "$displayTemp$tempSuffix"
                weatherInfo = WeatherCodes.getDescription(code, context)

                // Hourly
                val hourly = json.getJSONObject("hourly")
                val times = hourly.getJSONArray("time")
                val temps = hourly.getJSONArray("temperature_2m")
                val codes = hourly.getJSONArray("weather_code")

                val sdfIn = SimpleDateFormat("yyyy-MM-dd'T'HH:mm", Locale.getDefault())
                val sdfOut = SimpleDateFormat("HH:mm", Locale.getDefault())
                val now = Calendar.getInstance()
                
                for (i in 0 until times.length()) {
                    val date = sdfIn.parse(times.getString(i)) ?: continue
                    if (date.after(now.time) && hourlyList.size < 5) {
                        val hTemp = temps.getDouble(i)
                        val hDisplayTemp = if (tempUnit == "fahrenheit") (hTemp * 9/5 + 32).toInt() else hTemp.toInt()
                        val hCode = codes.getInt(i)
                        
                        hourlyList.add(HourlyForecast(
                            time = sdfOut.format(date),
                            temp = hDisplayTemp,
                            iconRes = WeatherIconMapper.getWeatherIcon(hCode, "google")
                        ))
                    }
                }
            } catch (e: Exception) {
                weatherInfo = context.getString(R.string.error_connection)
            }
        } else {
            weatherInfo = context.getString(R.string.select_city_msg)
        }

        provideContent {
            val size = LocalSize.current
            WeatherWidgetContent(locationName, tempString, weatherInfo, hourlyList, size)
        }
    }

    @Composable
    private fun WeatherWidgetContent(name: String, temp: String, info: String, hourly: List<HourlyForecast>, size: DpSize) {
        val isExpanded = size.width >= 200.dp

        Column(
            modifier = GlanceModifier
                .fillMaxSize()
                .background(ColorProvider(androidx.compose.ui.graphics.Color(0xFF2196F3)))
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalAlignment = Alignment.Start
        ) {
            Row(
                modifier = GlanceModifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = GlanceModifier.defaultWeight()) {
                    Text(
                        text = name,
                        maxLines = 1,
                        style = TextStyle(
                            color = ColorProvider(androidx.compose.ui.graphics.Color.White),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (temp.isNotEmpty()) {
                            Text(
                                text = temp,
                                style = TextStyle(
                                    color = ColorProvider(androidx.compose.ui.graphics.Color.White),
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
                                color = ColorProvider(androidx.compose.ui.graphics.Color.White.copy(alpha = 0.8f)),
                                fontSize = 12.sp
                            )
                        )
                    }
                }
                
                Image(
                    provider = ImageProvider(android.R.drawable.ic_menu_rotate),
                    contentDescription = "Refresh",
                    modifier = GlanceModifier
                        .size(24.dp)
                        .clickable(actionRunCallback<RefreshActionCallback>())
                )
            }

            if (isExpanded && hourly.isNotEmpty()) {
                Spacer(GlanceModifier.height(8.dp))
                Row(
                    modifier = GlanceModifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    hourly.forEach { item ->
                        HourlyItem(item)
                        Spacer(GlanceModifier.width(8.dp))
                    }
                }
            }
        }
    }

    @Composable
    private fun HourlyItem(item: HourlyForecast) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = item.time,
                style = TextStyle(color = ColorProvider(androidx.compose.ui.graphics.Color.White.copy(alpha = 0.7f)), fontSize = 10.sp)
            )
            Image(
                provider = ImageProvider(item.iconRes),
                contentDescription = null,
                modifier = GlanceModifier.size(24.dp)
            )
            Text(
                text = "${item.temp}°",
                style = TextStyle(color = ColorProvider(androidx.compose.ui.graphics.Color.White), fontSize = 12.sp, fontWeight = FontWeight.Bold)
            )
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

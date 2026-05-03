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
import androidx.glance.color.ColorProvider
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
        val weatherRepository = WeatherRepository(context)
        
        val requireLogin = sharedPreferences.getBoolean("require_login", false)
        val authManager = AuthManager.getInstance(context)

        if (requireLogin && !authManager.isAuthenticated) {
            provideContent {
                WeatherWidgetContent(
                    context.getString(R.string.app_name),
                    "",
                    context.getString(R.string.login_required_widget)
                )
            }
            return
        }

        val location = withContext(Dispatchers.IO) {
            db.locationDao().getSelectedLocation()
        }
        
        val tempUnit = sharedPreferences.getString("temp_unit", "celsius") ?: "celsius"
        var weatherInfo = context.getString(R.string.widget_loading)
        var tempString = ""
        var locationName = location?.name ?: context.getString(R.string.no_location_selected)
        var iconRes: Int? = null

        if (location != null) {
            val result = weatherRepository.getWeatherData(location.latitude, location.longitude, 1)
            
            when (result) {
                is WeatherRepository.WeatherDataResult.Success -> {
                    val displayTemp = if (tempUnit == "fahrenheit") (result.temp * 9/5 + 32).toInt() else result.temp.toInt()
                    val tempSuffix = if (tempUnit == "fahrenheit") "°F" else "°C"
                    
                    tempString = "$displayTemp$tempSuffix"
                    weatherInfo = WeatherCodes.getDescription(result.weatherCode, context, result.provider)
                    iconRes = WeatherIconMapper.getIcon(result.weatherCode, result.provider, result.isDay)
                }
                is WeatherRepository.WeatherDataResult.Error -> {
                    weatherInfo = context.getString(R.string.error_connection)
                }
            }
        } else {
            weatherInfo = context.getString(R.string.select_city_msg)
        }

        provideContent {
            WeatherWidgetContent(locationName, tempString, weatherInfo, iconRes)
        }
    }

    @Composable
    private fun WeatherWidgetContent(name: String, temp: String, info: String, iconRes: Int? = null) {
        Row(
            modifier = GlanceModifier
                .fillMaxSize()
                .background(ColorProvider(Color(0xFFE3F2FD), Color(0xFF1A1C1E)))
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (iconRes != null) {
                Image(
                    provider = ImageProvider(iconRes),
                    contentDescription = null,
                    modifier = GlanceModifier.size(48.dp)
                )
                Spacer(GlanceModifier.width(8.dp))
            }

            Column(
                modifier = GlanceModifier.defaultWeight(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalAlignment = Alignment.Start
            ) {
                Text(
                    text = name,
                    maxLines = 1,
                    style = TextStyle(
                        color = ColorProvider(Color.Black, Color.White),
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
                                color = ColorProvider(Color.Black, Color.White),
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
                            color = ColorProvider(Color.DarkGray, Color.LightGray),
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

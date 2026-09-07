package com.freetime.geoweather

import android.R
import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
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
import com.freetime.geoweather.data.DependencyManager
import geoweather.shared.generated.resources.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jetbrains.compose.resources.getString

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
        val repository = DependencyManager.getRepository()
        val appSettings = DependencyManager.getAppSettings()
        
        val location = withContext(Dispatchers.IO) {
            repository.getSelectedLocation()
        }
        
        val tempUnit = appSettings.tempUnit.value

        var weatherInfo = getString(Res.string.widget_loading)
        var tempString = ""
        var locationName = location?.name ?: getString(Res.string.no_location_selected)
        var hourlyList = mutableListOf<HourlyForecast>()

        if (location != null) {
            try {
                val updatedLocation = repository.refreshSelectedLocationWeather(includeHourly = true) ?: location
                
                tempString = repository.getDisplayTemp(updatedLocation, tempUnit)
                weatherInfo = WeatherCodes.getDescription(updatedLocation.currentWeatherCode ?: 0)
            } catch (e: Exception) {
                weatherInfo = getString(Res.string.error_connection)
            }
        }

        val refreshDesc = getString(Res.string.refresh_nav_desc)

        provideContent {
            val size = LocalSize.current
            WeatherWidgetContent(locationName, tempString, weatherInfo, hourlyList, size, refreshDesc)
        }
    }

    @Composable
    private fun WeatherWidgetContent(name: String, temp: String, info: String, hourly: List<HourlyForecast>, size: DpSize, refreshDesc: String) {
        val isExpanded = size.width >= 200.dp

        Column(
            modifier = GlanceModifier
                .fillMaxSize()
                .background(ColorProvider(Color(0xFF2196F3)))
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
                            color = ColorProvider(Color.White),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                    )
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
                
                Image(
                    provider = ImageProvider(R.drawable.ic_menu_rotate),
                    contentDescription = refreshDesc,
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
                style = TextStyle(color = ColorProvider(Color.White.copy(alpha = 0.7f)), fontSize = 10.sp)
            )
            Image(
                provider = ImageProvider(item.iconRes),
                contentDescription = null,
                modifier = GlanceModifier.size(24.dp)
            )
            Text(
                text = "${item.temp}°",
                style = TextStyle(color = ColorProvider(Color.White), fontSize = 12.sp, fontWeight = FontWeight.Bold)
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

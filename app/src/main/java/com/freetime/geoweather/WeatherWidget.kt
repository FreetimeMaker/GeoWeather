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
import com.freetime.geoweather.R as Res
import com.freetime.geoweather.data.DailyForecast
import com.freetime.geoweather.data.DependencyManager
import com.freetime.geoweather.data.HourlyForecast
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

enum class WidgetVariant { RESPONSIVE, COMPACT, FORECAST, DETAILED }

open class WeatherWidget(
    private val variant: WidgetVariant = WidgetVariant.RESPONSIVE
) : GlanceAppWidget() {
    companion object {
        private val SMALL = DpSize(120.dp, 60.dp)
        private val MEDIUM = DpSize(240.dp, 100.dp)
        private val LARGE = DpSize(320.dp, 180.dp)
    }

    override val sizeMode: SizeMode = if (variant == WidgetVariant.RESPONSIVE) {
        SizeMode.Responsive(setOf(SMALL, MEDIUM, LARGE))
    } else {
        SizeMode.Exact
    }

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val repository = DependencyManager.getRepository()
        val settings = DependencyManager.getAppSettings()
        val location = withContext(Dispatchers.IO) { repository.getSelectedLocation() }
        val dataSaver = settings.dataSaver.value

        var current = location
        var cached = false
        if (location != null && !(dataSaver && location.weatherData != null)) {
            try {
                current = repository.refreshSelectedLocationWeather(includeHourly = true) ?: location
            } catch (_: Exception) {
                cached = location.weatherData != null
            }
        }

        val name = current?.name ?: context.getString(Res.string.no_location_selected)
        val temp = current?.let { repository.getDisplayTemp(it, settings.tempUnit.value) }.orEmpty()
        val code = current?.currentWeatherCode ?: 0
        val info = if (cached) {
            context.getString(Res.string.widget_cached, WeatherCodes.getDescription(code))
        } else {
            WeatherCodes.getDescription(code)
        }
        val hourly = current?.let { repository.getHourlyForecasts(it).take(5) }.orEmpty()
        val daily = current?.let { repository.getDailyForecasts(it).take(3) }.orEmpty()
        val refreshDesc = context.getString(Res.string.refresh_nav_desc)

        provideContent {
            WidgetContent(context, name, temp, code, info, hourly, daily, cached, dataSaver, LocalSize.current, refreshDesc)
        }
    }

    @Composable
    private fun WidgetContent(
        context: Context,
        name: String,
        temp: String,
        code: Int,
        info: String,
        hourly: List<HourlyForecast>,
        daily: List<DailyForecast>,
        cached: Boolean,
        dataSaver: Boolean,
        size: DpSize,
        refreshDesc: String
    ) {
        val expanded = variant == WidgetVariant.FORECAST || variant == WidgetVariant.DETAILED ||
            (variant == WidgetVariant.RESPONSIVE && size.width >= 200.dp)
        val detailed = variant == WidgetVariant.DETAILED ||
            (variant == WidgetVariant.RESPONSIVE && size.height >= 140.dp)

        Column(
            modifier = GlanceModifier.fillMaxSize()
                .background(ColorProvider(Color(0xFFE3F2FD))).padding(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(modifier = GlanceModifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Image(
                    provider = ImageProvider(widgetWeatherIcon(code)),
                    contentDescription = info,
                    modifier = GlanceModifier.size(if (detailed) 48.dp else 36.dp)
                )
                Spacer(GlanceModifier.width(8.dp))
                Column(modifier = GlanceModifier.defaultWeight()) {
                    Text(name, maxLines = 1, style = TextStyle(ColorProvider(Color(0xFF102A43)), 14.sp, FontWeight.Bold))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (temp.isNotEmpty()) {
                            Text(temp, style = TextStyle(ColorProvider(Color(0xFF102A43)), 18.sp, FontWeight.Bold))
                            Spacer(GlanceModifier.width(5.dp))
                        }
                        Text(info, maxLines = 1, style = TextStyle(ColorProvider(Color(0xFF334E68)), 11.sp))
                    }
                }
                Image(
                    provider = ImageProvider(R.drawable.ic_menu_rotate),
                    contentDescription = refreshDesc,
                    modifier = GlanceModifier.size(24.dp).clickable(refreshAction())
                )
            }

            if (cached) {
                Text(
                    if (dataSaver) context.getString(Res.string.widget_data_saver_saved)
                    else context.getString(Res.string.widget_offline_saved),
                    style = TextStyle(ColorProvider(Color(0xFF486581)), 10.sp)
                )
            }

            if (expanded && hourly.isNotEmpty()) {
                Spacer(GlanceModifier.height(8.dp))
                Row(modifier = GlanceModifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                    hourly.forEach { hour ->
                        Column(modifier = GlanceModifier.defaultWeight(), horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(hour.time.takeLast(5), style = TextStyle(ColorProvider(Color(0xFF486581)), 9.sp))
                            Image(ImageProvider(widgetWeatherIcon(hour.code)), null, GlanceModifier.size(24.dp))
                            Text("${hour.temp}°", style = TextStyle(ColorProvider(Color(0xFF102A43)), 11.sp, FontWeight.Bold))
                        }
                    }
                }
            }

            if (detailed && daily.isNotEmpty()) {
                Spacer(GlanceModifier.height(7.dp))
                Row(modifier = GlanceModifier.fillMaxWidth()) {
                    daily.forEach { day ->
                        Text(
                            day.date.takeLast(5) + "  " + day.minTemp + "°/" + day.maxTemp + "°",
                            modifier = GlanceModifier.defaultWeight(),
                            style = TextStyle(ColorProvider(Color(0xFF334E68)), 9.sp)
                        )
                    }
                }
            }
        }
    }

    private fun refreshAction() = when (variant) {
        WidgetVariant.RESPONSIVE -> actionRunCallback<RefreshActionCallback>()
        WidgetVariant.COMPACT -> actionRunCallback<RefreshCompactWidgetCallback>()
        WidgetVariant.FORECAST -> actionRunCallback<RefreshForecastWidgetCallback>()
        WidgetVariant.DETAILED -> actionRunCallback<RefreshDetailedWidgetCallback>()
    }
}

class CompactWeatherWidget : WeatherWidget(WidgetVariant.COMPACT)
class ForecastWeatherWidget : WeatherWidget(WidgetVariant.FORECAST)
class DetailedWeatherWidget : WeatherWidget(WidgetVariant.DETAILED)

private fun widgetWeatherIcon(code: Int): Int = WeatherIconMapper.getWeatherIcon(code)

class RefreshActionCallback : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        WeatherWidget().update(context, glanceId)
    }
}
class RefreshCompactWidgetCallback : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        CompactWeatherWidget().update(context, glanceId)
    }
}
class RefreshForecastWidgetCallback : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        ForecastWeatherWidget().update(context, glanceId)
    }
}
class RefreshDetailedWidgetCallback : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        DetailedWeatherWidget().update(context, glanceId)
    }
}

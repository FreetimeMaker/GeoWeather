package com.freetime.geoweather

import android.R
import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
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
    private val variant: WidgetVariant = WidgetVariant.RESPONSIVE,
    private val forceRefresh: Boolean = false
) : GlanceAppWidget() {
    companion object {
        private val SMALL = DpSize(120.dp, 60.dp)
        private val MEDIUM = DpSize(240.dp, 100.dp)
        private val LARGE = DpSize(320.dp, 180.dp)
        private val EXTRA_LARGE = DpSize(320.dp, 240.dp)
        private const val WIDGET_REFRESH_WINDOW_MS = 5 * 60 * 1000L
    }

    override val sizeMode: SizeMode = if (variant == WidgetVariant.RESPONSIVE) {
        SizeMode.Responsive(setOf(SMALL, MEDIUM, LARGE, EXTRA_LARGE))
    } else {
        SizeMode.Exact
    }

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val repository = DependencyManager.getRepository()
        val settings = DependencyManager.getAppSettings()
        val appWidgetId = (id as? androidx.glance.appwidget.AppWidgetId)?.appWidgetId
        val configuredLocationId = appWidgetId?.let { WidgetLocationPreferences.getLocationId(context, it) }
        val location = withContext(Dispatchers.IO) {
            configuredLocationId?.let { repository.getLocationById(it) } ?: repository.getSelectedLocation()
        }
        val dataSaver = settings.dataSaver.value
        val networkAvailable = isWidgetNetworkAvailable(context)

        var current = location
        var cached = location?.weatherData != null && !networkAvailable
        val now = System.currentTimeMillis()
        val freshEnough = location?.weatherData != null &&
            location.lastUpdated > 0L &&
            now - location.lastUpdated < WIDGET_REFRESH_WINDOW_MS
        val automaticRefreshAllowed = !dataSaver && !freshEnough
        val shouldRefresh = networkAvailable && (forceRefresh || automaticRefreshAllowed)
        if (location != null && shouldRefresh) {
            try {
                current = configuredLocationId?.let {
                    repository.refreshLocationWeather(it, if (forceRefresh) 0L else WIDGET_REFRESH_WINDOW_MS)
                } ?: repository.refreshSelectedLocationWeather(
                    includeHourly = true,
                    minAgeMillis = if (forceRefresh) 0L else WIDGET_REFRESH_WINDOW_MS
                ) ?: location
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
        val hourly = current?.let { repository.getHourlyForecasts(it) }.orEmpty()
        val daily = current?.let { repository.getDailyForecasts(it) }.orEmpty()
        val refreshDesc = context.getString(Res.string.refresh_nav_desc)
        val locationTimeZone = current?.let { repository.getWeatherTimeZoneId(it) }
            ?.let { runCatching { java.time.ZoneId.of(it) }.getOrNull() }
            ?: java.time.ZoneId.systemDefault()

        provideContent {
            WidgetContent(context, name, temp, code, info, hourly, daily, cached, dataSaver, LocalSize.current, refreshDesc, settings.tempUnit.value, locationTimeZone)
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
        refreshDesc: String,
        tempUnit: String,
        locationTimeZone: java.time.ZoneId
    ) {
        val compact = size.width < 180.dp || size.height < 80.dp
        val expanded = !compact && size.height >= 140.dp && (variant == WidgetVariant.FORECAST || variant == WidgetVariant.DETAILED ||
            (variant == WidgetVariant.RESPONSIVE && size.width >= 200.dp))
        val detailed = !compact && size.height >= 230.dp && (variant == WidgetVariant.DETAILED ||
            (variant == WidgetVariant.RESPONSIVE && size.height >= 140.dp))
        val hourlyCount = ((size.width.value - 20f) / 64f).toInt().coerceIn(0, 5)
        val dailyCount = ((size.width.value - 20f) / 96f).toInt().coerceIn(0, 3)
        val visibleHourly = hourly.take(hourlyCount)
        val visibleDaily = daily.take(dailyCount)
        val isDay = widgetCurrentIsDay(currentTimeZone = locationTimeZone, daily = daily)
        val palette = widgetPalette(code, isDay)

        Column(
            modifier = GlanceModifier.fillMaxSize()
                .background(ColorProvider(palette.background)).padding(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(modifier = GlanceModifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                if (!compact) {
                    Image(
                        provider = ImageProvider(WeatherIconMapper.getWeatherIcon(code)),
                        contentDescription = info,
                        modifier = GlanceModifier.size(40.dp)
                    )
                    Spacer(GlanceModifier.width(6.dp))
                }
                Column(modifier = GlanceModifier.defaultWeight()) {
                    Text(name, maxLines = 1, style = TextStyle(ColorProvider(palette.primary), if (compact) 12.sp else 14.sp, FontWeight.Bold))
                    if (temp.isNotEmpty()) {
                        Text(temp, maxLines = 1, style = TextStyle(ColorProvider(palette.primary), if (compact) 22.sp else 28.sp, FontWeight.Bold))
                    }
                    if (!compact) {
                        Text(info, maxLines = 1, style = TextStyle(ColorProvider(palette.secondary), 13.sp))
                    }
                }
                Image(
                    provider = ImageProvider(R.drawable.ic_menu_rotate),
                    contentDescription = refreshDesc,
                    modifier = GlanceModifier.size(24.dp).clickable(refreshAction())
                )
            }

            if (cached && !compact) {
                Text(
                    if (dataSaver) context.getString(Res.string.widget_data_saver_saved)
                    else context.getString(Res.string.widget_offline_saved),
                    style = TextStyle(ColorProvider(palette.muted), 12.sp)
                )
            }

            if (expanded && visibleHourly.isNotEmpty()) {
                Spacer(GlanceModifier.height(8.dp))
                Row(modifier = GlanceModifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                    visibleHourly.forEach { hour ->
                        Column(modifier = GlanceModifier.defaultWeight(), horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(hour.time.takeLast(5), maxLines = 1, style = TextStyle(ColorProvider(palette.secondary), 14.sp, FontWeight.Bold))
                            Image(
                                ImageProvider(WeatherIconMapper.getWeatherIcon(hour.code, widgetForecastIsDay(hour.time, daily))),
                                null,
                                GlanceModifier.size(24.dp)
                            )
                            Text(formatWidgetTemp(hour.temp, tempUnit), maxLines = 1, style = TextStyle(ColorProvider(palette.primary), 16.sp, FontWeight.Bold))
                        }
                    }
                }
            }

            if (detailed && visibleDaily.isNotEmpty()) {
                Spacer(GlanceModifier.height(7.dp))
                Row(modifier = GlanceModifier.fillMaxWidth()) {
                    visibleDaily.forEach { day ->
                        Column(modifier = GlanceModifier.defaultWeight(), horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(day.date.takeLast(5), maxLines = 1, style = TextStyle(ColorProvider(palette.secondary), 13.sp, FontWeight.Bold))
                            Image(
                                ImageProvider(WeatherIconMapper.getWeatherIcon(day.code, true)),
                                null,
                                GlanceModifier.size(22.dp)
                            )
                            Text(
                                formatWidgetTemp(day.minTemp, tempUnit) + "/" + formatWidgetTemp(day.maxTemp, tempUnit),
                                maxLines = 1,
                                                                style = TextStyle(ColorProvider(palette.primary), 13.sp, FontWeight.Bold)
                            )
                            if (day.precipProbMax > 0) {
                                Text(day.precipProbMax.toString() + "%", style = TextStyle(ColorProvider(palette.secondary), 12.sp))
                            }
                        }
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

class CompactWeatherWidget(forceRefresh: Boolean = false) : WeatherWidget(WidgetVariant.COMPACT, forceRefresh)
class ForecastWeatherWidget(forceRefresh: Boolean = false) : WeatherWidget(WidgetVariant.FORECAST, forceRefresh)
class DetailedWeatherWidget(forceRefresh: Boolean = false) : WeatherWidget(WidgetVariant.DETAILED, forceRefresh)

internal data class WidgetPalette(
    val background: Color,
    val primary: Color,
    val secondary: Color,
    val muted: Color
)

internal fun widgetPalette(code: Int, isDay: Boolean): WidgetPalette {
    val background = when {
        !isDay -> Color(0xFF17233A)
        code in 95..99 -> Color(0xFF394552)
        code in 71..77 || code in 85..86 -> Color(0xFFE8F1F5)
        code in 51..67 || code in 80..82 -> Color(0xFFD9E7EE)
        code in 1..3 || code == 45 || code == 48 -> Color(0xFFE3E8ED)
        else -> Color(0xFFE3F2FD)
    }
    val darkBackground = !isDay || code in 95..99
    return if (!darkBackground) {
        WidgetPalette(background, Color(0xFF102A43), Color(0xFF334E68), Color(0xFF486581))
    } else {
        WidgetPalette(background, Color(0xFFF5F8FC), Color(0xFFD9E2EC), Color(0xFFBCCCDC))
    }
}

private fun widgetCurrentIsDay(currentTimeZone: java.time.ZoneId, daily: List<DailyForecast>): Boolean {
    val today = daily.firstOrNull() ?: return true
    val now = java.time.ZonedDateTime.now(currentTimeZone).toLocalTime()
    val sunrise = runCatching { java.time.LocalTime.parse(today.sunrise.takeLast(5)) }
        .getOrDefault(java.time.LocalTime.of(7, 0))
    val sunset = runCatching { java.time.LocalTime.parse(today.sunset.takeLast(5)) }
        .getOrDefault(java.time.LocalTime.of(19, 0))
    return !now.isBefore(sunrise) && now.isBefore(sunset)
}

private fun widgetForecastIsDay(time: String, daily: List<DailyForecast>): Boolean {
    val dateTime = runCatching { java.time.LocalDateTime.parse(time) }.getOrNull()
    val localTime = dateTime?.toLocalTime()
        ?: runCatching { java.time.LocalTime.parse(time.takeLast(5)) }.getOrNull()
        ?: return true
    val date = dateTime?.toLocalDate()
    val day = date?.let { target ->
        daily.firstOrNull { runCatching { java.time.LocalDate.parse(it.date) }.getOrNull() == target }
    } ?: daily.firstOrNull()
    val sunrise = runCatching { java.time.LocalTime.parse(day?.sunrise?.takeLast(5) ?: "07:00") }
        .getOrDefault(java.time.LocalTime.of(7, 0))
    val sunset = runCatching { java.time.LocalTime.parse(day?.sunset?.takeLast(5) ?: "19:00") }
        .getOrDefault(java.time.LocalTime.of(19, 0))
    return !localTime.isBefore(sunrise) && localTime.isBefore(sunset)
}

private fun isWidgetNetworkAvailable(context: Context): Boolean {
    val connectivity = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
        ?: return false
    val network = connectivity.activeNetwork ?: return false
    val capabilities = connectivity.getNetworkCapabilities(network) ?: return false
    return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
        capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
}

private fun formatWidgetTemp(value: Int, unit: String): String {
    return if (unit == "fahrenheit") {
        ((value * 9.0 / 5.0) + 32.0).toInt().toString() + "°F"
    } else {
        value.toString() + "°C"
    }
}

class RefreshActionCallback : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        WeatherWidget(forceRefresh = true).update(context, glanceId)
    }
}
class RefreshCompactWidgetCallback : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        CompactWeatherWidget(forceRefresh = true).update(context, glanceId)
    }
}
class RefreshForecastWidgetCallback : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        ForecastWeatherWidget(forceRefresh = true).update(context, glanceId)
    }
}
class RefreshDetailedWidgetCallback : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        DetailedWeatherWidget(forceRefresh = true).update(context, glanceId)
    }
}

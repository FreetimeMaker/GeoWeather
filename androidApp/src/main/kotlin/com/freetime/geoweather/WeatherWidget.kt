package com.freetime.geoweather

import android.content.Context
import androidx.glance.GlanceId
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.provideContent
import androidx.compose.runtime.Composable
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.padding
import androidx.glance.text.Text
import com.freetime.geoweather.data.getDatabaseBuilder
import com.freetime.geoweather.data.getRoomDatabase
import androidx.glance.GlanceModifier
import androidx.glance.layout.fillMaxSize
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.freetime.geoweather.data.LocationEntity
import com.freetime.geoweather.domain.WeatherCodes

class WeatherWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val database = getRoomDatabase(getDatabaseBuilder(context))
        val location = database.locationDao().getSelectedLocation()

        provideContent {
            WeatherWidgetContent(location)
        }
    }
}

@Composable
fun WeatherWidgetContent(location: LocationEntity?) {
    Column(
        modifier = GlanceModifier.fillMaxSize().padding(12.dp)
    ) {
        Text(
            text = location?.name ?: "No location",
            style = TextStyle(color = ColorProvider(Color.White))
        )
        location?.let { current ->
            Row {
                Text(
                    text = current.currentTemp?.let { "${it}°" } ?: "--",
                    style = TextStyle(color = ColorProvider(Color.White))
                )
                current.hourlyForecast.firstOrNull()?.let { hour ->
                    Text(
                        text = "  ${hour.temperature}°",
                        style = TextStyle(color = ColorProvider(Color.White))
                    )
                }
            }
            Text(
                text = current.currentWeatherCode?.let { WeatherCodes.getDescription(it) } ?: "",
                style = TextStyle(color = ColorProvider(Color.White))
            )
        }
    }
}

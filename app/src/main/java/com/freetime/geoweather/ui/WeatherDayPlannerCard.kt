package com.freetime.geoweather.ui

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.Card
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.freetime.geoweather.R
import com.freetime.geoweather.WeatherDayPlanner
import com.freetime.geoweather.data.DailyForecast
import com.freetime.geoweather.data.HourlyForecast
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun WeatherDayPlannerCard(
    hourly: List<HourlyForecast>, daily: List<DailyForecast>, timezoneName: String,
    tempUnit: String, windUnit: String, nowMillis: Long, lastUpdated: Long
) {
    var activityName by rememberSaveable { mutableStateOf(WeatherDayPlanner.Activity.WALKING.name) }
    val activity = WeatherDayPlanner.Activity.valueOf(activityName)
    val plan = WeatherDayPlanner.plan(hourly, daily, timezoneName, nowMillis, lastUpdated, activity)
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(stringResource(R.string.day_planner_title), style = MaterialTheme.typography.titleMedium)
            when (plan.status) {
                WeatherDayPlanner.Status.STALE -> Text(stringResource(R.string.day_planner_stale))
                WeatherDayPlanner.Status.INVALID_ZONE -> Text(stringResource(R.string.day_planner_zone))
                WeatherDayPlanner.Status.FRESH -> {
                    val zone = ZoneId.of(timezoneName)
                    val formatter = DateTimeFormatter.ofPattern("HH:mm", Locale.getDefault()).withZone(zone)
                    val summary = requireNotNull(plan.today)
                    val temperature = if (summary.minTemp != null && summary.maxTemp != null) {
                        stringResource(R.string.day_planner_temperature,
                            formatTemp(summary.minTemp.toDouble(), tempUnit), formatTemp(summary.maxTemp.toDouble(), tempUnit))
                    } else stringResource(R.string.day_planner_temperature_unknown)
                    val rain = summary.firstRain?.let {
                        stringResource(R.string.day_planner_rain, formatter.format(Instant.ofEpochMilli(it.millis)),
                            it.forecast.precipProbability.takeIf { probability -> probability in 1..100 }?.let { probability ->
                                stringResource(R.string.day_planner_probability, probability)
                            } ?: stringResource(R.string.day_planner_probability_unknown))
                    } ?: stringResource(if (summary.incompleteRain) R.string.day_planner_rain_unknown else R.string.day_planner_no_rain)
                    val wind = if (summary.maxWind != null) {
                        stringResource(R.string.day_planner_wind, formatWind(summary.maxWind, null, windUnit))
                    } else stringResource(R.string.day_planner_wind_unknown)
                    Text(stringResource(R.string.day_planner_summary, temperature, rain, wind))
                    if (summary.incompleteRain || summary.incompleteWind) Text(stringResource(R.string.day_planner_incomplete))
                    Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        WeatherDayPlanner.Activity.entries.forEach { option ->
                            val label = when (option) {
                                WeatherDayPlanner.Activity.WALKING -> R.string.day_planner_walking
                                WeatherDayPlanner.Activity.RUNNING -> R.string.day_planner_running
                                WeatherDayPlanner.Activity.CYCLING -> R.string.day_planner_cycling
                                WeatherDayPlanner.Activity.GARDENING -> R.string.day_planner_gardening
                            }
                            FilterChip(selected = option == activity, onClick = { activityName = option.name },
                                label = { Text(stringResource(label)) })
                        }
                    }
                    val window = plan.window
                    if (window == null) Text(stringResource(R.string.day_planner_no_window)) else {
                        val windowFormatter = DateTimeFormatter.ofPattern("EEE HH:mm XXX", Locale.getDefault()).withZone(zone)
                        Text(stringResource(R.string.day_planner_window,
                            windowFormatter.format(Instant.ofEpochMilli(window.startMillis)),
                            windowFormatter.format(Instant.ofEpochMilli(window.endMillis)), window.score))
                    }
                    Text(stringResource(R.string.day_planner_disclaimer), style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}

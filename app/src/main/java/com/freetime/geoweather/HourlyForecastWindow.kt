package com.freetime.geoweather

import com.freetime.geoweather.data.HourlyForecast
import java.time.Duration
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZoneOffset

/** UI-only window; callers must retain the full forecast for daily details and analytics. */
internal fun upcoming24HourForecasts(
    forecasts: List<HourlyForecast>,
    timeZoneId: String,
    nowMillis: Long
): List<HourlyForecast> {
    val zone = runCatching { ZoneId.of(timeZoneId) }.getOrDefault(ZoneOffset.UTC)
    val start = Instant.ofEpochMilli(nowMillis)
    val end = start.plus(Duration.ofHours(24))
    return forecasts.filter { forecast ->
        val time = runCatching { LocalDateTime.parse(forecast.time).atZone(zone).toInstant() }
            .getOrNull() ?: return@filter false
        // Exclude already-started buckets; the upper bound is 24 elapsed hours, even across DST.
        !time.isBefore(start) && time.isBefore(end)
    }
}

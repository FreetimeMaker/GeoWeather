package com.freetime.geoweather

import com.freetime.geoweather.data.DailyForecast
import com.freetime.geoweather.data.HourlyForecast
import java.time.Instant
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.OffsetDateTime
import java.time.ZoneId
import kotlin.math.abs

/** Pure, local ranking; temperatures are Celsius, wind km/h, amounts mm. Not safety advice. */
object WeatherDayPlanner {
    enum class Activity(val idealTemp: Int, val windPenalty: Double) {
        WALKING(18, 0.5), RUNNING(12, 0.6), CYCLING(18, 1.2), GARDENING(20, 0.7)
    }
    enum class Status { FRESH, STALE, INVALID_ZONE }
    data class Hour(val millis: Long, val forecast: HourlyForecast)
    data class Window(val startMillis: Long, val endMillis: Long, val score: Int)
    data class Today(
        val minTemp: Int?, val maxTemp: Int?, val firstRain: Hour?,
        val maxWind: Double?, val incompleteRain: Boolean, val incompleteWind: Boolean
    )
    data class Plan(val status: Status, val today: Today? = null, val window: Window? = null)
    private const val HOUR = 3_600_000L

    fun plan(
        hourly: List<HourlyForecast>, daily: List<DailyForecast>, timezoneName: String,
        nowMillis: Long, lastUpdated: Long, activity: Activity = Activity.WALKING
    ): Plan {
        if (lastUpdated <= 0 || lastUpdated > nowMillis || nowMillis - lastUpdated > 2 * HOUR) {
            return Plan(Status.STALE)
        }
        val zone = runCatching { ZoneId.of(timezoneName) }.getOrNull() ?: return Plan(Status.INVALID_ZONE)
        val today = Instant.ofEpochMilli(nowMillis).atZone(zone).toLocalDate()
        val hours = resolveHours(hourly, zone, lastUpdated)
        val future = hours.filter { it.millis >= nowMillis && it.millis < nowMillis + 24 * HOUR }
        val remainingToday = future.filter { Instant.ofEpochMilli(it.millis).atZone(zone).toLocalDate() == today }
        val day = daily.firstOrNull { it.date == today.toString() }
        val summary = Today(
            day?.minTemp ?: remainingToday.minOfOrNull { it.forecast.temp },
            day?.maxTemp ?: remainingToday.maxOfOrNull { it.forecast.temp },
            remainingToday.firstOrNull { wet(it.forecast) },
            remainingToday.mapNotNull { valid(it.forecast.windSpeed) }.maxOrNull(),
            remainingToday.isEmpty() || remainingToday.any { !rainKnown(it.forecast) },
            remainingToday.isEmpty() || remainingToday.any { valid(it.forecast.windSpeed) == null }
        )
        val window = future.zipWithNext().mapNotNull { (a, b) ->
            if (b.millis - a.millis != HOUR || b.millis + HOUR > nowMillis + 24 * HOUR ||
                !eligible(a.forecast) || !eligible(b.forecast)) null
            else Window(a.millis, b.millis + HOUR, ((score(a.forecast, activity) + score(b.forecast, activity)) / 2).toInt())
        }.filter { it.score >= 60 }.maxByOrNull { it.score }
        return Plan(Status.FRESH, summary, window)
    }

    /** HH:mm lists preserve source order and are anchored to the fetch day, not the viewing day.
     * A first hour before the fetch hour belongs to tomorrow (repository future-only contract).
     * Explicit dates win. Ambiguous/nonexistent DST local times are discarded, never guessed.
     */
    fun resolveHours(hourly: List<HourlyForecast>, zone: ZoneId, lastUpdated: Long): List<Hour> {
        val anchor = Instant.ofEpochMilli(lastUpdated).atZone(zone)
        var date = anchor.toLocalDate()
        var previous: LocalTime? = null
        return hourly.mapNotNull { forecast ->
            val instant = runCatching {
                val text = forecast.time
                if ('T' in text) {
                    runCatching { Instant.parse(text) }.getOrNull()
                        ?: runCatching { OffsetDateTime.parse(text).toInstant() }.getOrNull()
                        ?: localInstant(LocalDateTime.parse(text), zone)
                } else {
                    val time = LocalTime.parse(text)
                    if ((previous == null && time.hour < anchor.hour) || (previous != null && time < previous)) {
                        date = date.plusDays(1)
                    }
                    previous = time
                    localInstant(date.atTime(time), zone)
                }
            }.getOrNull() ?: return@mapNotNull null
            Hour(instant.toEpochMilli(), forecast)
        }.distinctBy { it.millis }.sortedBy { it.millis }
    }

    private fun localInstant(time: LocalDateTime, zone: ZoneId): Instant? =
        zone.rules.getValidOffsets(time).singleOrNull()?.let { time.toInstant(it) }
    private fun valid(value: Double?): Double? = value?.takeIf { it.isFinite() && it >= 0 }
    // Probability zero alone cannot distinguish a parser default from a measured zero.
    private fun rainKnown(h: HourlyForecast) = h.precipProbability in 0..100 &&
        (h.precipProbability > 0 || valid(h.precipitation) != null || valid(h.rain) != null)
    private fun wet(h: HourlyForecast) = h.precipProbability > 0 ||
        (valid(h.precipitation) ?: 0.0) > 0 || (valid(h.rain) ?: 0.0) > 0 ||
        h.code in setOf(51, 53, 55, 56, 57, 61, 63, 65, 66, 67, 80, 81, 82)
    private fun eligible(h: HourlyForecast): Boolean =
        h.code in setOf(0, 1, 2, 3, 45, 48, 51, 53, 55, 61, 63, 65, 80, 81, 82) &&
            rainKnown(h) && valid(h.windSpeed) != null && valid(h.windGusts) != null &&
            h.windSpeed!! < 50 && h.windGusts!! < 65 && (valid(h.snowfall) ?: 0.0) == 0.0

    /** 100 minus 3/°C from activity ideal, 0.6/probability point, 8/mm precipitation,
     * activity-specific wind penalty and 0.2/gust km/h. Mean of two complete hours;
     * only windows scoring at least 60 are suggested, ties prefer earliest.
     * Exclusions are conservative heuristics, not a safety guarantee.
     */
    private fun score(h: HourlyForecast, activity: Activity): Double =
        (100 - 3 * abs(h.temp - activity.idealTemp) - 0.6 * h.precipProbability -
            8 * (valid(h.precipitation) ?: valid(h.rain) ?: 0.0) -
            activity.windPenalty * h.windSpeed!! - 0.2 * h.windGusts!!).coerceIn(0.0, 100.0)
}

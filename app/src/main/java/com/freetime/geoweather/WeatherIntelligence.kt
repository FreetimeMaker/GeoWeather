package com.freetime.geoweather

import com.freetime.geoweather.data.DailyForecast
import com.freetime.geoweather.data.HourlyForecast
import com.freetime.geoweather.data.WeatherHistoryEntity
import kotlin.math.abs

data class ActivityWindow(val activity: String, val hours: List<String>, val score: Int)
data class ProviderSnapshot(val provider: String, val temperature: Double?, val weatherCode: Int?)
data class NowcastSummary(val startsAt: String?, val endsAt: String?, val peakProbability: Int, val peakAmountMm: Double)
data class SmartHeroInsight(val primary: String, val secondary: String?)

object WeatherIntelligence {
    fun nowcast(hourly: List<HourlyForecast>, windowHours: Int = 2): NowcastSummary? {
        val window = hourly.take(windowHours.coerceAtLeast(1))
        if (window.isEmpty()) return null
        val wet = window.filter { (it.precipitation ?: 0.0) > 0.05 || it.precipProbability >= 35 }
        if (wet.isEmpty()) {
            return NowcastSummary(
                startsAt = null,
                endsAt = null,
                peakProbability = window.maxOfOrNull { it.precipProbability } ?: 0,
                peakAmountMm = 0.0
            )
        }
        return NowcastSummary(
            startsAt = wet.first().time,
            endsAt = wet.last().time,
            peakProbability = wet.maxOf { it.precipProbability },
            peakAmountMm = wet.maxOf { it.precipitation ?: 0.0 }
        )
    }

    fun pressureTrendLabel(trend: Int): String = when {
        trend > 0 -> "Rising"
        trend < 0 -> "Falling"
        else -> "Steady"
    }

    fun smartHero(hourly: List<HourlyForecast>, daily: DailyForecast?): SmartHeroInsight {
        val next = hourly.firstOrNull()
        val severeRain = hourly.firstOrNull {
            (it.precipitation ?: 0.0) >= 2.5 || it.precipProbability >= 70
        }
        val strongWind = hourly.firstOrNull {
            (it.windGusts ?: it.windSpeed ?: 0.0) >= 45.0
        }
        return when {
            severeRain != null -> SmartHeroInsight(
                primary = "Rain likely around " + severeRain.time,
                secondary = severeRain.precipProbability.toString() + "% · " +
                    String.format(java.util.Locale.US, "%.1f mm", severeRain.precipitation ?: 0.0)
            )
            strongWind != null -> SmartHeroInsight(
                primary = "Strong gusts around " + strongWind.time,
                secondary = ((strongWind.windGusts ?: strongWind.windSpeed ?: 0.0).toInt()).toString() + " km/h"
            )
            (daily?.uvMax ?: 0.0) >= 6.0 -> SmartHeroInsight(
                primary = "High UV today",
                secondary = "UV max " + String.format(java.util.Locale.US, "%.1f", daily?.uvMax ?: 0.0)
            )
            next != null -> SmartHeroInsight(
                primary = "Next hour " + next.temp + "°",
                secondary = next.precipProbability.toString() + "% precipitation"
            )
            else -> SmartHeroInsight("Weather overview", null)
        }
    }

    fun briefing(location: String, daily: DailyForecast?, hourly: List<HourlyForecast>, evening: Boolean = false): String {
        if (daily == null) return location
        val rain = hourly.firstOrNull { it.precipProbability >= 40 }
        val period = if (evening) "Tonight" else "Today"
        return buildString {
            append("$period in $location: ${daily.minTemp}–${daily.maxTemp}°")
            if (rain != null) append(", rain possible around ${rain.time} (${rain.precipProbability}%)")
            if (daily.windMax >= 35) append(", strong wind possible")
            if ((daily.uvMax ?: 0.0) >= 6) append(", high UV")
        }
    }

    fun activityWindows(hourly: List<HourlyForecast>): List<ActivityWindow> {
        fun score(h: HourlyForecast, maxWind: Double, maxRain: Int, maxUv: Double): Int {
            var value = 100
            if (h.precipProbability > maxRain) value -= 45
            if ((h.windSpeed ?: 0.0) > maxWind) value -= 30
            if ((h.uvIndex ?: 0.0) > maxUv) value -= 15
            if (h.temp !in 0..30) value -= 20
            return value.coerceIn(0, 100)
        }
        return listOf(
            Triple("Walking", 35.0, 45) to 7.0,
            Triple("Running", 30.0, 35) to 6.0,
            Triple("Cycling", 25.0, 30) to 6.0,
            Triple("Photography", 40.0, 50) to 8.0
        ).map { (base, uv) ->
            val (name, wind, rain) = base
            val ranked = hourly.map { it to score(it, wind, rain, uv) }.filter { it.second >= 60 }.take(4)
            ActivityWindow(name, ranked.map { it.first.time }, ranked.maxOfOrNull { it.second } ?: 0)
        }
    }

    fun forecastAccuracy(history: List<WeatherHistoryEntity>, forecastTemp: Double?): Int? {
        val actual = history.firstOrNull()?.temperature ?: return null
        forecastTemp ?: return null
        return (100 - (abs(actual - forecastTemp) * 10).toInt()).coerceIn(0, 100)
    }

    fun providerComparison(primary: ProviderSnapshot, others: List<ProviderSnapshot>): List<ProviderSnapshot> =
        (listOf(primary) + others).distinctBy { it.provider }
}

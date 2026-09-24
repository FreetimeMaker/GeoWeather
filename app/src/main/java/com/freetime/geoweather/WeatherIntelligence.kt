package com.freetime.geoweather

import com.freetime.geoweather.data.DailyForecast
import com.freetime.geoweather.data.HourlyForecast

data class ActivityWindow(val activity: String, val hours: List<String>, val score: Int)
data class ProviderSnapshot(val provider: String, val temperature: Double?, val weatherCode: Int?)
data class NowcastSummary(val startsAt: String?, val endsAt: String?, val peakProbability: Int, val peakAmountMm: Double)
data class SmartHeroInsight(
    val kind: String,
    val time: String? = null,
    val value: Double? = null,
    val probability: Int? = null,
    val temperature: Int? = null
)
data class WeatherBriefing(
    val location: String,
    val minTemp: Int,
    val maxTemp: Int,
    val evening: Boolean,
    val rainTime: String?,
    val rainProbability: Int?,
    val strongWind: Boolean,
    val highUv: Boolean
)
data class ActivityDetail(val activity: String, val score: Int, val bestHours: List<String>, val reasons: List<String>)

object WeatherIntelligence {
    private fun formatOneDecimal(value: Double): String {
        val scaled = kotlin.math.round(value * 10.0).toInt()
        return (scaled / 10).toString() + "." + kotlin.math.abs(scaled % 10)
    }

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

    fun pressureTrendKey(trend: Int): String = when {
        trend > 0 -> "rising"
        trend < 0 -> "falling"
        else -> "steady"
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
                kind = "rain",
                time = severeRain.time,
                value = severeRain.precipitation ?: 0.0,
                probability = severeRain.precipProbability
            )
            strongWind != null -> SmartHeroInsight(
                kind = "wind",
                time = strongWind.time,
                value = strongWind.windGusts ?: strongWind.windSpeed ?: 0.0
            )
            (daily?.uvMax ?: 0.0) >= 6.0 -> SmartHeroInsight(kind = "uv", value = daily?.uvMax)
            next != null -> SmartHeroInsight(
                kind = "next_hour",
                temperature = next.temp,
                probability = next.precipProbability
            )
            else -> SmartHeroInsight(kind = "overview")
        }
    }

    fun briefing(
        location: String,
        daily: DailyForecast?,
        hourly: List<HourlyForecast>,
        evening: Boolean = false
    ): WeatherBriefing? {
        if (daily == null) return null
        val rain = hourly.firstOrNull { it.precipProbability >= 40 }
        return WeatherBriefing(
            location = location,
            minTemp = daily.minTemp,
            maxTemp = daily.maxTemp,
            evening = evening,
            rainTime = rain?.time,
            rainProbability = rain?.precipProbability,
            strongWind = daily.windMax >= 35,
            highUv = (daily.uvMax ?: 0.0) >= 6
        )
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
            Triple("walking", 35.0, 45) to 7.0,
            Triple("running", 30.0, 35) to 6.0,
            Triple("cycling", 25.0, 30) to 6.0,
            Triple("photography", 40.0, 50) to 8.0
        ).map { (base, uv) ->
            val (name, wind, rain) = base
            val ranked = hourly.map { it to score(it, wind, rain, uv) }.filter { it.second >= 60 }.take(4)
            ActivityWindow(name, ranked.map { it.first.time }, ranked.maxOfOrNull { it.second } ?: 0)
        }
    }

    fun activityDetails(hourly: List<HourlyForecast>): List<ActivityDetail> {
        val next = hourly.take(24)
        fun build(name: String, windLimit: Double, rainLimit: Int, uvLimit: Double): ActivityDetail {
            val ranked = next.map { hour ->
                var score = 100
                if (hour.precipProbability >= rainLimit) score -= 40
                if ((hour.windGusts ?: hour.windSpeed ?: 0.0) >= windLimit) score -= 30
                if ((hour.uvIndex ?: 0.0) >= uvLimit) score -= 15
                if (hour.temp !in 2..28) score -= 15
                hour to score.coerceIn(0, 100)
            }.sortedByDescending { it.second }
            val best = ranked.take(4)
            val sample = best.firstOrNull()?.first
            val reasons = buildList {
                sample?.let {
                    add("rain:" + it.precipProbability)
                    add("wind:" + (it.windGusts ?: it.windSpeed ?: 0.0).toInt())
                    it.uvIndex?.let { uv -> add("uv:" + formatOneDecimal(uv)) }
                    add("feels:" + formatOneDecimal(it.feelsLike ?: it.temp.toDouble()))
                }
            }
            return ActivityDetail(name, best.maxOfOrNull { it.second } ?: 0, best.map { it.first.time }, reasons)
        }
        return listOf(
            build("running", 35.0, 40, 7.0),
            build("cycling", 30.0, 35, 7.0)
        )
    }

    fun photographyWindows(hourly: List<HourlyForecast>, daily: DailyForecast?): ActivityDetail {
        val sunrise = daily?.sunrise?.takeLast(5)
        val sunset = daily?.sunset?.takeLast(5)
        val candidates = hourly.take(24).filter { hour ->
            val time = hour.time.takeLast(5)
            (sunrise != null && kotlin.math.abs(timeToMinutes(time) - timeToMinutes(sunrise)) <= 60) ||
                (sunset != null && kotlin.math.abs(timeToMinutes(time) - timeToMinutes(sunset)) <= 60)
        }
        val scored = candidates.map { hour ->
            var score = 100
            if (hour.precipProbability >= 50) score -= 35
            if ((hour.windGusts ?: 0.0) >= 45) score -= 20
            val clouds = hour.cloudBaseM
            if (clouds != null && clouds < 300) score -= 15
            hour to score.coerceIn(0, 100)
        }.sortedByDescending { it.second }
        return ActivityDetail(
            activity = "photography",
            score = scored.firstOrNull()?.second ?: 0,
            bestHours = scored.take(4).map { it.first.time },
            reasons = listOfNotNull(
                sunrise?.let { "sunrise:" + it },
                sunset?.let { "sunset:" + it },
                scored.firstOrNull()?.first?.precipProbability?.let { "rain:" + it }
            )
        )
    }

    private fun timeToMinutes(value: String): Int {
        val parts = value.split(":")
        return (parts.getOrNull(0)?.toIntOrNull() ?: 0) * 60 + (parts.getOrNull(1)?.toIntOrNull() ?: 0)
    }

    fun providerComparison(primary: ProviderSnapshot, others: List<ProviderSnapshot>): List<ProviderSnapshot> =
        (listOf(primary) + others).distinctBy { it.provider }
}

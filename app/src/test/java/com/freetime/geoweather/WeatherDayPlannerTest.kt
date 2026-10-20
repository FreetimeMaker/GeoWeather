package com.freetime.geoweather

import com.freetime.geoweather.data.DailyForecast
import com.freetime.geoweather.data.HourlyForecast
import java.time.Instant
import java.time.ZoneId
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class WeatherDayPlannerTest {
    private val now = millis("2026-06-01T10:00:00Z")
    private fun millis(text: String) = Instant.parse(text).toEpochMilli()
    private fun hour(time: String, temp: Int = 18) = HourlyForecast(
        time, temp, 0, precipitation = 0.0, windSpeed = 5.0, windGusts = 10.0
    )
    private fun plan(hours: List<HourlyForecast>, updated: Long = now, clock: Long = now,
                     zone: String = "UTC", daily: List<DailyForecast> = emptyList(),
                     activity: WeatherDayPlanner.Activity = WeatherDayPlanner.Activity.WALKING) =
        WeatherDayPlanner.plan(hours, daily, zone, clock, updated, activity)

    @Test fun freshnessBoundaryAndInvalidTimes() {
        val hours = listOf(hour("12:00"), hour("13:00"))
        assertEquals(WeatherDayPlanner.Status.FRESH, plan(hours, now - 7_200_000).status)
        for (updated in listOf(0L, now + 1, now - 7_200_001)) {
            val result = plan(hours, updated)
            assertEquals(WeatherDayPlanner.Status.STALE, result.status)
            assertNull(result.today)
            assertNull(result.window)
        }
        assertEquals(WeatherDayPlanner.Status.INVALID_ZONE, plan(hours, zone = "invalid").status)
    }

    @Test fun clockOnlyRolloverAndFetchAnchor() {
        val fetched = millis("2026-06-01T23:30:00Z")
        val hours = listOf(hour("23:00"), hour("00:00"), hour("01:00"))
        val resolved = WeatherDayPlanner.resolveHours(hours, ZoneId.of("UTC"), fetched)
        assertEquals(millis("2026-06-02T00:00:00Z"), resolved[1].millis)
        val result = plan(hours, fetched, fetched)
        assertNull(result.today!!.minTemp)
        assertEquals(resolved[1].millis, result.window!!.startMillis)
        assertEquals(resolved[1].millis, plan(hours, fetched, fetched + 1_800_000).window!!.startMillis)
        assertEquals(resolved[1].millis, WeatherDayPlanner.resolveHours(
            listOf(hour("00:00")), ZoneId.of("UTC"), fetched).single().millis)
    }

    @Test fun explicitPastDatesAreNotRecycledAndTomorrowIsNotToday() {
        val hours = listOf(hour("2026-05-31T12:00"), hour("2026-06-02T00:00"), hour("2026-06-02T01:00"))
        val result = plan(hours, daily = listOf(DailyForecast("2026-06-02", 0, 40, 30)))
        assertNull(result.today!!.minTemp)
        assertEquals(millis("2026-06-02T00:00:00Z"), result.window!!.startMillis)
    }

    @Test fun summaryUsesLocationDayAndOnlyFutureRain() {
        val hours = listOf(hour("2026-06-01T18:00").copy(precipProbability = 90),
            hour("2026-06-01T20:00").copy(precipProbability = 30),
            hour("2026-06-02T00:00").copy(precipProbability = 80))
        val clock = millis("2026-06-01T10:30:00Z") // 19:30 Tokyo
        val result = plan(hours, clock = clock, zone = "Asia/Tokyo",
            daily = listOf(DailyForecast("2026-06-01", 0, 25, 12)))
        assertEquals(12, result.today!!.minTemp)
        assertEquals(30, result.today!!.firstRain!!.forecast.precipProbability)
    }

    @Test fun missingRainWindAndGustsNeverProduceAWindow() {
        val a = hour("11:00")
        for (bad in listOf(a.copy(precipitation = null), a.copy(windSpeed = null),
            a.copy(windGusts = null), a.copy(windSpeed = Double.NaN), a.copy(precipProbability = -1))) {
            assertNull(plan(listOf(bad, hour("12:00"))).window)
        }
        assertTrue(plan(listOf(a.copy(precipitation = null))).today!!.incompleteRain)
    }

    @Test fun excludesHazardsAndRequiresConsecutiveCompleteHours() {
        for (code in listOf(56, 57, 66, 67, 71, 73, 75, 77, 85, 86, 95, 96, 99, -1)) {
            assertNull(plan(listOf(hour("11:00").copy(code = code), hour("12:00"))).window)
        }
        for (bad in listOf(hour("11:00").copy(windSpeed = 50.0),
            hour("11:00").copy(windGusts = 65.0), hour("11:00").copy(snowfall = 0.1))) {
            assertNull(plan(listOf(bad, hour("12:00"))).window)
        }
        assertNull(plan(listOf(hour("11:00"), hour("13:00"))).window)
        assertNull(plan(listOf(hour("2026-06-02T09:00"), hour("2026-06-02T10:00"))).window)
    }

    @Test fun poorConditionsDoNotBecomeRecommendationsJustBecauseTheyAreTheBestAvailable() {
        val poor = listOf("11:00", "12:00").map { time ->
            hour(time, 4).copy(precipProbability = 90, precipitation = 5.0, windSpeed = 35.0, windGusts = 50.0)
        }
        assertNull(plan(poor).window)
    }

    @Test fun activityChangesRankingAndTiesPreferEarlier() {
        val hours = listOf(hour("11:00", 18), hour("12:00", 18), hour("14:00", 12), hour("15:00", 12))
        assertEquals(millis("2026-06-01T11:00:00Z"), plan(hours).window!!.startMillis)
        assertEquals(millis("2026-06-01T14:00:00Z"),
            plan(hours, activity = WeatherDayPlanner.Activity.RUNNING).window!!.startMillis)
        assertEquals(millis("2026-06-01T11:00:00Z"),
            plan(listOf(hour("11:00"), hour("12:00"), hour("13:00"))).window!!.startMillis)
    }

    @Test fun malformedAndAmbiguousDstTimesAreDiscarded() {
        val hours = listOf(hour("oops"), hour("2026-10-25T02:00"), hour("2026-03-29T02:00"))
        assertTrue(WeatherDayPlanner.resolveHours(hours, ZoneId.of("Europe/Berlin"), now).isEmpty())
        val explicit = WeatherDayPlanner.resolveHours(listOf(hour("2026-10-25T02:00+02:00")),
            ZoneId.of("Europe/Berlin"), now)
        assertNotNull(explicit.singleOrNull())
        assertEquals(millis("2026-10-25T00:00:00Z"), explicit.single().millis)
    }
}

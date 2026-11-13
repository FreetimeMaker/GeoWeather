package com.freetime.geoweather

import com.freetime.geoweather.data.HourlyForecast
import java.time.Instant
import java.time.LocalDateTime
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class HourlyForecastWindowTest {
    private fun hour(time: String) = HourlyForecast(time, 18, 0)
    private fun window(times: List<String>, zone: String, now: String) = upcoming24HourForecasts(
        times.map(::hour), zone, Instant.parse(now).toEpochMilli()
    ).map { it.time }

    @Test fun excludesPastAndUsesHalfOpen24HourBoundary() {
        val times = listOf(
            "2026-06-01T09:00", "2026-06-01T10:00", "2026-06-02T09:59",
            "2026-06-02T10:00", "2026-06-03T10:00"
        )
        assertEquals(times.slice(1..2), window(times, "UTC", "2026-06-01T10:00:00Z"))
    }

    @Test fun excludesAlreadyStartedBucketAndDoesNotTake24FromMidnight() {
        val midnight = LocalDateTime.parse("2026-06-01T00:00")
        val times = (0L..71L).map { midnight.plusHours(it).toString() }
        val expected = (11L..34L).map { midnight.plusHours(it).toString() }
        val forecasts = times.map(::hour)
        assertEquals(expected, upcoming24HourForecasts(
            forecasts, "UTC", Instant.parse("2026-06-01T10:30:00Z").toEpochMilli()
        ).map { it.time })
        assertEquals(72, forecasts.size) // The caller's multi-day data remains intact.
    }

    @Test fun usesLocationTimezoneAcrossDateBoundaries() {
        val tokyo = listOf("2026-06-01T23:00", "2026-06-02T00:00", "2026-06-02T23:00", "2026-06-03T00:00")
        assertEquals(tokyo.slice(1..2), window(tokyo, "Asia/Tokyo", "2026-06-01T14:30:00Z"))
        val losAngeles = listOf("2026-05-31T23:00", "2026-06-01T00:00", "2026-06-01T23:00", "2026-06-02T00:00")
        assertEquals(losAngeles.slice(1..2), window(losAngeles, "America/Los_Angeles", "2026-06-01T06:30:00Z"))
    }

    @Test fun gapsDoNotExtendWindowToFill24Entries() {
        val times = listOf("2026-06-01T11:00", "2026-06-02T09:00", "2026-06-02T11:00")
        assertEquals(times.take(2), window(times, "UTC", "2026-06-01T10:00:00Z"))
    }

    @Test fun measures24ElapsedHoursAcrossDaylightSavingChanges() {
        // New York jumps from 01:59 to 03:00; next midnight is only 23 hours away.
        val spring = listOf("2026-03-08T00:00", "2026-03-09T00:00", "2026-03-09T01:00")
        assertEquals(spring.take(2), window(spring, "America/New_York", "2026-03-08T05:00:00Z"))
        // On the 25-hour autumn day, 23:00 is already exactly 24 elapsed hours away.
        val autumn = listOf("2026-11-01T00:00", "2026-11-01T22:00", "2026-11-01T23:00")
        assertEquals(autumn.take(2), window(autumn, "America/New_York", "2026-11-01T04:00:00Z"))
    }

    @Test fun invalidTimesAreExcludedAndInvalidTimezoneFallsBackToUtc() {
        val times = listOf("bad", "11:00", "2026-06-01T11:00")
        assertEquals(times.takeLast(1), window(times, "invalid", "2026-06-01T10:00:00Z"))
        assertTrue(window(emptyList(), "UTC", "2026-06-01T10:00:00Z").isEmpty())
        assertTrue(window(listOf("2026-05-01T10:00"), "UTC", "2026-06-01T10:00:00Z").isEmpty())
    }
}

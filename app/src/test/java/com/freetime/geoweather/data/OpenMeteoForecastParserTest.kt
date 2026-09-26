package com.freetime.geoweather.data

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class OpenMeteoForecastParserTest {
    @Test fun invalidCoreHourlyEntriesAreSkipped() {
        val raw = """{"timezone":"UTC","hourly":{"time":["2026-01-01T00:00","2026-01-01T01:00"],"temperature_2m":[null,5.4],"weather_code":[0,61]}}"""
        val rows = OpenMeteoForecastParser.hourly(raw, 1767225600000)
        assertEquals(1, rows.size)
        assertEquals(5, rows.single().temp)
        assertEquals(61, rows.single().code)
    }

    @Test fun invalidCoreDailyEntriesAreSkipped() {
        val raw = """{"daily":{"time":["2026-01-01","2026-01-02"],"weather_code":[null,3],"temperature_2m_max":[10,11],"temperature_2m_min":[2,3]}}"""
        val rows = OpenMeteoForecastParser.daily(raw)
        assertEquals(1, rows.size)
        assertEquals("2026-01-02", rows.single().date)
    }

    @Test fun sixteenDaysAndAllHoursArePreserved() {
        val dates=(1..16).map { "2026-01-"+it.toString().padStart(2,'0') }
        val times=dates.flatMap { d -> (0..23).map { h -> d+"T"+h.toString().padStart(2,'0')+":00" } }
        fun strings(values: List<String>)=values.joinToString(prefix="[",postfix="]") { "\"$it\"" }
        fun nums(count:Int,value:String)=List(count){value}.joinToString(prefix="[",postfix="]")
        val raw="""{"timezone":"UTC","hourly":{"time":${strings(times)},"temperature_2m":${nums(times.size,"12.0")},"weather_code":${nums(times.size,"2")}},"daily":{"time":${strings(dates)},"weather_code":${nums(16,"2")},"temperature_2m_max":${nums(16,"15.0")},"temperature_2m_min":${nums(16,"5.0")}}}"""
        assertEquals(384, OpenMeteoForecastParser.hourly(raw, 1767225600000).size)
        assertEquals(16, OpenMeteoForecastParser.daily(raw).size)
    }

    @Test fun missingForecastSectionsAreEmpty() {
        assertTrue(OpenMeteoForecastParser.hourly("""{"timezone":"UTC"}""").isEmpty())
        assertTrue(OpenMeteoForecastParser.daily("""{}""").isEmpty())
    }
}

package com.freetime.geoweather.data

import kotlin.test.Test
import kotlin.test.assertEquals

class LocationEntityWeatherCodeTest {
    private fun location(icon: String) = LocationEntity(
        name = "Test",
        latitude = 0.0,
        longitude = 0.0,
        weatherData = """{"currentConditions":{"icon":"$icon","temp":12.0}}"""
    )

    @Test fun visualCrossingClearMapsToClearWmo() = assertEquals(0, location("clear-night").currentWeatherCode)
    @Test fun visualCrossingCloudyMapsToOvercastWmo() = assertEquals(3, location("cloudy").currentWeatherCode)
    @Test fun visualCrossingRainMapsToRainWmo() = assertEquals(63, location("rain").currentWeatherCode)
    @Test fun visualCrossingShowersMapsToShowerWmo() = assertEquals(80, location("showers-day").currentWeatherCode)
    @Test fun visualCrossingSnowMapsToSnowWmo() = assertEquals(73, location("snow").currentWeatherCode)
    @Test fun visualCrossingThunderMapsToThunderstormWmo() = assertEquals(95, location("thunder-rain").currentWeatherCode)
}

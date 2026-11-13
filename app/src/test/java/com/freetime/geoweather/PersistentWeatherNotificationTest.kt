package com.freetime.geoweather

import com.freetime.geoweather.data.LocationEntity
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class PersistentWeatherNotificationTest {
    private val first = LocationEntity(1L, "First", 1.0, 2.0)
    private val second = LocationEntity(2L, "Second", 3.0, 4.0)

    @Test fun savedLocationWorksWithoutOpeningItsDetailsFirst() {
        assertEquals(first, persistentWeatherLocation(listOf(first, second)))
    }

    @Test fun selectedLocationTakesPrecedence() {
        val selected = second.copy(selected = true)
        assertEquals(selected, persistentWeatherLocation(listOf(first, selected)))
    }

    @Test fun deletedSelectionFallsBackToRemainingLocation() {
        assertEquals(first, persistentWeatherLocation(listOf(first)))
        assertNull(persistentWeatherLocation(emptyList()))
    }

    @Test fun oldFavoriteFlagDoesNotChangeNotificationLocation() {
        assertEquals(first, persistentWeatherLocation(listOf(first, second.copy(isDefault = true))))
    }
}

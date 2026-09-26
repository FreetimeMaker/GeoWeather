package com.freetime.geoweather.data

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class LocationBackupStoreTest {
    private val location = LocationEntity(
        id = 42,
        name = "Zurich",
        latitude = 47.3769,
        longitude = 8.5417,
        weatherData = "{\"current\":{\"temperature_2m\":20}}",
        lastUpdated = 123456,
        notificationsEnabled = true,
        notificationTime = "07:30",
        changeAlertsEnabled = true,
        changeAlertInterval = "6",
        selected = true,
        isDefault = true,
        sortOrder = 2,
        offlinePackEnabled = true
    )

    @Test
    fun backupDoesNotContainRebuildableWeatherData() {
        val raw = LocationBackupStore.encodeLocations(listOf(location))
        assertFalse(raw.contains("weatherData"))
        assertFalse(raw.contains("123456"))
        assertTrue(raw.contains("Zurich"))
    }

    @Test
    fun existingCoordinatesAreNotRestoredTwice() {
        val raw = LocationBackupStore.encodeLocations(listOf(location))
        assertTrue(LocationBackupStore.locationsToRestore(raw, listOf(location.copy(id = 7))).isEmpty())
    }

    @Test
    fun duplicateEntriesInsideBackupAreCollapsed() {
        val raw = LocationBackupStore.encodeLocations(listOf(location, location.copy(id = 99, name = "Duplicate")))
        val restored = LocationBackupStore.locationsToRestore(raw, emptyList())
        assertEquals(1, restored.size)
        assertEquals("Zurich", restored.single().name)
    }

    @Test
    fun corruptBackupIsIgnored() {
        assertTrue(LocationBackupStore.locationsToRestore("{not-json", emptyList()).isEmpty())
    }

    @Test
    fun durableLocationSettingsSurviveSerialization() {
        val raw = LocationBackupStore.encodeLocations(listOf(location))
        val restored = LocationBackupStore.locationsToRestore(raw, emptyList()).single()
        assertEquals("07:30", restored.notificationTime)
        assertTrue(restored.notificationsEnabled)
        assertTrue(restored.changeAlertsEnabled)
        assertTrue(restored.isDefault)
        assertTrue(restored.offlinePackEnabled)
        assertEquals(2, restored.sortOrder)
    }
}

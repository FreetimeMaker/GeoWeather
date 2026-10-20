package com.freetime.geoweather

import com.freetime.geoweather.data.AppSettings
import com.freetime.geoweather.data.LocationEntity
import com.russhwolf.settings.Settings
import java.lang.reflect.Proxy
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull

class StartPreferencesTest {
    private fun store(): Settings {
        val values = mutableMapOf<String, Any>()
        return Proxy.newProxyInstance(Settings::class.java.classLoader, arrayOf(Settings::class.java)) { _, method, args ->
            val arguments = args.orEmpty()
            when {
                method.name.startsWith("put") -> {
                    values[arguments[0] as String] = arguments[1]
                    null
                }
                method.name.startsWith("get") -> values[arguments[0] as String] ?: arguments.getOrNull(1)
                method.name == "hasKey" -> values.containsKey(arguments[0])
                else -> error("Unexpected settings call: ${method.name}")
            }
        } as Settings
    }

    @Test fun defaultsPreserveExistingBehavior() {
        val settings = AppSettings(store())
        assertEquals("favorite", settings.startScreen.value)
        assertEquals("compact", settings.detailLevel.value)
        assertEquals(-1L, settings.lastLocationId.value)
    }

    @Test fun preferencesUpdateFlowsAndSurviveRecreation() {
        val store = store()
        val settings = AppSettings(store)
        for (screen in listOf("favorite", "last", "overview", "current")) {
            settings.setStartScreen(screen)
            assertEquals(screen, settings.startScreen.value)
            assertEquals(screen, AppSettings(store).startScreen.value)
        }
        for (level in listOf("compact", "detailed")) {
            settings.setDetailLevel(level)
            assertEquals(level, settings.detailLevel.value)
            assertEquals(level, AppSettings(store).detailLevel.value)
        }
        settings.setLastLocationId(4_000_000_000L)
        assertEquals(4_000_000_000L, settings.lastLocationId.value)
        assertEquals(4_000_000_000L, AppSettings(store).lastLocationId.value)
    }

    @Test fun unrelatedPreferencesRemainIntact() {
        val store = store()
        val settings = AppSettings(store)
        settings.setTempUnit("fahrenheit")
        settings.setReduceMotion(true)
        settings.setStartScreen("last")
        settings.setDetailLevel("detailed")
        settings.setLastLocationId(12L)
        val restored = AppSettings(store)
        assertEquals("fahrenheit", restored.tempUnit.value)
        assertEquals(true, restored.reduceMotion.value)
    }

    @Test fun invalidSelectionsDoNotChangePreferences() {
        val settings = AppSettings(store())
        assertFailsWith<IllegalArgumentException> { settings.setStartScreen("Favorit") }
        assertFailsWith<IllegalArgumentException> { settings.setDetailLevel("Ausführlich") }
        assertEquals("favorite", settings.startScreen.value)
        assertEquals("compact", settings.detailLevel.value)
    }

    private val first = LocationEntity(1L, "First", 1.0, 2.0)
    private val favorite = LocationEntity(2L, "Favorite", 3.0, 4.0, isDefault = true)

    @Test fun favoriteWinsOverFirstAndFirstIsFallback() {
        assertEquals(Screen.Detail(2L), savedStartScreen("favorite", listOf(first, favorite), -1L))
        assertEquals(Screen.Detail(1L), savedStartScreen("favorite", listOf(first), -1L))
    }

    @Test fun lastUsesSavedIdOrFavoriteFallback() {
        assertEquals(Screen.Detail(1L), savedStartScreen("last", listOf(first, favorite), 1L))
        assertEquals(Screen.Detail(2L), savedStartScreen("last", listOf(first, favorite), 99L))
        assertEquals(Screen.Detail(1L), savedStartScreen("last", listOf(first), -1L))
    }

    @Test fun overviewAndEmptyDatabaseStayOnMain() {
        assertNull(savedStartScreen("overview", listOf(first, favorite), 1L))
        for (screen in listOf("favorite", "last", "overview", "current")) {
            assertNull(savedStartScreen(screen, emptyList(), 1L))
        }
    }

    @Test fun currentAndUnknownPreferenceFallBackToFavorite() {
        assertEquals(Screen.Detail(2L), savedStartScreen("current", listOf(first, favorite), 1L))
        assertEquals(Screen.Detail(2L), savedStartScreen("unknown", listOf(first, favorite), 1L))
    }
}

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

    @Test fun defaultsStartOnOverview() {
        val settings = AppSettings(store())
        assertEquals("overview", settings.startScreen.value)
        assertEquals("compact", settings.detailLevel.value)
        assertEquals(-1L, settings.lastLocationId.value)
    }

    @Test fun preferencesUpdateFlowsAndSurviveRecreation() {
        val store = store()
        val settings = AppSettings(store)
        for (screen in listOf("last", "overview", "current")) {
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

    @Test fun legacyAndUnknownStoredPreferencesNormalizeWithoutRewriting() {
        for (screen in listOf("favorite", "unknown", "")) {
            val store = store()
            store.putString(AppSettings.KEY_START_SCREEN, screen)
            assertEquals("overview", AppSettings(store).startScreen.value)
            assertEquals(screen, store.getString(AppSettings.KEY_START_SCREEN, "missing"))
        }
    }

    @Test fun invalidSelectionsDoNotChangePreferences() {
        val settings = AppSettings(store())
        assertFailsWith<IllegalArgumentException> { settings.setStartScreen("favorite") }
        assertFailsWith<IllegalArgumentException> { settings.setStartScreen("Favorit") }
        assertFailsWith<IllegalArgumentException> { settings.setDetailLevel("Ausführlich") }
        assertEquals("overview", settings.startScreen.value)
        assertEquals("compact", settings.detailLevel.value)
    }

    private val first = LocationEntity(1L, "First", 1.0, 2.0)
    private val legacyDefault = LocationEntity(2L, "Legacy default", 3.0, 4.0, isDefault = true)

    @Test fun lastUsesSavedIdOrFirstFallbackIgnoringLegacyDefault() {
        val locations = listOf(first, legacyDefault)
        assertEquals(Screen.Detail(1L), savedStartScreen("last", locations, 1L))
        assertEquals(Screen.Detail(2L), savedStartScreen("last", locations, 2L))
        assertEquals(Screen.Detail(1L), savedStartScreen("last", locations, 99L))
        assertEquals(Screen.Detail(1L), savedStartScreen("last", locations, -1L))
    }

    @Test fun overviewLegacyAndUnknownPreferencesStayOnMain() {
        for (screen in listOf("overview", "favorite", "unknown", "")) {
            assertNull(savedStartScreen(screen, listOf(first, legacyDefault), 2L))
        }
    }

    @Test fun emptyDatabaseStaysOnMain() {
        for (screen in listOf("favorite", "last", "overview", "current", "unknown")) {
            assertNull(savedStartScreen(screen, emptyList(), 1L))
        }
    }

    @Test fun currentFallsBackToFirstIgnoringLastIdAndLegacyDefault() {
        assertEquals(Screen.Detail(1L), savedStartScreen("current", listOf(first, legacyDefault), 2L))
    }
}

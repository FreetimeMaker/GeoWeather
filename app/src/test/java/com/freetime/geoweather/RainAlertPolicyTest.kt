package com.freetime.geoweather

import java.time.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class RainAlertPolicyTest {
    private val now = Instant.parse("2026-10-20T10:30:00Z").toEpochMilli()
    private val slot = RainAlertPolicy.Slot("2026-10-20T13:00", 80, 1.0)
    private fun select(
        slots: List<RainAlertPolicy.Slot> = listOf(slot), fresh: Boolean = true,
        quiet: Boolean = false, hour: Int = 12, sent: Long? = null, previous: Long? = null,
        zone: String? = "Europe/Berlin"
    ) = RainAlertPolicy.select(slots, zone, now, fresh, quiet, hour, 60, sent, previous)

    @Test fun `requires successful fresh refresh`() {
        assertNull(select(fresh = false))
        assertTrue(RainAlertPolicy.isFreshRefresh(now - 1000, now - 100, now, now))
        assertFalse(RainAlertPolicy.isFreshRefresh(now, now, now, now))
        assertFalse(RainAlertPolicy.isFreshRefresh(0, now, now - 1, now))
        assertFalse(RainAlertPolicy.isFreshRefresh(0, 1, now - RainAlertPolicy.MAX_REFRESH_AGE_MILLIS - 1, now))
        assertFalse(RainAlertPolicy.isFreshRefresh(0, now, now + 1, now))
    }

    @Test fun `cooldown includes clock rollback and expires after three hours`() {
        assertNull(select(sent = now - RainAlertPolicy.COOLDOWN_MILLIS + 1))
        assertNull(select(sent = now + 1))
        assertEquals(slot, select(sent = now - RainAlertPolicy.COOLDOWN_MILLIS)?.slot)
    }

    @Test fun `quiet hours suppress without consuming candidate`() {
        for (hour in listOf(22, 23, 0, 6)) assertNull(select(quiet = true, hour = hour))
        for (hour in listOf(7, 21)) assertEquals(slot, select(quiet = true, hour = hour)?.slot)
        assertEquals(slot, select(quiet = false, hour = 23)?.slot)
    }

    @Test fun `selects earliest actual future hour in location timezone`() {
        val later = slot.copy(time = "2026-10-20T14:00")
        val past = slot.copy(time = "2026-10-20T12:00")
        assertEquals(slot, select(listOf(later, past, slot))?.slot)
        assertEquals(Instant.parse("2026-10-20T11:00:00Z").toEpochMilli(), select()?.startMillis)
        assertNull(select(listOf(slot.copy(time = "2026-10-20T16:00"))))
        assertNull(select(listOf(slot.copy(time = "13:00"))))
        assertNull(select(listOf(slot.copy(time = "2026-10-20T13:15"))))
        assertNull(select(zone = null))
        assertNull(select(zone = "invalid"))
    }

    @Test fun `slot cannot repeat even when probability changes`() {
        val start = select()!!.startMillis
        assertNull(select(listOf(slot.copy(probability = 90)), previous = start))
        assertNull(select(previous = start + 3600000))
    }

    @Test fun `requires rain and threshold`() {
        assertNull(select(listOf(slot.copy(probability = 59))))
        assertNull(select(listOf(slot.copy(rain = 0.0))))
        assertNull(select(listOf(slot.copy(rain = Double.NaN))))
        assertEquals(60, select(listOf(slot.copy(probability = 60)))?.slot?.probability)
    }
}

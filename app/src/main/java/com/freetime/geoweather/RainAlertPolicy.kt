package com.freetime.geoweather

import java.time.LocalDateTime
import java.time.ZoneId

/** Pure policy for hourly forecasts; timestamps are local to the API's timezone. */
object RainAlertPolicy {
    const val COOLDOWN_MILLIS = 3 * 60 * 60 * 1000L
    const val MAX_REFRESH_AGE_MILLIS = 15 * 60 * 1000L

    data class Slot(val time: String, val probability: Int, val rain: Double)
    data class Candidate(val slot: Slot, val startMillis: Long)

    fun isFreshRefresh(previousUpdate: Long, refreshStarted: Long, updated: Long, now: Long): Boolean =
        updated > previousUpdate && updated >= refreshStarted && updated <= now &&
            now - updated <= MAX_REFRESH_AGE_MILLIS

    fun select(
        slots: List<Slot>, timezone: String?, now: Long, fresh: Boolean,
        quietHours: Boolean, deviceHour: Int, threshold: Int,
        lastSent: Long?, lastSlot: Long?
    ): Candidate? {
        if (!fresh || (quietHours && (deviceHour >= 22 || deviceHour < 7))) return null
        if (lastSent != null && (lastSent > now || now - lastSent < COOLDOWN_MILLIS)) return null
        val zone = try { ZoneId.of(timezone ?: return null) } catch (_: Exception) { return null }
        return slots.mapNotNull { slot ->
            val local = try { LocalDateTime.parse(slot.time) } catch (_: Exception) { return@mapNotNull null }
            // Do not guess across a daylight-saving gap or an ambiguous repeated hour.
            val offsets = zone.rules.getValidOffsets(local)
            if (offsets.size != 1 || local.minute != 0 || local.second != 0 || local.nano != 0) return@mapNotNull null
            val start = local.toInstant(offsets.single()).toEpochMilli()
            if (start <= now || start - now > COOLDOWN_MILLIS ||
                (lastSlot != null && start <= lastSlot) ||
                slot.probability !in threshold..100 || !slot.rain.isFinite() || slot.rain <= 0.0
            ) null else Candidate(slot, start)
        }.minByOrNull { it.startMillis }
    }
}

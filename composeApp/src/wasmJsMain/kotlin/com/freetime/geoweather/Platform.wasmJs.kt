package com.freetime.geoweather

import kotlinx.datetime.Instant

external object Date {
    fun now(): Double
}

@Suppress("NOTHING_TO_INLINE")
internal inline fun nowMillis(): Long = Date.now().toLong()

actual fun currentInstant(): Instant = Instant.fromEpochMilliseconds(nowMillis())
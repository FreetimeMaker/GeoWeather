package com.freetime.geoweather

import kotlinx.datetime.Instant

@Suppress("NOTHING_TO_INLINE")
internal inline fun nowMillis(): Long = (js("Date.now()") as Number).toLong()

actual fun currentInstant(): Instant = Instant.fromEpochMilliseconds(nowMillis())
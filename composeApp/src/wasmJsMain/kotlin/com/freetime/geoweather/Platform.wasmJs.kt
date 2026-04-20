package com.freetime.geoweather

import kotlinx.datetime.Instant

@Suppress("NOTHING_TO_INLINE")
internal inline fun nowMillis(): Long = kotlin.js.Date.now().toLong()

actual fun currentInstant(): Instant = Instant.fromEpochMilliseconds(nowMillis())
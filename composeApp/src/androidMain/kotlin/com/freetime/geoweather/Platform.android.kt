package com.freetime.geoweather

import kotlinx.datetime.Instant

actual fun currentInstant(): Instant = Instant.fromEpochMilliseconds(System.currentTimeMillis())
package io.github.freetimemaker.geoweather

import kotlinx.datetime.Instant

actual fun currentInstant(): Instant = Instant.fromEpochMilliseconds(System.currentTimeMillis())
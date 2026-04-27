package io.github.freetimemaker.geoweather

import kotlinx.datetime.Instant

fun currentInstant(): Instant = Instant.fromEpochMilliseconds(System.currentTimeMillis())

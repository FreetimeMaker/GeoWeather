package com.freetime.geoweather

import kotlinx.datetime.Clock
import kotlinx.datetime.Instant

actual fun currentInstant(): Instant = Clock.System.now()
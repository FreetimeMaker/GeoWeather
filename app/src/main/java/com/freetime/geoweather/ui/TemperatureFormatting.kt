package com.freetime.geoweather.ui

import java.util.Locale

fun temperatureValue(celsius: Double, tempUnit: String): Double =
    if (tempUnit == "fahrenheit") celsius * 9.0 / 5.0 + 32.0 else celsius

// Differences and spreads have no freezing-point offset.
fun temperatureDeltaValue(celsius: Double, tempUnit: String): Double =
    if (tempUnit == "fahrenheit") celsius * 9.0 / 5.0 else celsius

private fun temperatureSuffix(tempUnit: String): String =
    if (tempUnit == "fahrenheit") "°F" else "°C"

fun formatTemp(celsius: Double, tempUnit: String): String =
    "${temperatureValue(celsius, tempUnit).toInt()}${temperatureSuffix(tempUnit)}"

fun formatTempDecimal(celsius: Double, tempUnit: String): String =
    String.format(Locale.getDefault(), "%.1f%s", temperatureValue(celsius, tempUnit), temperatureSuffix(tempUnit))

fun formatTempDelta(celsius: Double, tempUnit: String, signed: Boolean = false): String =
    String.format(Locale.getDefault(), if (signed) "%+.1f%s" else "%.1f%s",
        temperatureDeltaValue(celsius, tempUnit), temperatureSuffix(tempUnit))

package com.freetime.geoweather

import androidx.compose.runtime.Composable

expect fun openUrl(url: String)

/**
 * Returns the platform-specific payment context (an [Any] used by the Freetime SDK).
 * On Android this is the current Activity, on other platforms it may be null.
 */
@Composable
expect fun rememberPaymentContext(): Any?

/**
 * Best last-known device coordinates, or null when unavailable
 * (no permission, no fix, or unsupported platform).
 */
expect suspend fun getCurrentCoordinates(): Pair<Double, Double>?

/**
 * Applies the in-app language ("system", "de", "en", "ru").
 * Best effort per platform; the choice is always persisted.
 */
expect fun applyAppLanguage(language: String)

/** Copies plain text to the system clipboard. */
expect fun copyToClipboard(text: String)

/**
 * Invoked by the host (Activity) on system back press.
 * Returns true when the press was consumed (a screen was popped).
 */
var systemBackHandler: (() -> Boolean)? = null

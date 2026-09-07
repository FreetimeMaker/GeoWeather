package com.freetime.geoweather

import androidx.compose.runtime.Composable

expect fun openUrl(url: String)

/**
 * Returns the platform-specific payment context (an [Any] used by the Freetime SDK).
 * On Android this is the current Activity, on other platforms it may be null.
 */
@Composable
expect fun rememberPaymentContext(): Any?

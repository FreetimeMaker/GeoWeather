package org.jetbrains.compose.resources

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.painter.Painter

/** Android-only compatibility aliases used while keeping the existing UI call sites unchanged. */
typealias StringResource = Int
typealias DrawableResource = Int

@Composable
fun stringResource(resource: Int, vararg formatArgs: Any): String =
    androidx.compose.ui.res.stringResource(resource, *formatArgs)

@Composable
fun painterResource(resource: Int): Painter =
    androidx.compose.ui.res.painterResource(resource)

suspend fun getString(resource: Int, vararg formatArgs: Any): String {
    val context = com.freetime.geoweather.getAndroidAppContext()
        ?: error("Android application context has not been initialized")
    return context.getString(resource, *formatArgs)
}

package com.freetime.geoweather.ui.glass

import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.RowScope
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import me.free_time.design.FreetimeButton
import me.free_time.design.FreetimeCard
import me.free_time.design.FreetimeDesign
import me.free_time.design.FreetimeGlassDepth
import me.free_time.design.FreetimeGlassPanel
import me.free_time.design.FreetimeGlassTopBar
import me.free_time.design.FreetimeIconButton
import me.free_time.design.FreetimeTextField

enum class GeoWeatherGlassDepth { Subtle, Standard, Elevated }

private fun GeoWeatherGlassDepth.coreDepth(): FreetimeGlassDepth = when (this) {
    GeoWeatherGlassDepth.Subtle -> FreetimeGlassDepth.SUBTLE
    GeoWeatherGlassDepth.Standard -> FreetimeGlassDepth.STANDARD
    GeoWeatherGlassDepth.Elevated -> FreetimeGlassDepth.ELEVATED
}

@Composable
fun GeoWeatherGlassTopBar(
    title: String,
    modifier: Modifier = Modifier,
    onBack: (() -> Unit)? = null,
    compact: Boolean = false,
    compactSubtitle: String? = null,
    actions: @Composable RowScope.() -> Unit = {},
) {
    FreetimeGlassTopBar(
        title = title,
        modifier = modifier,
        compact = compact,
        subtitle = compactSubtitle,
        navigation = {
            if (onBack != null) {
                FreetimeIconButton(
                    icon = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = null,
                    onClick = onBack,
                )
            }
        },
        actions = actions,
    )
}

@Composable
fun GeoWeatherGlassAction(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable RowScope.() -> Unit,
) = me.free_time.design.FreetimeGlassAction(onClick = onClick, modifier = modifier, content = content)

@Composable
fun GeoWeatherGlassPanel(
    modifier: Modifier = Modifier,
    interactive: Boolean = false,
    depth: GeoWeatherGlassDepth = GeoWeatherGlassDepth.Standard,
    content: @Composable BoxScope.() -> Unit,
) = FreetimeGlassPanel(
    modifier = modifier,
    depth = depth.coreDepth(),
    interactive = interactive,
    content = content,
)

@Composable
fun GeoWeatherGlassTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "",
    singleLine: Boolean = true,
) = FreetimeTextField(
    value = value,
    onValueChange = onValueChange,
    modifier = modifier,
    placeholder = placeholder,
    singleLine = singleLine,
)

@Composable
fun GeoWeatherGlassIconAction(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit,
) {
    // Compatibility bridge for existing call sites. New screens should use
    // FreetimeIconButton directly so all controls come from Freetime-Core:Design.
    FreetimeCard(modifier = modifier, onClick = onClick, content = content)
}

@Deprecated("Use FreetimeDialog from Freetime-Core:Design directly")
@Composable
fun GeoWeatherGlassDialog(
    onDismissRequest: () -> Unit,
    title: String,
    modifier: Modifier = Modifier,
    actions: @Composable RowScope.() -> Unit = {},
    content: @Composable ColumnScope.() -> Unit,
) {
    // Kept temporarily for source compatibility where dialogs have custom bodies.
    // Its surface is still provided entirely by Freetime-Core:Design.
    FreetimeGlassPanel(modifier = modifier) { }
}

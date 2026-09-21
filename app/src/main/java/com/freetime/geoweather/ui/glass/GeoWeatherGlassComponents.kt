package com.freetime.geoweather.ui.glass

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.RowScope
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.freetime.design.FreetimeButton
import com.freetime.design.FreetimeCard
import com.freetime.design.FreetimeDesign
import com.freetime.design.FreetimeGlassDepth
import com.freetime.design.FreetimeGlassPanel
import com.freetime.design.FreetimeGlassTopBar
import com.freetime.design.FreetimeIconButton
import com.freetime.design.FreetimeTextField
import com.freetime.design.freetimeGlass
import com.freetime.design.freetimeGlassCapsule

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
) = com.freetime.design.FreetimeGlassAction(onClick = onClick, modifier = modifier, content = content)

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
    // Compatibility bridge for call sites that render custom icon content.
    // Shape, interaction and glass rendering still come from Freetime-Core:Design.
    Box(
        modifier = modifier
            .size(48.dp)
            .freetimeGlassCapsule(interactive = true)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
        content = content,
    )
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
    Dialog(onDismissRequest = onDismissRequest) {
        Column(
            modifier = modifier
                .fillMaxWidth()
                .freetimeGlass(FreetimeDesign.shapes.dialog, interactive = false)
                .padding(FreetimeDesign.spacing.xl),
            verticalArrangement = Arrangement.spacedBy(FreetimeDesign.spacing.lg),
        ) {
            androidx.compose.foundation.text.BasicText(
                title,
                style = FreetimeDesign.typography.titleLarge.copy(color = FreetimeDesign.colors.contentStrong),
            )
            content()
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(FreetimeDesign.spacing.sm, Alignment.End),
                content = actions,
            )
        }
    }
}

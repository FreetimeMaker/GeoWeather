package com.freetime.geoweather.ui
import com.freetime.design.FreetimeDesign
import com.freetime.design.FreetimeGlassAction
import com.freetime.design.FreetimeGlassTitle
import com.freetime.design.FreetimeText
import com.freetime.design.FreetimeScreen

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.freetime.geoweather.R as Res
import com.freetime.design.freetimeGlass
import com.freetime.design.FreetimeButton
import com.freetime.design.FreetimeCard
import com.freetime.design.FreetimeProgressIndicator

@Composable
fun OnboardingScreen(onDone: () -> Unit) {
    var page by remember { mutableIntStateOf(0) }
    val titles = listOf(Res.string.onboarding_weather, Res.string.onboarding_alerts, Res.string.onboarding_account)
    val texts = listOf(Res.string.onboarding_weather_desc, Res.string.onboarding_alerts_desc, Res.string.onboarding_account_desc)
    Box(Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
        FreetimeCard(modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            FreetimeGlassTitle(stringResource(titles[page]), maxLines = 2)
            FreetimeText(stringResource(texts[page]))
            FreetimeProgressIndicator(progress = (page + 1) / 3f, modifier = Modifier.fillMaxWidth())
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                FreetimeButton(text = stringResource(Res.string.skip), onClick = onDone)
                FreetimeGlassAction(onClick = { if (page < 2) page++ else onDone() }) {
                    FreetimeText(stringResource(if (page < 2) Res.string.next else Res.string.done))
                }
            }
            }
        }
    }
}

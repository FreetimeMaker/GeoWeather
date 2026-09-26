package com.freetime.geoweather.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.freetime.design.liquidGlass
import com.freetime.geoweather.R as Res

@Composable
fun OnboardingScreen(onDone: () -> Unit) {
    var page by remember { mutableIntStateOf(0) }
    val titles = listOf(Res.string.onboarding_weather, Res.string.onboarding_alerts, Res.string.onboarding_account)
    val texts = listOf(Res.string.onboarding_weather_desc, Res.string.onboarding_alerts_desc, Res.string.onboarding_account_desc)

    Box(Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .liquidGlass(interactive = false)
        ) {
            Column(
                Modifier.fillMaxWidth().padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = stringResource(titles[page]),
                    style = MaterialTheme.typography.headlineMedium,
                    maxLines = 2
                )
                Text(
                    text = stringResource(texts[page]),
                    style = MaterialTheme.typography.bodyLarge
                )
                LinearProgressIndicator(
                    progress = { (page + 1) / 3f },
                    modifier = Modifier.fillMaxWidth()
                )
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Button(onClick = onDone) {
                        Text(stringResource(Res.string.skip))
                    }
                    Button(onClick = { if (page < 2) page++ else onDone() }) {
                        Text(stringResource(if (page < 2) Res.string.next else Res.string.done))
                    }
                }
            }
        }
    }
}

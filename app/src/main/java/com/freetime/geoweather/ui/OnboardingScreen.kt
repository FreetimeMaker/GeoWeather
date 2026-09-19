package com.freetime.geoweather.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.freetime.geoweather.R as Res
import com.freetime.geoweather.ui.glass.geoWeatherGlass

@Composable
fun OnboardingScreen(onDone: () -> Unit) {
    var page by remember { mutableIntStateOf(0) }
    val titles = listOf(Res.string.onboarding_weather, Res.string.onboarding_alerts, Res.string.onboarding_account)
    val texts = listOf(Res.string.onboarding_weather_desc, Res.string.onboarding_alerts_desc, Res.string.onboarding_account_desc)
    Box(Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
        Column(Modifier.fillMaxWidth().geoWeatherGlass(RoundedCornerShape(32.dp), interactive = false).padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text(stringResource(titles[page]), style = MaterialTheme.typography.headlineMedium)
            Text(stringResource(texts[page]), style = MaterialTheme.typography.bodyLarge)
            LinearProgressIndicator(progress = { (page + 1) / 3f }, modifier = Modifier.fillMaxWidth())
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                TextButton(onClick = onDone) { Text(stringResource(Res.string.skip)) }
                Button(onClick = { if (page < 2) page++ else onDone() }, colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent), modifier = Modifier.geoWeatherGlass(RoundedCornerShape(22.dp))) {
                    Text(stringResource(if (page < 2) Res.string.next else Res.string.done))
                }
            }
        }
    }
}

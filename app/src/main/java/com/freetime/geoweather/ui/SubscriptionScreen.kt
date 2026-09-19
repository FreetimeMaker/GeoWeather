package com.freetime.geoweather.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.freetime.geoweather.AppwriteData
import com.freetime.geoweather.SubscriptionPlans
import com.freetime.geoweather.R as Res
import com.freetime.geoweather.ui.glass.geoWeatherGlass

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SubscriptionScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    var current by remember { mutableStateOf("free") }
    var plans by remember { mutableStateOf(emptyMap<String, com.freetime.geoweather.SubscriptionPlan>()) }
    var loading by remember { mutableStateOf(true) }

    suspend fun refresh() {
        current = runCatching { AppwriteData.account(context).subscription.lowercase() }.getOrDefault("free")
        SubscriptionPlans.cacheSubscription(context, current)
        plans = SubscriptionPlans.load()
        loading = false
    }
    LaunchedEffect(Unit) { refresh() }

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                    .geoWeatherGlass(RoundedCornerShape(28.dp), interactive = false),
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
                title = { Text(stringResource(Res.string.subscription_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(Res.string.back_nav_desc))
                    }
                }
            )
        }
    ) { padding ->
        Column(
            Modifier.padding(padding).padding(16.dp).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (loading) LinearProgressIndicator(Modifier.fillMaxWidth())
            listOf("free", "freemium", "premium", "ultrimium").forEach { key ->
                val plan = SubscriptionPlans.planFor(plans, key)
                Card(
                    modifier = Modifier.fillMaxWidth().geoWeatherGlass(RoundedCornerShape(24.dp), interactive = false),
                    colors = CardDefaults.cardColors(containerColor = Color.Transparent)
                ) {
                    Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(key.uppercase(), style = MaterialTheme.typography.titleLarge)
                            if (key == current) AssistChip(onClick = {}, label = { Text(stringResource(Res.string.current_plan)) })
                        }
                        Text(stringResource(Res.string.plan_locations, plan.maxLocations))
                        Text(stringResource(Res.string.plan_forecast_days, plan.forecastDays))
                        Text(stringResource(if (plan.notifications) Res.string.plan_notifications_on else Res.string.plan_notifications_off))
                    }
                }
            }
            OutlinedButton(onClick = { loading = true }, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(Res.string.refresh_nav_desc))
            }
            if (loading) LaunchedEffect(loading) { refresh() }
        }
    }
}

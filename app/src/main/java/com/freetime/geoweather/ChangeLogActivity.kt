package com.freetime.geoweather

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.freetime.geoweather.ui.theme.GeoWeatherTheme

class ChangeLogActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            GeoWeatherTheme {
                ChangeLogScreen(onBack = { finish() })
            }
        }
    }
}

@Composable
fun ReleaseCard(
    version: String,
    details: List<String>,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
    ) {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.Start
        ) {
            Text(
                text = version,
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(8.dp))

            details.forEach { line ->
                Text(
                    text = "• $line",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChangeLogScreen(onBack: () -> Unit) {
    val releases = listOf(
        "v1.3.4" to listOf(
            "${stringResource(R.string.added_label)} ${stringResource(R.string.cl_material_you)}",
            stringResource(R.string.cl_unit_switching),
            stringResource(R.string.cl_multi_service),
            stringResource(R.string.cl_historical_data),
            stringResource(R.string.cl_forecast_enhanced),
            stringResource(R.string.cl_custom_notifs),
            stringResource(R.string.cl_widget_refresh),
            stringResource(R.string.cl_localization),
            stringResource(R.string.cl_moon_phase)
        ),
        "v1.3.3" to listOf(
            "${stringResource(R.string.added_label)} ${stringResource(R.string.cl_coordinate_search)}",
            stringResource(R.string.cl_state_in_search),
            stringResource(R.string.cl_material_you)
        ),
        "v1.3.2" to listOf(
            "${stringResource(R.string.added_label)} ${stringResource(R.string.AddedMoreDons)}",
            "${stringResource(R.string.fixed_label)} ${stringResource(R.string.HopefullyFixedFMSDK)}"
        ),
        "v1.3.1" to listOf(
            "${stringResource(R.string.fixed_label)} ${stringResource(R.string.FixActivities)}",
            stringResource(R.string.AddedMoonData),
            "${stringResource(R.string.removed_label)} ${stringResource(R.string.changelog_remove_api_key)}"
        ),
        "v1.3.0" to listOf(
            "${stringResource(R.string.added_label)} ${stringResource(R.string.AddedMoonData)}",
            "${stringResource(R.string.fixed_label)} ${stringResource(R.string.FixFMSDK)}",
            "${stringResource(R.string.removed_label)} ${stringResource(R.string.changelog_remove_coin)}"
        )
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.whats_new_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back_nav_desc))
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            releases.forEach { (version, details) ->
                ReleaseCard(version = version, details = details)
            }
        }
    }
}

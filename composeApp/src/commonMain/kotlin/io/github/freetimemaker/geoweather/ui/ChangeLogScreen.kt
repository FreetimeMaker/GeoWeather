package io.github.freetimemaker.geoweather.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import geoweather.composeapp.generated.resources.*
import org.jetbrains.compose.resources.stringResource

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
        "v1.3.6" to listOf(
            "${stringResource(Res.string.added_label)} ${stringResource(Res.string.cl_v136_auto_open)}",
            "${stringResource(Res.string.added_label)} ${stringResource(Res.string.cl_v136_default_location)}",
            "${stringResource(Res.string.added_label)} ${stringResource(Res.string.cl_v136_current_location)}",
            "${stringResource(Res.string.removed_label)} ${stringResource(Res.string.cl_v136_degoogled)}",
            "${stringResource(Res.string.added_label)} ${stringResource(Res.string.cl_v136_ui_ux)}",
            "${stringResource(Res.string.fixed_label)} ${stringResource(Res.string.cl_v136_stability)}"
        ),
        "v1.3.5" to listOf(
            "${stringResource(Res.string.added_label)} ${stringResource(Res.string.cl_v135_added_dons)}",
            "${stringResource(Res.string.fixed_label)} ${stringResource(Res.string.cl_v135_fixed_system_theme)}"
        ),
        "v1.3.4" to listOf(
            "${stringResource(Res.string.added_label)} ${stringResource(Res.string.cl_material_you)}",
            stringResource(Res.string.cl_unit_switching),
            stringResource(Res.string.cl_multi_service),
            stringResource(Res.string.cl_historical_data),
            stringResource(Res.string.cl_forecast_enhanced),
            stringResource(Res.string.cl_custom_notifs),
            stringResource(Res.string.cl_widget_refresh),
            stringResource(Res.string.cl_localization),
            stringResource(Res.string.cl_moon_phase)
        ),
        "v1.3.3" to listOf(
            "${stringResource(Res.string.added_label)} ${stringResource(Res.string.cl_coordinate_search)}",
            stringResource(Res.string.cl_state_in_search),
            stringResource(Res.string.cl_material_you)
        ),
        "v1.3.2" to listOf(
            "${stringResource(Res.string.added_label)} ${stringResource(Res.string.AddedMoreDons)}",
            "${stringResource(Res.string.fixed_label)} ${stringResource(Res.string.HopefullyFixedFMSDK)}"
        ),
        "v1.3.1" to listOf(
            "${stringResource(Res.string.fixed_label)} ${stringResource(Res.string.FixActivities)}",
            stringResource(Res.string.AddedMoonData),
            "${stringResource(Res.string.removed_label)} ${stringResource(Res.string.changelog_remove_api_key)}"
        ),
        "v1.3.0" to listOf(
            "${stringResource(Res.string.added_label)} ${stringResource(Res.string.AddedMoonData)}",
            "${stringResource(Res.string.fixed_label)} ${stringResource(Res.string.FixFMSDK)}",
            "${stringResource(Res.string.removed_label)} ${stringResource(Res.string.changelog_remove_coin)}"
        )
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(Res.string.whats_new_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(Res.string.back_nav_desc))
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

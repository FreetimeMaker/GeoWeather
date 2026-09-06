package com.freetime.geoweather.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import geoweather.shared.generated.resources.Res
import geoweather.shared.generated.resources.back_nav_desc
import org.jetbrains.compose.resources.stringResource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChangeLogScreen(onBack: () -> Unit) {
    val releases = listOf(
        "v2.3.0" to listOf(
            "Responsive hourly weather forecast",
            "Improved weather widget layout",
            "Integrated weather radar",
            "Expanded 16-day forecast",
            "Improved translations and stability"
        ),
        "v2.2.2" to listOf(
            "Expandable 16-day forecast",
            "Compact weather details grid",
            "Wind speed and cardinal direction support"
        )
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("What's new") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(Res.string.back_nav_desc)
                        )
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            releases.forEach { (version, changes) ->
                Card(modifier = Modifier.fillMaxSize()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(version, style = MaterialTheme.typography.titleLarge)
                        changes.forEach { change ->
                            Text("- $change", style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            }
        }
    }
}

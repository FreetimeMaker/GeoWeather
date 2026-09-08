package com.freetime.geoweather.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import geoweather.shared.generated.resources.*
import org.jetbrains.compose.resources.stringResource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutScreen(onBack: () -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(Res.string.about_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(Res.string.back_nav_desc))
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = stringResource(Res.string.app_name),
                style = MaterialTheme.typography.headlineLarge
            )
            Text(
                text = "v3.0.0 (KMP)",
                style = MaterialTheme.typography.bodySmall
            )
            
            Spacer(Modifier.height(16.dp))
            
            Text(
                text = stringResource(Res.string.about_developer_title),
                style = MaterialTheme.typography.titleMedium
            )
            Text(
                text = stringResource(Res.string.about_developer_text),
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.bodyMedium
            )
            
            Spacer(Modifier.weight(1f))
            
            Text(
                text = "© 2026 Freetime Maker",
                style = MaterialTheme.typography.labelSmall
            )
        }
    }
}

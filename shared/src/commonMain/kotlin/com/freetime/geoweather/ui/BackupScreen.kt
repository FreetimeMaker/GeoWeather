package com.freetime.geoweather.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.freetime.geoweather.Platform
import com.freetime.geoweather.data.BackupManager
import com.freetime.geoweather.data.LocationEntity
import geoweather.shared.generated.resources.Res
import geoweather.shared.generated.resources.back_nav_desc
import org.jetbrains.compose.resources.stringResource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BackupScreen(
    locations: List<LocationEntity>,
    platform: Platform,
    onImport: (String) -> Unit,
    onBack: () -> Unit
) {
    var backupText by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Backup & Restore") },
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
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Button(
                onClick = { platform.copyToClipboard(BackupManager.exportLocations(locations)) },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Copy locations backup")
            }
            OutlinedTextField(
                value = backupText,
                onValueChange = { backupText = it },
                modifier = Modifier.fillMaxWidth().weight(1f),
                label = { Text("Paste backup JSON") }
            )
            Button(
                onClick = { onImport(backupText) },
                enabled = backupText.isNotBlank(),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Restore locations")
            }
        }
    }
}
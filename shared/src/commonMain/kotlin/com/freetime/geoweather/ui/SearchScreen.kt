package com.freetime.geoweather.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import geoweather.shared.generated.resources.*
import org.jetbrains.compose.resources.stringResource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(
    viewModel: WeatherViewModel,
    onCitySelected: () -> Unit
) {
    var query by remember { mutableStateOf("") }
    val results by viewModel.searchResults.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(title = { Text(stringResource(Res.string.search_title)) })
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).padding(16.dp)) {
            OutlinedTextField(
                value = query,
                onValueChange = { 
                    query = it
                    if (it.length > 2) viewModel.searchCity(it)
                },
                modifier = Modifier.fillMaxWidth(),
                label = { Text(stringResource(Res.string.search_hint)) },
                trailingIcon = { Icon(Icons.Default.Search, null) }
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            LazyColumn {
                items(results) { city ->
                    ListItem(
                        headlineContent = { Text(city.name) },
                        supportingContent = { Text("${city.admin1 ?: ""}, ${city.country ?: ""}") },
                        modifier = Modifier.clickable {
                            viewModel.addLocation(city)
                            onCitySelected()
                        }
                    )
                    HorizontalDivider()
                }
            }
        }
    }
}

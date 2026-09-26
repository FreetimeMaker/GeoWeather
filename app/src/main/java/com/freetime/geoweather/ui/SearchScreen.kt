package com.freetime.geoweather.ui
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.freetime.geoweather.R as Res
import com.freetime.design.liquidGlass
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(
    viewModel: WeatherViewModel,
    onCitySelected: () -> Unit,
    onBack: () -> Unit = onCitySelected
) {
    var query by remember { mutableStateOf("") }
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("search_history", android.content.Context.MODE_PRIVATE) }
    val savedLocations by viewModel.locations.collectAsState()
    var recentSearches by remember {
        mutableStateOf(prefs.getStringSet("queries", emptySet()).orEmpty().toList().take(5))
    }
    fun rememberQuery(value: String) {
        if (value.isBlank()) return
        recentSearches = (listOf(value.trim()) + recentSearches.filterNot { it.equals(value.trim(), true) }).take(5)
        prefs.edit().putStringSet("queries", recentSearches.toSet()).apply()
    }
    val results by viewModel.searchResults.collectAsState()
    val isSearching by viewModel.isSearching.collectAsState()

    DisposableEffect(Unit) {
        onDispose { viewModel.clearSearch() }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(Res.string.search_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                    }
                },
                modifier = Modifier.liquidGlass(interactive = false)
            )
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).padding(16.dp)) {
            OutlinedTextField(
                value = query,
                onValueChange = {
                    query = it
                    viewModel.searchCity(it)
                },
                modifier = Modifier.fillMaxWidth().liquidGlass(),
                placeholder = { Text(stringResource(Res.string.search_placeholder)) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                singleLine = true
            )

            Spacer(modifier = Modifier.height(12.dp))
            if (query.isBlank()) {
                Text(text = stringResource(Res.string.quick_actions_title))
                Spacer(modifier = Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    AssistChip(label = { Text(stringResource(Res.string.command_refresh_weather)) }, onClick = {
                            kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Main).launch {
                                viewModel.refreshAllLocations()
                            }
                        }
                    )
                    AssistChip(label = { Text(stringResource(Res.string.command_saved_locations)) }, onClick = onBack)
                }
                Spacer(modifier = Modifier.height(12.dp))
            }
            if (query.isBlank() && savedLocations.isNotEmpty()) {
                Text(text = stringResource(Res.string.saved_places))
                Spacer(modifier = Modifier.height(8.dp))
                androidx.compose.foundation.lazy.LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(savedLocations.take(8), key = { "saved-search-" + it.id }) { loc ->
                        AssistChip(label = { Text(loc.name) }, onClick = { query = loc.name; viewModel.searchCity(loc.name) })
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
            }
            if (recentSearches.isNotEmpty() && query.isBlank()) {
                Text(text = stringResource(Res.string.recent_searches))
                Spacer(modifier = Modifier.height(8.dp))
                androidx.compose.foundation.lazy.LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(recentSearches) { recent ->
                        AssistChip(label = { Text(recent) }, onClick = { query = recent; viewModel.searchCity(recent) })
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
            }

            when {
                isSearching -> { LinearProgressIndicator(modifier = Modifier.fillMaxWidth()) }
                query.isNotBlank() && results.isEmpty() -> { Text(stringResource(Res.string.search_no_results), modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center) }
                else -> {
                    LazyColumn {
                        items(results, key = { "${it.latitude},${it.longitude}" }) { city ->
                            ListItem(
                                headlineContent = { Text(city.name) },
                                supportingContent = { Text(listOfNotNull(city.admin1, city.country).filter { it.isNotBlank() }.joinToString(" · ").ifBlank { "${city.latitude}, ${city.longitude}" }) },
                                modifier = Modifier.padding(vertical = 4.dp).clickable {
                                    rememberQuery(city.name)
                                    viewModel.addLocation(city)
                                    onCitySelected()
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

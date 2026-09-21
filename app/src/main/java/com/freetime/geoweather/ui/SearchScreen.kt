package com.freetime.geoweather.ui
import com.freetime.design.FreetimeIconButton
import com.freetime.design.FreetimeScaffold
import com.freetime.design.FreetimeDesign
import com.freetime.design.FreetimeGlassTopBar
import com.freetime.design.FreetimeGlassSearchField
import com.freetime.design.FreetimeLoadingState
import com.freetime.design.FreetimeEmptyState
import com.freetime.design.FreetimeSectionHeader
import com.freetime.design.FreetimeListItem
import com.freetime.design.freetimeGlass

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Search
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.freetime.geoweather.R as Res
import com.freetime.design.freetimeGlass
import com.freetime.design.FreetimeGlassTopBar
import com.freetime.design.FreetimeTextField
import kotlinx.coroutines.launch
import com.freetime.design.FreetimeChip
import com.freetime.design.FreetimeCard
import com.freetime.design.FreetimeProgressIndicator

@Composable
fun SearchScreen(
    viewModel: WeatherViewModel,
    onCitySelected: () -> Unit,
    onBack: () -> Unit = onCitySelected
) {
    var query by remember { mutableStateOf("") }
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("search_history", android.content.Context.MODE_PRIVATE) }
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

    FreetimeScaffold(
        topBar = {
            FreetimeGlassTopBar(
                title = stringResource(Res.string.search_title),
                navigation = { FreetimeIconButton(icon = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null, onClick = onBack) }
            )
        }
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            FreetimeGlassSearchField(
                value = query,
                onValueChange = {
                    query = it
                    viewModel.searchCity(it)
                },
                modifier = Modifier.fillMaxWidth(),
                placeholder = stringResource(Res.string.search_placeholder)
            )

            Spacer(modifier = Modifier.height(12.dp))
            if (query.isBlank()) {
                FreetimeSectionHeader(title = stringResource(Res.string.quick_actions_title))
                Spacer(modifier = Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FreetimeChip(
                        text = stringResource(Res.string.command_refresh_weather),
                        onClick = {
                            kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Main).launch {
                                viewModel.refreshAllLocations()
                            }
                        }
                    )
                    FreetimeChip(text = stringResource(Res.string.command_saved_locations), onClick = onBack)
                }
                Spacer(modifier = Modifier.height(12.dp))
            }
            if (recentSearches.isNotEmpty() && query.isBlank()) {
                FreetimeSectionHeader(title = stringResource(Res.string.recent_searches))
                Spacer(modifier = Modifier.height(8.dp))
                androidx.compose.foundation.lazy.LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(recentSearches) { recent ->
                        FreetimeChip(text = recent, onClick = { query = recent; viewModel.searchCity(recent) })
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
            }

            when {
                isSearching -> { FreetimeLoadingState(modifier = Modifier.fillMaxWidth()) }
                query.isNotBlank() && results.isEmpty() -> { FreetimeEmptyState(title = stringResource(Res.string.search_no_results), modifier = Modifier.fillMaxWidth()) }
                else -> {
                    LazyColumn {
                        items(results, key = { "${it.latitude},${it.longitude}" }) { city ->
                            FreetimeListItem(
                                title = city.name,
                                subtitle = "${city.latitude}, ${city.longitude}",
                                modifier = Modifier.padding(vertical = 4.dp),
                                onClick = {
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

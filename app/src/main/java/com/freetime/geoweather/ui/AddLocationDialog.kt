package com.freetime.geoweather.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.freetime.geoweather.data.LocationEntity
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.util.*

data class LocationSearchResult(
    val name: String,
    val country: String,
    val admin1: String?,
    val latitude: Double,
    val longitude: Double
) {
    fun getDisplayName(): String {
        return if (admin1?.isNotEmpty() == true) {
            "$name, $admin1, $country"
        } else {
            "$name, $country"
        }
    }
}

@Composable
fun AddLocationDialog(
    onDismiss: () -> Unit,
    onLocationSelected: (LocationEntity) -> Unit
) {
    val context = LocalContext.current
    var searchQuery by remember { mutableStateOf("") }
    var searchResults by remember { mutableStateOf<List<LocationSearchResult>>(emptyList()) }
    var isLoading by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.8f)
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                Text(
                    text = "Search for a City and Add it",
                    style = MaterialTheme.typography.headlineSmall,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    label = { Text("Enter City (Berlin, Paris, New York)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = {
                        if (searchQuery.isNotBlank()) {
                            isLoading = true
                            searchResults = emptyList()

                            scope.launch {
                                try {
                                    val geoUrl = "https://geocoding-api.open-meteo.com/v1/search?name=" +
                                            URLEncoder.encode(searchQuery, "UTF-8") +
                                            "&count=20&language=${Locale.getDefault().language}&format=json"

                                    val geoJson = httpGet(geoUrl, "GeoWeatherApp")
                                    val geoObj = JSONObject(geoJson)
                                    val results = geoObj.optJSONArray("results")

                                    if (results != null && results.length() > 0) {
                                        val newResults = mutableListOf<LocationSearchResult>()
                                        for (i in 0 until results.length()) {
                                            val item = results.getJSONObject(i)
                                            newResults.add(
                                                LocationSearchResult(
                                                    name = item.getString("name"),
                                                    country = item.optString("country", ""),
                                                    admin1 = item.optString("admin1", ""),
                                                    latitude = item.getDouble("latitude"),
                                                    longitude = item.getDouble("longitude")
                                                )
                                            )
                                        }
                                        searchResults = newResults
                                    }
                                } catch (e: Exception) {
                                    // Handle error
                                } finally {
                                    isLoading = false
                                }
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = searchQuery.isNotBlank() && !isLoading
                ) {
                    Text("Search")
                }

                Spacer(modifier = Modifier.height(16.dp))

                if (isLoading) {
                    Box(
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(searchResults) { result ->
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        val location = LocationEntity(
                                            name = result.getDisplayName(),
                                            latitude = result.latitude,
                                            longitude = result.longitude
                                        )
                                        onLocationSelected(location)
                                        onDismiss()
                                    }
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Text(
                                        text = result.name,
                                        style = MaterialTheme.typography.titleMedium
                                    )
                                    Text(
                                        text = "${result.country}${result.admin1?.let { ", $it" } ?: ""} (${String.format("%.4f, %.4f", result.latitude, result.longitude)})",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.weight(1f))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel")
                    }
                }
            }
        }
    }
}

private fun httpGet(urlString: String, userAgent: String): String {
    val url = URL(urlString)
    val connection = url.openConnection() as HttpURLConnection
    connection.setRequestProperty("User-Agent", userAgent)
    connection.connectTimeout = 12000
    connection.readTimeout = 12000

    return connection.inputStream.bufferedReader(StandardCharsets.UTF_8).use { it.readText() }
}
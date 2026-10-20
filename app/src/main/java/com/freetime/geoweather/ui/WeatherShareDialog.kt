package com.freetime.geoweather.ui

import android.text.format.DateFormat
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.freetime.geoweather.R
import java.util.Date
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** updatedAt is the weather data timestamp in Unix milliseconds, not the share time. */
@Composable
fun WeatherShareDialog(
    locationName: String,
    temperature: String,
    description: String,
    updatedAt: Long,
    forecastDetails: List<String>,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var title by rememberSaveable { mutableStateOf("") }
    var hideLocation by rememberSaveable { mutableStateOf(false) }
    var busy by remember { mutableStateOf(false) }
    var failed by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = { if (!busy) onDismiss() },
        title = { Text(stringResource(R.string.weather_share_dialog_title)) },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text(stringResource(R.string.weather_share_custom_title)) },
                    placeholder = { Text(stringResource(R.string.weather_share_title_example)) },
                    enabled = !busy,
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Row(
                    modifier = Modifier.fillMaxWidth().toggleable(
                        value = hideLocation,
                        enabled = !busy,
                        role = Role.Checkbox,
                        onValueChange = { hideLocation = it }
                    ),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(checked = hideLocation, onCheckedChange = null, enabled = !busy)
                    Text(stringResource(R.string.weather_share_hide_location))
                }
                Text(stringResource(R.string.weather_share_privacy_hint))
                if (busy) CircularProgressIndicator()
                if (failed) Text(
                    stringResource(R.string.weather_share_error),
                    color = MaterialTheme.colorScheme.error
                )
            }
        },
        confirmButton = {
            TextButton(enabled = !busy, onClick = {
                busy = true
                failed = false
                // Snapshot and sanitize once: neither output can accidentally use the raw location.
                val redact: (String) -> String = { value ->
                    if (hideLocation && locationName.isNotBlank()) {
                        value.replace(locationName, "", ignoreCase = true).trim()
                    } else value
                }
                val cardTitle = redact(title.trim()).ifBlank {
                    if (hideLocation || locationName.isBlank()) {
                        context.getString(R.string.weather_share_default_title)
                    } else locationName
                }
                val safeTemperature = redact(temperature)
                val safeDescription = redact(description)
                val safeDetails = forecastDetails.map(redact).filter { it.isNotBlank() }
                val visibleLocation = if (!hideLocation && locationName.isNotBlank() && cardTitle != locationName) {
                    listOf(locationName)
                } else emptyList()
                scope.launch {
                    try {
                        val date = if (updatedAt > 0) {
                            val instant = Date(updatedAt)
                            context.getString(
                                R.string.weather_share_updated_at,
                                DateFormat.getDateFormat(context).format(instant),
                                DateFormat.getTimeFormat(context).format(instant)
                            )
                        } else context.getString(R.string.weather_share_updated_unknown)
                        val source = context.getString(R.string.weather_share_source)
                        val details = visibleLocation + safeDetails
                        val text = (listOf(cardTitle, safeTemperature, safeDescription, date) + details + source)
                            .filter { it.isNotBlank() }.joinToString("\n")
                        // Keep ownership across cancellation at the dispatcher boundary.
                        var bitmap: android.graphics.Bitmap? = null
                        try {
                            withContext(Dispatchers.Default) {
                                bitmap = createWeatherShareCard(
                                    cardTitle, safeTemperature, safeDescription, date, details, source
                                )
                            }
                            sendWeatherShareCard(context, checkNotNull(bitmap), text)
                        } finally {
                            bitmap?.recycle()
                        }
                        onDismiss()
                    } catch (error: CancellationException) {
                        throw error
                    } catch (error: Exception) {
                        failed = true
                    } finally {
                        busy = false
                    }
                }
            }) { Text(stringResource(R.string.weather_share_send)) }
        },
        dismissButton = {
            TextButton(enabled = !busy, onClick = onDismiss) {
                Text(stringResource(R.string.weather_share_cancel))
            }
        }
    )
}

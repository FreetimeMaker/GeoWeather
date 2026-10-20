package com.freetime.geoweather.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.produceState
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.freetime.geoweather.R
import com.freetime.geoweather.isNetworkAvailable
import kotlinx.coroutines.delay
import java.text.DateFormat
import java.util.Date

@Composable
fun rememberWeatherClock(): State<Long> = produceState(System.currentTimeMillis()) {
    while (true) {
        value = System.currentTimeMillis()
        delay(30_000)
    }
}

@Composable
fun WeatherDataStatus(lastUpdated: Long, nowMillis: Long) {
    val online = produceState(isNetworkAvailable()) {
        while (true) {
            value = isNetworkAvailable()
            delay(10_000)
        }
    }.value
    val validTimestamp = lastUpdated > 0 && lastUpdated <= nowMillis
    val stale = !validTimestamp || nowMillis - lastUpdated > 2 * 60 * 60 * 1000L
    val stamp = if (validTimestamp) DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT)
        .format(Date(lastUpdated)) else stringResource(R.string.data_status_unknown)
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp)) {
            Text(stringResource(R.string.data_status_updated, stamp), style = MaterialTheme.typography.labelMedium)
            if (!online) Text(stringResource(R.string.data_status_offline), style = MaterialTheme.typography.bodyMedium)
            if (stale) Text(stringResource(R.string.data_status_stale), style = MaterialTheme.typography.bodyMedium)
        }
    }
}

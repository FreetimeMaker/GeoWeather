package com.freetime.geoweather

import android.content.SharedPreferences
import androidx.compose.runtime.*

@Composable
fun SharedPreferences.collectAsState(key: String, defaultValue: Boolean): State<Boolean> {
    val state = remember { mutableStateOf(getBoolean(key, defaultValue)) }
    DisposableEffect(this, key) {
        val listener = SharedPreferences.OnSharedPreferenceChangeListener { prefs, k ->
            if (k == key) state.value = prefs.getBoolean(key, defaultValue)
        }
        registerOnSharedPreferenceChangeListener(listener)
        onDispose { unregisterOnSharedPreferenceChangeListener(listener) }
    }
    return state
}

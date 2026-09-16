package com.freetime.geoweather

import androidx.compose.runtime.*
import com.russhwolf.settings.Settings
import com.russhwolf.settings.get

@Composable
fun Settings.collectAsState(key: String, defaultValue: Boolean): State<Boolean> {
    var value by remember { mutableStateOf(getBoolean(key, defaultValue)) }
    
    // In a real app, we might use a Flow from Settings if available
    // multiplatform-settings-coroutines provides Flow support
    
    return remember(value) { derivedStateOf { value } }
}

@Composable
fun Settings.collectStringAsState(key: String, defaultValue: String): State<String> {
    var value by remember { mutableStateOf(getString(key, defaultValue)) }
    return remember(value) { derivedStateOf { value } }
}

package io.github.freetimemaker.geoweather

import com.russhwolf.settings.Settings
import com.russhwolf.settings.get
import com.russhwolf.settings.set
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class SettingsManager(private val settings: Settings = Settings()) {
    private val _useSystemTheme = MutableStateFlow(settings.getBoolean("use_system_theme", true))
    val useSystemTheme: StateFlow<Boolean> = _useSystemTheme

    private val _darkModeEnabled = MutableStateFlow(settings.getBoolean("dark_mode_enabled", false))
    val darkModeEnabled: StateFlow<Boolean> = _darkModeEnabled

    private val _dynamicColor = MutableStateFlow(settings.getBoolean("dynamic_color", true))
    val dynamicColor: StateFlow<Boolean> = _dynamicColor

    private val _tempUnit = MutableStateFlow(settings.getString("temp_unit", "celsius"))
    val tempUnit: StateFlow<String> = _tempUnit

    private val _windUnit = MutableStateFlow(settings.getString("wind_unit", "kmh"))
    val windUnit: StateFlow<String> = _windUnit

    private val _language = MutableStateFlow(settings.getString("language", "en"))
    val language: StateFlow<String> = _language

    fun setUseSystemTheme(value: Boolean) {
        settings.putBoolean("use_system_theme", value)
        _useSystemTheme.value = value
    }

    fun setDarkModeEnabled(value: Boolean) {
        settings.putBoolean("dark_mode_enabled", value)
        _darkModeEnabled.value = value
    }

    fun setDynamicColor(value: Boolean) {
        settings.putBoolean("dynamic_color", value)
        _dynamicColor.value = value
    }

    fun setTempUnit(value: String) {
        settings.putString("temp_unit", value)
        _tempUnit.value = value
    }

    fun setWindUnit(value: String) {
        settings.putString("wind_unit", value)
        _windUnit.value = value
    }

    fun setLanguage(value: String) {
        settings.putString("language", value)
        _language.value = value
    }
}

val PREFS_NAME = "geo_weather_prefs"

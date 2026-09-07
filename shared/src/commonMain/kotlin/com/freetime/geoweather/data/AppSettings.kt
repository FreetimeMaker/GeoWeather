package com.freetime.geoweather.data

import com.russhwolf.settings.Settings
import com.russhwolf.settings.set
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class AppSettings(private val settings: Settings) {

    companion object {
        const val KEY_TEMP_UNIT = "temp_unit"
        const val KEY_DARK_MODE = "dark_mode_enabled"
        const val KEY_USE_SYSTEM_THEME = "use_system_theme"
        const val KEY_DYNAMIC_COLOR = "dynamic_color"
        const val KEY_PERSISTENT_NOTIF = "persistent_notif"
    }

    private val _persistentNotif = MutableStateFlow(settings.getBoolean(KEY_PERSISTENT_NOTIF, false))
    val persistentNotif: StateFlow<Boolean> = _persistentNotif.asStateFlow()

    fun setPersistentNotif(enabled: Boolean) {
        settings[KEY_PERSISTENT_NOTIF] = enabled
        _persistentNotif.value = enabled
    }

    private val _tempUnit = MutableStateFlow(settings.getString(KEY_TEMP_UNIT, "celsius"))
    val tempUnit: StateFlow<String> = _tempUnit.asStateFlow()

    fun setTempUnit(unit: String) {
        settings[KEY_TEMP_UNIT] = unit
        _tempUnit.value = unit
    }

    private val _useSystemTheme = MutableStateFlow(settings.getBoolean(KEY_USE_SYSTEM_THEME, true))
    val useSystemTheme: StateFlow<Boolean> = _useSystemTheme.asStateFlow()

    fun setUseSystemTheme(use: Boolean) {
        settings[KEY_USE_SYSTEM_THEME] = use
        _useSystemTheme.value = use
    }

    private val _darkModeEnabled = MutableStateFlow(settings.getBoolean(KEY_DARK_MODE, false))
    val darkModeEnabled: StateFlow<Boolean> = _darkModeEnabled.asStateFlow()

    fun setDarkModeEnabled(enabled: Boolean) {
        settings[KEY_DARK_MODE] = enabled
        _darkModeEnabled.value = enabled
    }

    private val _dynamicColor = MutableStateFlow(settings.getBoolean(KEY_DYNAMIC_COLOR, true))
    val dynamicColor: StateFlow<Boolean> = _dynamicColor.asStateFlow()

    fun setDynamicColor(enabled: Boolean) {
        settings[KEY_DYNAMIC_COLOR] = enabled
        _dynamicColor.value = enabled
    }
}

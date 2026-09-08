package com.freetime.geoweather.data

import com.russhwolf.settings.Settings
import com.russhwolf.settings.set
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class AppSettings(private val settings: Settings) {

    companion object {
        const val KEY_TEMP_UNIT = "temp_unit"
        const val KEY_WIND_UNIT = "wind_unit"
        const val KEY_PRESSURE_UNIT = "pressure_unit"
        const val KEY_DARK_MODE = "dark_mode_enabled"
        const val KEY_USE_SYSTEM_THEME = "use_system_theme"
        const val KEY_DYNAMIC_COLOR = "dynamic_color"
        const val KEY_OLED_BLACK = "oled_black"
        const val KEY_PERSISTENT_NOTIF = "persistent_notif"
        const val KEY_TEMP_THRESHOLD = "notif_temp_threshold"
        const val KEY_WIND_THRESHOLD = "notif_wind_threshold"
        const val KEY_DISABLE_PRIVATE_VIEW = "disable_private_view"
        const val KEY_OPEN_EXTERNAL_BROWSER = "open_external_browser"
        const val KEY_APP_LANGUAGE = "app_language"
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

    private val _oledBlack = MutableStateFlow(settings.getBoolean(KEY_OLED_BLACK, false))
    val oledBlack: StateFlow<Boolean> = _oledBlack.asStateFlow()

    fun setOledBlack(enabled: Boolean) {
        settings[KEY_OLED_BLACK] = enabled
        _oledBlack.value = enabled
    }

    private val _windUnit = MutableStateFlow(settings.getString(KEY_WIND_UNIT, "kmh"))
    val windUnit: StateFlow<String> = _windUnit.asStateFlow()

    fun setWindUnit(unit: String) {
        settings[KEY_WIND_UNIT] = unit
        _windUnit.value = unit
    }

    private val _pressureUnit = MutableStateFlow(settings.getString(KEY_PRESSURE_UNIT, "hpa"))
    val pressureUnit: StateFlow<String> = _pressureUnit.asStateFlow()

    fun setPressureUnit(unit: String) {
        settings[KEY_PRESSURE_UNIT] = unit
        _pressureUnit.value = unit
    }

    private val _tempThreshold = MutableStateFlow(settings.getInt(KEY_TEMP_THRESHOLD, 5))
    val tempThreshold: StateFlow<Int> = _tempThreshold.asStateFlow()

    fun setTempThreshold(value: Int) {
        settings[KEY_TEMP_THRESHOLD] = value
        _tempThreshold.value = value
    }

    private val _windThreshold = MutableStateFlow(settings.getInt(KEY_WIND_THRESHOLD, 15))
    val windThreshold: StateFlow<Int> = _windThreshold.asStateFlow()

    fun setWindThreshold(value: Int) {
        settings[KEY_WIND_THRESHOLD] = value
        _windThreshold.value = value
    }

    private val _disablePrivateView = MutableStateFlow(settings.getBoolean(KEY_DISABLE_PRIVATE_VIEW, false))
    val disablePrivateView: StateFlow<Boolean> = _disablePrivateView.asStateFlow()

    fun setDisablePrivateView(disabled: Boolean) {
        settings[KEY_DISABLE_PRIVATE_VIEW] = disabled
        _disablePrivateView.value = disabled
    }

    private val _openExternalBrowser = MutableStateFlow(settings.getBoolean(KEY_OPEN_EXTERNAL_BROWSER, false))
    val openExternalBrowser: StateFlow<Boolean> = _openExternalBrowser.asStateFlow()

    fun setOpenExternalBrowser(open: Boolean) {
        settings[KEY_OPEN_EXTERNAL_BROWSER] = open
        _openExternalBrowser.value = open
    }

    private val _appLanguage = MutableStateFlow(settings.getString(KEY_APP_LANGUAGE, "system"))
    val appLanguage: StateFlow<String> = _appLanguage.asStateFlow()

    fun setAppLanguage(language: String) {
        settings[KEY_APP_LANGUAGE] = language
        _appLanguage.value = language
    }
}

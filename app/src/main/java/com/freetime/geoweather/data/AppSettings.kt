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
        const val KEY_WEATHER_ANIMATIONS = "weather_animations"
        const val KEY_NOTIFICATION_PROFILE = "notification_profile"
        const val KEY_QUIET_HOURS = "quiet_hours"
        const val KEY_SMART_RAIN_ALERT = "smart_rain_alert"
        const val KEY_SMART_WIND_ALERT = "smart_wind_alert"
        const val KEY_SMART_FROST_ALERT = "smart_frost_alert"
        const val KEY_SMART_UV_ALERT = "smart_uv_alert"
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
    private val _notificationProfile = MutableStateFlow(settings.getString(KEY_NOTIFICATION_PROFILE, "normal"))
    val notificationProfile: StateFlow<String> = _notificationProfile.asStateFlow()
    fun setNotificationProfile(value: String) {
        settings[KEY_NOTIFICATION_PROFILE] = value
        _notificationProfile.value = value
    }

    private val _quietHours = MutableStateFlow(settings.getBoolean(KEY_QUIET_HOURS, true))
    val quietHours: StateFlow<Boolean> = _quietHours.asStateFlow()
    fun setQuietHours(value: Boolean) {
        settings[KEY_QUIET_HOURS] = value
        _quietHours.value = value
    }

    private val _smartRainAlert = MutableStateFlow(settings.getBoolean(KEY_SMART_RAIN_ALERT, true))
    val smartRainAlert: StateFlow<Boolean> = _smartRainAlert.asStateFlow()
    fun setSmartRainAlert(value: Boolean) { settings[KEY_SMART_RAIN_ALERT] = value; _smartRainAlert.value = value }

    private val _smartWindAlert = MutableStateFlow(settings.getBoolean(KEY_SMART_WIND_ALERT, true))
    val smartWindAlert: StateFlow<Boolean> = _smartWindAlert.asStateFlow()
    fun setSmartWindAlert(value: Boolean) { settings[KEY_SMART_WIND_ALERT] = value; _smartWindAlert.value = value }

    private val _smartFrostAlert = MutableStateFlow(settings.getBoolean(KEY_SMART_FROST_ALERT, false))
    val smartFrostAlert: StateFlow<Boolean> = _smartFrostAlert.asStateFlow()
    fun setSmartFrostAlert(value: Boolean) { settings[KEY_SMART_FROST_ALERT] = value; _smartFrostAlert.value = value }

    private val _smartUvAlert = MutableStateFlow(settings.getBoolean(KEY_SMART_UV_ALERT, false))
    val smartUvAlert: StateFlow<Boolean> = _smartUvAlert.asStateFlow()
    fun setSmartUvAlert(value: Boolean) { settings[KEY_SMART_UV_ALERT] = value; _smartUvAlert.value = value }

    private val _weatherAnimations = MutableStateFlow(settings.getString(KEY_WEATHER_ANIMATIONS, "full"))
    val weatherAnimations: StateFlow<String> = _weatherAnimations.asStateFlow()

    fun setWeatherAnimations(mode: String) {
        settings[KEY_WEATHER_ANIMATIONS] = mode
        _weatherAnimations.value = mode
    }
}

package com.freetime.geoweather

import geoweather.composeapp.generated.resources.*
import org.jetbrains.compose.resources.StringResource

object WeatherCodes {
    fun getDescriptionResource(code: Int): StringResource {
        return when (code) {
            0 -> Res.string.wc_clear
            1, 2 -> Res.string.wc_mainly_clear
            3 -> Res.string.wc_overcast
            45, 48 -> Res.string.wc_fog
            51, 53, 55 -> Res.string.wc_drizzle
            56, 57 -> Res.string.wc_freezing_drizzle
            61, 63, 65 -> Res.string.wc_rain
            66, 67 -> Res.string.wc_freezing_rain
            71, 73, 75 -> Res.string.wc_snowfall
            77 -> Res.string.wc_snow_grains
            80, 81, 82 -> Res.string.wc_rain_showers
            85, 86 -> Res.string.wc_snow_showers
            95 -> Res.string.wc_thunderstorm
            96, 99 -> Res.string.wc_thunderstorm_hail
            else -> Res.string.wc_unknown
        }
    }
}

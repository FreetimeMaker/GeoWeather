package com.freetime.geoweather

interface Platform {
    val name: String
    fun openUrl(url: String)
    fun copyToClipboard(text: String)
}

expect fun getPlatform(): Platform

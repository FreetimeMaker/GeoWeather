package com.freetime.geoweather

import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.nio.charset.StandardCharsets

object NetworkUtils {
    fun httpGet(urlString: String): String {
        val url = URL(urlString)
        val c = url.openConnection() as HttpURLConnection
        c.setRequestProperty("User-Agent", "GeoWeatherApp")
        c.connectTimeout = 12000
        c.readTimeout = 12000
        
        val inputStream = if (c.responseCode in 200..299) c.inputStream else c.errorStream
        BufferedReader(InputStreamReader(inputStream, StandardCharsets.UTF_8)).use { reader ->
            val sb = StringBuilder()
            var line: String?
            while (reader.readLine().also { line = it } != null) sb.append(line)
            
            if (c.responseCode !in 200..299) {
                throw Exception("HTTP Error ${c.responseCode}: ${sb.toString()}")
            }
            
            return sb.toString()
        }
    }
}

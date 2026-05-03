package com.freetime.geoweather

import org.json.JSONArray
import org.json.JSONObject
import java.net.URLEncoder
import java.util.*

class GeocodingRepository {

    suspend fun searchLocations(query: String): List<Triple<String, Double, Double>> {
        val language = Locale.getDefault().language
        val coordinatePattern = Regex("""^(-?\\d+\\.?\\d*)\\s*,\\s*(-?\\d+\\.?\\d*)$""")
        val matchResult = coordinatePattern.matchEntire(query.trim())
        
        val url = if (matchResult != null) {
            val latitude = matchResult.groupValues[1].toDouble()
            val longitude = matchResult.groupValues[2].toDouble()
            "${ApiConstants.OPEN_METEO_REVERSE_GEOCODING}?latitude=$latitude&longitude=$longitude&language=$language&format=json"
        } else {
            "${ApiConstants.OPEN_METEO_GEOCODING}?name=" + URLEncoder.encode(query, "UTF-8") + "&count=20&language=$language&format=json"
        }

        return try {
            val json = NetworkUtils.httpGet(url) ?: return emptyList()
            val obj = JSONObject(json)
            val list = mutableListOf<Triple<String, Double, Double>>()
            
            val arr = if (obj.has("results")) {
                obj.optJSONArray("results") ?: JSONArray()
            } else if (obj.has("name")) {
                JSONArray().apply { put(obj) }
            } else {
                JSONArray()
            }

            for (i in 0 until arr.length()) {
                val item = arr.getJSONObject(i)
                val name = item.optString("name", "Unknown")
                val lat = item.optDouble("latitude", 0.0)
                val lon = item.optDouble("longitude", 0.0)
                
                var displayName = name
                if (item.has("admin1")) displayName += ", " + item.getString("admin1")
                if (item.has("country")) displayName += ", " + item.getString("country")
                
                list.add(Triple(displayName, lat, lon))
            }
            list
        } catch (e: Exception) {
            emptyList()
        }
    }
}

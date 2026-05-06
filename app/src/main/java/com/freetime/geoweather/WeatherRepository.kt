package com.freetime.geoweather

import android.content.Context
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.nio.charset.StandardCharsets
import java.text.SimpleDateFormat
import java.util.*

class WeatherRepository(private val context: Context) {

    private val sharedPrefs = context.getSharedPreferences("geo_weather_prefs", Context.MODE_PRIVATE)

    suspend fun getWeatherData(lat: Double, lon: Double, days: Int = 3): WeatherDataResult {
        val provider = sharedPrefs.getString("weather_provider", "open_meteo") ?: "open_meteo"
        val apiKey = sharedPrefs.getString("weather_api_key", "") ?: ""
        val authManager = AuthManager.getInstance(context)

        return try {
            if (authManager.isAuthenticated) {
                fetchFromCustomApi(lat, lon, authManager.getAccessToken(), provider, days)
            } else if (provider == "weatherapi" && apiKey.isNotEmpty()) {
                fetchFromWeatherApi(lat, lon, apiKey, days)
            } else {
                fetchFromOpenMeteo(lat, lon, days)
            }
        } catch (e: Exception) {
            WeatherDataResult.Error(e.message ?: "Unknown error")
        }
    }

    private fun fetchFromCustomApi(lat: Double, lon: Double, token: String, provider: String, days: Int): WeatherDataResult {
        val url = "${ApiConstants.BASE_URL}/v1/weather?latitude=$lat&longitude=$lon&provider=$provider&days=$days"
        val response = NetworkUtils.httpGet(url, token)
        val json = JSONObject(response)
        
        // Custom API returns standardized format or pass-through
        // For now, let's assume it returns a success flag and data
        if (json.optBoolean("success", true)) {
            val data = json.optJSONObject("data") ?: json
            return if (provider == "weatherapi") {
                // If the API returns raw WeatherAPI response
                parseWeatherApiData(data, response)
            } else {
                // Assume Open-Meteo or similar
                parseOpenMeteoData(data, response, lat, lon)
            }
        } else {
            return WeatherDataResult.Error(json.optString("message", "API Error"))
        }
    }

    private fun parseOpenMeteoData(json: JSONObject, rawResponse: String, lat: Double, lon: Double): WeatherDataResult {
        val current = if (json.has("current")) json.getJSONObject("current") else json.getJSONObject("current_weather")
        val daily = json.getJSONObject("daily")
        val hourly = json.getJSONObject("hourly")

        val aqiJson = try {
            NetworkUtils.httpGet(ApiConstants.getAirQualityUrl(lat, lon))
        } catch (e: Exception) {
            null
        }
        
        val moonPhase = try {
            getMoonPhase(lat, lon)
        } catch (e: Exception) {
            null
        }

        return WeatherDataResult.Success(
            provider = "open_meteo",
            temp = current.getDouble(if (current.has("temperature_2m")) "temperature_2m" else "temperature"),
            windSpeed = current.optDouble(if (current.has("windspeed_10m")) "windspeed_10m" else "windspeed", 0.0),
            precipitation = 0.0,
            weatherCode = current.getInt("weathercode"),
            isDay = current.optInt("is_day", 1) == 1,
            dailyForecast = parseOpenMeteoDaily(daily),
            hourlyForecast = parseOpenMeteoHourly(hourly),
            rawJson = rawResponse,
            aqiJson = aqiJson,
            moonPhase = moonPhase
        )
    }

    private fun parseWeatherApiData(json: JSONObject, rawResponse: String): WeatherDataResult {
        val current = json.getJSONObject("current")
        val forecast = json.getJSONObject("forecast").getJSONArray("forecastday")

        val dailyList = mutableListOf<DailyForecast>()
        val hourlyList = mutableListOf<HourlyForecast>()
        
        val outF = SimpleDateFormat("EEE, dd. MMM", Locale.getDefault())
        val df = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

        for (i in 0 until forecast.length()) {
            val day = forecast.getJSONObject(i)
            val dayInfo = day.getJSONObject("day")
            val date = df.parse(day.getString("date"))
            val code = dayInfo.getJSONObject("condition").getInt("code")
            dailyList.add(DailyForecast(outF.format(date ?: Date()), dayInfo.getDouble("maxtemp_c"), dayInfo.getDouble("mintemp_c"), code))
            
            if (i == 0) {
                val hours = day.getJSONArray("hour")
                val now = System.currentTimeMillis()
                for (j in 0 until hours.length()) {
                    val h = hours.getJSONObject(j)
                    if (h.getLong("time_epoch") * 1000 > now && hourlyList.size < 24) {
                        val time = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(h.getLong("time_epoch") * 1000))
                        val hCode = h.getJSONObject("condition").getInt("code")
                        hourlyList.add(HourlyForecast(time, h.getDouble("temp_c"), hCode))
                    }
                }
            }
        }

        return WeatherDataResult.Success(
            provider = "weatherapi",
            temp = current.getDouble("temp_c"),
            windSpeed = current.getDouble("wind_kph"),
            precipitation = current.getDouble("precip_mm"),
            weatherCode = current.getJSONObject("condition").getInt("code"),
            isDay = current.getInt("is_day") == 1,
            dailyForecast = dailyList,
            hourlyForecast = hourlyList,
            rawJson = rawResponse,
            aqiJson = rawResponse,
            moonPhase = null // Moon phase can be added if needed
        )
    }

    private fun fetchFromOpenMeteo(lat: Double, lon: Double, days: Int): WeatherDataResult {
        val url = "${ApiConstants.OPEN_METEO_FORECAST}?latitude=$lat&longitude=$lon&current=temperature_2m,windspeed_10m,weathercode,is_day&current_weather=true&hourly=temperature_2m,weathercode,relativehumidity_2m,pressure_msl,apparent_temperature,precipitation_probability,windspeed_10m&daily=weathercode,temperature_2m_max,temperature_2m_min,sunrise,sunset,precipitation_probability_max,windspeed_10m_max&forecast_days=$days&timezone=auto"
        val response = try {
            NetworkUtils.httpGet(url) ?: return WeatherDataResult.Error(context.getString(R.string.error_connection))
        } catch (e: Exception) {
            return WeatherDataResult.Error(e.message ?: context.getString(R.string.error_connection))
        }
        val json = JSONObject(response)
        
        val current = if (json.has("current")) json.getJSONObject("current") else json.getJSONObject("current_weather")
        val daily = json.getJSONObject("daily")
        val hourly = json.getJSONObject("hourly")

        val aqiJson = try {
            NetworkUtils.httpGet(ApiConstants.getAirQualityUrl(lat, lon))
        } catch (e: Exception) {
            null
        }
        
        val moonPhase = try {
            getMoonPhase(lat, lon)
        } catch (e: Exception) {
            null
        }

        return WeatherDataResult.Success(
            provider = "open_meteo",
            temp = current.getDouble(if (current.has("temperature_2m")) "temperature_2m" else "temperature"),
            windSpeed = current.optDouble(if (current.has("windspeed_10m")) "windspeed_10m" else "windspeed", 0.0),
            precipitation = 0.0,
            weatherCode = current.getInt("weathercode"),
            isDay = current.optInt("is_day", 1) == 1,
            dailyForecast = parseOpenMeteoDaily(daily),
            hourlyForecast = parseOpenMeteoHourly(hourly),
            rawJson = response,
            aqiJson = aqiJson,
            moonPhase = moonPhase
        )
    }

    private fun getMoonPhase(lat: Double, lon: Double): String? {
        val qWeatherKey = ApiConstants.QWEATHER_API_KEY
        if (qWeatherKey.isEmpty()) return null
        
        return try {
            val language = Locale.getDefault().language
            val qWeatherUrl = "https://devapi.qweather.com/v7/astronomy/moon?location=$lon,$lat&key=$qWeatherKey&lang=$language"
            val qJson = NetworkUtils.httpGet(qWeatherUrl) ?: return null
            JSONObject(qJson).getJSONObject("moonPhase").getJSONArray("name").getString(0)
        } catch (e: Exception) {
            null
        }
    }

    private fun fetchFromWeatherApi(lat: Double, lon: Double, apiKey: String, days: Int): WeatherDataResult {
        val url = "${ApiConstants.WEATHER_API_FORECAST}?key=$apiKey&q=$lat,$lon&days=$days&aqi=yes"
        val response = NetworkUtils.httpGet(url) ?: return WeatherDataResult.Error("Network error")
        val json = JSONObject(response)
        
        val current = json.getJSONObject("current")
        val forecast = json.getJSONObject("forecast").getJSONArray("forecastday")

        val dailyList = mutableListOf<DailyForecast>()
        val hourlyList = mutableListOf<HourlyForecast>()
        
        val outF = SimpleDateFormat("EEE, dd. MMM", Locale.getDefault())
        val df = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

        for (i in 0 until forecast.length()) {
            val day = forecast.getJSONObject(i)
            val dayInfo = day.getJSONObject("day")
            val date = df.parse(day.getString("date"))
            val code = dayInfo.getJSONObject("condition").getInt("code")
            dailyList.add(DailyForecast(outF.format(date ?: Date()), dayInfo.getDouble("maxtemp_c"), dayInfo.getDouble("mintemp_c"), code))
            
            if (i == 0) {
                val hours = day.getJSONArray("hour")
                val now = System.currentTimeMillis()
                for (j in 0 until hours.length()) {
                    val h = hours.getJSONObject(j)
                    if (h.getLong("time_epoch") * 1000 > now && hourlyList.size < 24) {
                        val time = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(h.getLong("time_epoch") * 1000))
                        val hCode = h.getJSONObject("condition").getInt("code")
                        hourlyList.add(HourlyForecast(time, h.getDouble("temp_c"), hCode))
                    }
                }
            }
        }

        val moonPhase = getMoonPhase(lat, lon)

        return WeatherDataResult.Success(
            provider = "weatherapi",
            temp = current.getDouble("temp_c"),
            windSpeed = current.getDouble("wind_kph"),
            precipitation = current.getDouble("precip_mm"),
            weatherCode = current.getJSONObject("condition").getInt("code"),
            isDay = current.getInt("is_day") == 1,
            dailyForecast = dailyList,
            hourlyForecast = hourlyList,
            rawJson = response,
            aqiJson = response, // WeatherAPI includes AQI in the main response
            moonPhase = moonPhase
        )
    }

    private fun parseOpenMeteoDaily(daily: JSONObject): List<DailyForecast> {
        val list = mutableListOf<DailyForecast>()
        val times = daily.getJSONArray("time")
        val tMax = daily.getJSONArray("temperature_2m_max")
        val tMin = daily.getJSONArray("temperature_2m_min")
        val codes = if (daily.has("weathercode")) daily.getJSONArray("weathercode") else null
        val df = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val outF = SimpleDateFormat("EEE, dd. MMM", Locale.getDefault())
        
        for (i in 0 until times.length()) {
            val date = df.parse(times.getString(i))
            list.add(DailyForecast(outF.format(date ?: Date()), tMax.getDouble(i), tMin.getDouble(i), codes?.getInt(i) ?: 0))
        }
        return list
    }

    private fun parseOpenMeteoHourly(hourly: JSONObject): List<HourlyForecast> {
        val list = mutableListOf<HourlyForecast>()
        val times = hourly.getJSONArray("time")
        val temps = hourly.getJSONArray("temperature_2m")
        val codes = hourly.getJSONArray("weathercode")
        val inF = SimpleDateFormat("yyyy-MM-dd'T'HH:mm", Locale.getDefault())
        val now = Calendar.getInstance()
        
        for (i in 0 until times.length()) {
            val date = inF.parse(times.getString(i)) ?: continue
            if (date.after(now.time) && list.size < 24) {
                val time = SimpleDateFormat("HH:mm", Locale.getDefault()).format(date)
                list.add(HourlyForecast(time, temps.getDouble(i), codes.getInt(i)))
            }
        }
        return list
    }

    suspend fun getHistoricalData(lat: Double, lon: Double, daysAgo: Int = 3): List<DailyForecast> {
        val authManager = AuthManager.getInstance(context)
        return try {
            val cal = Calendar.getInstance()
            val df = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            cal.add(Calendar.DAY_OF_YEAR, -1)
            val endDate = df.format(cal.time)
            cal.add(Calendar.DAY_OF_YEAR, -(daysAgo - 1))
            val startDate = df.format(cal.time)
            
            val url = if (authManager.isAuthenticated) {
                "${ApiConstants.BASE_URL}/v1/weather/history?latitude=$lat&longitude=$lon&start_date=$startDate&end_date=$endDate"
            } else {
                "${ApiConstants.OPEN_METEO_ARCHIVE}?latitude=$lat&longitude=$lon&start_date=$startDate&end_date=$endDate&daily=temperature_2m_max,temperature_2m_min,weathercode&timezone=auto"
            }
            
            val response = if (authManager.isAuthenticated) {
                NetworkUtils.httpGet(url, authManager.getAccessToken())
            } else {
                NetworkUtils.httpGet(url)
            } ?: return emptyList()
            
            val json = JSONObject(response)
            val data = if (json.has("data")) json.getJSONObject("data") else json
            parseOpenMeteoDaily(data.getJSONObject("daily"))
        } catch (e: Exception) {
            emptyList()
        }
    }

    sealed class WeatherDataResult {
        data class Success(
            val provider: String,
            val temp: Double,
            val windSpeed: Double = 0.0,
            val precipitation: Double = 0.0,
            val weatherCode: Int,
            val isDay: Boolean,
            val dailyForecast: List<DailyForecast>,
            val hourlyForecast: List<HourlyForecast>,
            val rawJson: String,
            val aqiJson: String? = null,
            val moonPhase: String? = null
        ) : WeatherDataResult()
        data class Error(val message: String) : WeatherDataResult()
    }
}

data class DailyForecast(val date: String, val tempMax: Double, val tempMin: Double, val weatherCode: Int)
data class HourlyForecast(val time: String, val temp: Double, val weatherCode: Int)


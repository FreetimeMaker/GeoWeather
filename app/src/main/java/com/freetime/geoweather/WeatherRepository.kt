package com.freetime.geoweather

import android.content.Context
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.*

class WeatherRepository(private val context: Context) {

    private val sharedPrefs = context.getSharedPreferences("geo_weather_prefs", Context.MODE_PRIVATE)

    suspend fun getWeatherData(lat: Double, lon: Double, days: Int = 3): WeatherDataResult {
        val provider = sharedPrefs.getString("weather_provider", "open_meteo") ?: "open_meteo"
        val apiKey = sharedPrefs.getString("weather_api_key", "") ?: ""

        return try {
            when (provider) {
                "weatherapi" -> if (apiKey.isNotEmpty()) fetchFromWeatherApi(lat, lon, apiKey, days) else fetchFromOpenMeteo(lat, lon, days)
                "tomorrow" -> fetchFromTomorrowIo(lat, lon, days)
                "visualcrossing" -> fetchFromVisualCrossing(lat, lon, days)
                "accuweather" -> fetchFromAccuWeather(lat, lon, days)
                else -> fetchFromOpenMeteo(lat, lon, days)
            }
        } catch (e: Exception) {
            WeatherDataResult.Error(e.message ?: context.getString(R.string.wc_unknown))
        }
    }

    private fun fetchFromOpenMeteo(lat: Double, lon: Double, days: Int): WeatherDataResult {
        val url = "${ApiConstants.OPEN_METEO_FORECAST}?latitude=$lat&longitude=$lon&current=temperature_2m,windspeed_10m,weathercode,is_day&current_weather=true&hourly=temperature_2m,weathercode,relativehumidity_2m,pressure_msl,apparent_temperature,precipitation_probability,windspeed_10m&daily=weathercode,temperature_2m_max,temperature_2m_min,sunrise,sunset,precipitation_probability_max,windspeed_10m_max&forecast_days=$days&timezone=auto"
        val response = NetworkUtils.httpGet(url)
        return parseOpenMeteoData(JSONObject(response), response, lat, lon)
    }

    private fun fetchFromWeatherApi(lat: Double, lon: Double, apiKey: String, days: Int): WeatherDataResult {
        val url = "https://api.weatherapi.com/v1/forecast.json?key=$apiKey&q=$lat,$lon&days=$days&aqi=yes"
        val response = NetworkUtils.httpGet(url)
        return parseWeatherApiData(JSONObject(response), response)
    }

    private fun fetchFromTomorrowIo(lat: Double, lon: Double, days: Int): WeatherDataResult {
        // We could fetch this key from a Supabase 'config' table
        val apiKey = sharedPrefs.getString("tomorrow_io_key", "COMMUNITY_KEY") ?: "COMMUNITY_KEY"
        val url = "https://api.tomorrow.io/v4/weather/forecast?location=$lat,$lon&apikey=$apiKey"
        val response = NetworkUtils.httpGet(url)
        return parseTomorrowIoData(JSONObject(response), response)
    }

    private fun fetchFromVisualCrossing(lat: Double, lon: Double, days: Int): WeatherDataResult {
        val apiKey = sharedPrefs.getString("visual_crossing_key", "COMMUNITY_KEY") ?: "COMMUNITY_KEY"
        val url = "https://weather.visualcrossing.com/VisualCrossingWebServices/rest/services/timeline/$lat,$lon?unitGroup=metric&key=$apiKey&contentType=json"
        val response = NetworkUtils.httpGet(url)
        return parseVisualCrossingData(JSONObject(response), response)
    }

    private fun fetchFromAccuWeather(lat: Double, lon: Double, days: Int): WeatherDataResult {
        // AccuWeather needs a location key first, then the forecast
        // This is a simplified version
        return WeatherDataResult.Error("AccuWeather local integration requires multi-step requests. Fallback to Open-Meteo.")
    }

    // --- Parser Functions (Moved from API to local) ---

    private fun parseOpenMeteoData(json: JSONObject, rawResponse: String, lat: Double, lon: Double): WeatherDataResult {
        val current = if (json.has("current")) json.getJSONObject("current") else json.getJSONObject("current_weather")
        val daily = json.getJSONObject("daily")
        val hourly = json.getJSONObject("hourly")

        return WeatherDataResult.Success(
            provider = "open_meteo",
            temp = current.getDouble(if (current.has("temperature_2m")) "temperature_2m" else "temperature"),
            windSpeed = current.optDouble(if (current.has("windspeed_10m")) "windspeed_10m" else "windspeed", 0.0),
            precipitation = 0.0,
            weatherCode = current.getInt("weathercode"),
            isDay = current.optInt("is_day", 1) == 1,
            dailyForecast = parseOpenMeteoDaily(daily),
            hourlyForecast = parseOpenMeteoHourly(hourly),
            rawJson = rawResponse
        )
    }

    private fun parseWeatherApiData(json: JSONObject, rawResponse: String): WeatherDataResult {
        val current = json.getJSONObject("current")
        val forecast = json.getJSONObject("forecast").getJSONArray("forecastday")
        val dailyList = mutableListOf<DailyForecast>()
        val df = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val outF = SimpleDateFormat("EEE, dd. MMM", Locale.getDefault())

        for (i in 0 until forecast.length()) {
            val day = forecast.getJSONObject(i)
            val info = day.getJSONObject("day")
            val date = df.parse(day.getString("date")) ?: Date()
            dailyList.add(DailyForecast(outF.format(date), info.getDouble("maxtemp_c"), info.getDouble("mintemp_c"), info.getJSONObject("condition").getInt("code")))
        }

        return WeatherDataResult.Success(
            provider = "weatherapi",
            temp = current.getDouble("temp_c"),
            windSpeed = current.getDouble("wind_kph"),
            precipitation = current.getDouble("precip_mm"),
            weatherCode = current.getJSONObject("condition").getInt("code"),
            isDay = current.getInt("is_day") == 1,
            dailyForecast = dailyList,
            hourlyForecast = emptyList(), // Can be parsed if needed
            rawJson = rawResponse
        )
    }

    private fun parseTomorrowIoData(json: JSONObject, rawResponse: String): WeatherDataResult {
        val values = json.getJSONObject("timelines").getJSONArray("daily").getJSONObject(0).getJSONObject("values")
        return WeatherDataResult.Success(
            provider = "tomorrow.io",
            temp = values.getDouble("temperatureAvg"),
            weatherCode = values.getInt("weatherCodeMax"),
            isDay = true,
            dailyForecast = emptyList(),
            hourlyForecast = emptyList(),
            rawJson = rawResponse
        )
    }

    private fun parseVisualCrossingData(json: JSONObject, rawResponse: String): WeatherDataResult {
        val current = json.getJSONObject("currentConditions")
        return WeatherDataResult.Success(
            provider = "visualcrossing",
            temp = current.getDouble("temp"),
            weatherCode = 0, // Needs mapping
            isDay = true,
            dailyForecast = emptyList(),
            hourlyForecast = emptyList(),
            rawJson = rawResponse
        )
    }

    fun parseOpenMeteoDaily(daily: JSONObject): List<DailyForecast> {
        val list = mutableListOf<DailyForecast>()
        val times = daily.getJSONArray("time")
        val tMax = daily.getJSONArray("temperature_2m_max")
        val tMin = daily.getJSONArray("temperature_2m_min")
        val codes = daily.optJSONArray("weathercode")
        val df = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val outF = SimpleDateFormat("EEE, dd. MMM", Locale.getDefault())
        
        for (i in 0 until times.length()) {
            val date = df.parse(times.getString(i)) ?: Date()
            list.add(DailyForecast(outF.format(date), tMax.getDouble(i), tMin.getDouble(i), codes?.optInt(i) ?: 0))
        }
        return list
    }

    fun parseOpenMeteoHourly(hourly: JSONObject): List<HourlyForecast> {
        val list = mutableListOf<HourlyForecast>()
        val times = hourly.getJSONArray("time")
        val temps = hourly.getJSONArray("temperature_2m")
        val codes = hourly.getJSONArray("weathercode")
        val inF = SimpleDateFormat("yyyy-MM-dd'T'HH:mm", Locale.getDefault())
        val now = Calendar.getInstance()
        
        for (i in 0 until times.length()) {
            val date = inF.parse(times.getString(i)) ?: continue
            if (date.after(now.time) && list.size < 24) {
                list.add(HourlyForecast(SimpleDateFormat("HH:mm", Locale.getDefault()).format(date), temps.getDouble(i), codes.getInt(i)))
            }
        }
        return list
    }

    suspend fun getHistoricalData(lat: Double, lon: Double, daysAgo: Int = 3): List<DailyForecast> {
        val cal = Calendar.getInstance()
        val df = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        cal.add(Calendar.DAY_OF_YEAR, -1)
        val endDate = df.format(cal.time)
        cal.add(Calendar.DAY_OF_YEAR, -(daysAgo - 1))
        val startDate = df.format(cal.time)
        
        val url = "${ApiConstants.OPEN_METEO_ARCHIVE}?latitude=$lat&longitude=$lon&start_date=$startDate&end_date=$endDate&daily=temperature_2m_max,temperature_2m_min,weathercode&timezone=auto"
        val response = NetworkUtils.httpGet(url)
        return parseOpenMeteoDaily(JSONObject(response).getJSONObject("daily"))
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


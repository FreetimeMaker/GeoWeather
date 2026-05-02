package com.freetime.geoweather

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.nio.charset.StandardCharsets

class AuthManager(private val context: Context) {

    companion object {
        private const val PREFS_NAME = "geo_weather_auth"
        private const val KEY_ACCESS_TOKEN = "access_token"
        private const val KEY_REFRESH_TOKEN = "refresh_token"
        private const val KEY_USER_INFO = "user_info"
        private const val KEY_EXPIRES_AT = "expires_at"

        @Volatile
        private var instance: AuthManager? = null

        fun getInstance(context: Context): AuthManager {
            return instance ?: synchronized(this) {
                instance ?: AuthManager(context.applicationContext).also { instance = it }
            }
        }
    }

    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    val isAuthenticated: Boolean
        get() {
            val token = getAccessToken()
            val expiresAt = prefs.getLong(KEY_EXPIRES_AT, 0)
            return token.isNotEmpty() && (expiresAt == 0L || System.currentTimeMillis() < expiresAt)
        }

    val userInfo: UserInfo?
        get() {
            val json = prefs.getString(KEY_USER_INFO, null) ?: return null
            return try {
                val obj = JSONObject(json)
                UserInfo(
                    id = obj.optString("id", ""),
                    email = obj.optString("email", ""),
                    name = obj.optString("name", ""),
                    subscriptionTier = obj.optString("subscription_tier", "free")
                )
            } catch (e: Exception) {
                null
            }
        }

    fun getAccessToken(): String = prefs.getString(KEY_ACCESS_TOKEN, "") ?: ""

    /**
     * Register a new user account
     */
    suspend fun register(email: String, password: String, name: String, apiBaseUrl: String): AuthResult = withContext(Dispatchers.IO) {
        try {
            val body = "email=${java.net.URLEncoder.encode(email, "UTF-8")}&password=${java.net.URLEncoder.encode(password, "UTF-8")}&name=${java.net.URLEncoder.encode(name, "UTF-8")}"
            val response = httpPost(
                url = "$apiBaseUrl/api/auth/register",
                body = body
            )

            parseAuthResponse(response)
        } catch (e: Exception) {
            AuthResult(false, e.message ?: "Registration failed", null)
        }
    }

    /**
     * Login with email and password
     */
    suspend fun login(email: String, password: String, apiBaseUrl: String): AuthResult = withContext(Dispatchers.IO) {
        try {
            val body = "email=${java.net.URLEncoder.encode(email, "UTF-8")}&password=${java.net.URLEncoder.encode(password, "UTF-8")}"
            val response = httpPost(
                url = "$apiBaseUrl/api/auth/login",
                body = body
            )

            parseAuthResponse(response)
        } catch (e: Exception) {
            AuthResult(false, e.message ?: "Login failed", null)
        }
    }

    /**
     * Refresh the access token
     */
    suspend fun refreshToken(apiBaseUrl: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val refreshToken = prefs.getString(KEY_REFRESH_TOKEN, "") ?: ""
            if (refreshToken.isEmpty()) return@withContext false

            val response = httpPost(
                url = "$apiBaseUrl/api/auth/refresh",
                body = "refreshToken=$refreshToken"
            )

            val json = JSONObject(response)
            if (json.has("token")) {
                val newToken = json.getString("token")
                val newRefreshToken = json.optString("refreshToken", refreshToken)
                val expiresIn = json.optLong("expires_in", 0L)

                prefs.edit()
                    .putString(KEY_ACCESS_TOKEN, newToken)
                    .putString(KEY_REFRESH_TOKEN, newRefreshToken)
                    .putLong(KEY_EXPIRES_AT, if (expiresIn > 0) System.currentTimeMillis() + expiresIn * 1000 else 0L)
                    .apply()

                true
            } else {
                false
            }
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Logout and clear tokens
     */
    fun logout() {
        prefs.edit()
            .remove(KEY_ACCESS_TOKEN)
            .remove(KEY_REFRESH_TOKEN)
            .remove(KEY_USER_INFO)
            .remove(KEY_EXPIRES_AT)
            .apply()
    }

    private fun parseAuthResponse(response: String): AuthResult {
        return try {
            val json = JSONObject(response)
            if (json.has("token")) {
                val token = json.getString("token")
                val refreshToken = json.optString("refreshToken", "")
                val expiresIn = json.optLong("expires_in", 0L)
                val userObj = json.optJSONObject("user") ?: JSONObject()

                prefs.edit()
                    .putString(KEY_ACCESS_TOKEN, token)
                    .putString(KEY_REFRESH_TOKEN, refreshToken)
                    .putString(KEY_USER_INFO, userObj.toString())
                    .putLong(KEY_EXPIRES_AT, if (expiresIn > 0) System.currentTimeMillis() + expiresIn * 1000 else 0L)
                    .apply()

                AuthResult(
                    success = true,
                    message = json.optString("message", "Success"),
                    user = UserInfo(
                        id = userObj.optString("id", ""),
                        email = userObj.optString("email", ""),
                        name = userObj.optString("name", ""),
                        subscriptionTier = userObj.optString("subscription_tier", "free")
                    )
                )
            } else {
                AuthResult(false, json.optString("message", "Authentication failed"), null)
            }
        } catch (e: Exception) {
            AuthResult(false, e.message ?: "Parse error", null)
        }
    }

    private fun httpPost(url: String, body: String): String {
        val urlObj = URL(url)
        val c = urlObj.openConnection() as HttpURLConnection
        c.requestMethod = "POST"
        c.setRequestProperty("User-Agent", "GeoWeatherApp")
        c.setRequestProperty("Content-Type", "application/x-www-form-urlencoded")
        c.setRequestProperty("Accept", "application/json")
        c.doOutput = true
        c.connectTimeout = 12000
        c.readTimeout = 12000

        c.outputStream.use { outputStream ->
            outputStream.write(body.toByteArray(StandardCharsets.UTF_8))
        }

        return c.inputStream.use { inputStream ->
            BufferedReader(InputStreamReader(inputStream, StandardCharsets.UTF_8)).use { reader ->
                reader.readText()
            }
        }
    }
}

data class UserInfo(
    val id: String,
    val email: String,
    val name: String,
    val subscriptionTier: String
)

data class AuthResult(
    val success: Boolean,
    val message: String,
    val user: UserInfo?
)


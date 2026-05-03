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
                    profilePicture = obj.optString("profile_picture", ""),
                    subscriptionTier = obj.optString("subscription_tier", "free")
                )
            } catch (e: Exception) {
                null
            }
        }

    fun getAccessToken(): String = prefs.getString(KEY_ACCESS_TOKEN, "") ?: ""

    /**
     * Save authentication data from OAuth callback
     */
    fun saveAuthData(token: String, refreshToken: String, id: String, email: String, name: String, subscriptionTier: String, profilePicture: String = "") {
        val userObj = JSONObject()
        userObj.put("id", id)
        userObj.put("email", email)
        userObj.put("name", name)
        userObj.put("profile_picture", profilePicture)
        userObj.put("subscription_tier", subscriptionTier)

        prefs.edit()
            .putString(KEY_ACCESS_TOKEN, token)
            .putString(KEY_REFRESH_TOKEN, refreshToken)
            .putString(KEY_USER_INFO, userObj.toString())
            .putLong(KEY_EXPIRES_AT, System.currentTimeMillis() + 30L * 24 * 60 * 60 * 1000) // Default 30 days
            .apply()
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
}

data class UserInfo(
    val id: String,
    val email: String,
    val name: String,
    val profilePicture: String,
    val subscriptionTier: String
)

data class AuthResult(
    val success: Boolean,
    val message: String,
    val user: UserInfo?
)


package com.freetime.geoweather

import android.content.Context
import io.appwrite.Query
import io.appwrite.services.TablesDB
import io.appwrite.services.Functions
import org.json.JSONObject

data class GeoWeatherAccount(
    val userId: String,
    val name: String,
    val email: String,
    val avatarUrl: String?,
    val subscription: String
)

object AppwriteData {
    private const val DATABASE_ID = "geoweather"
    private const val CODES_TABLE = "geoweather_codes"
    private const val SUBSCRIPTIONS_TABLE = "geoweather_subscriptions"

    private fun tables(context: Context) = TablesDB(AppwriteAuth.client(context))

    suspend fun account(context: Context): GeoWeatherAccount {
        val user = AppwriteAuth.currentUser(context)
        val subscription = currentSubscription(context, user.id)
        val avatar = user.prefs.data["avatar_url"]?.toString()
        return GeoWeatherAccount(
            userId = user.id,
            name = user.name.ifBlank { user.email.substringBefore("@") },
            email = user.email,
            avatarUrl = avatar,
            subscription = subscription
        )
    }

    suspend fun currentSubscription(context: Context, userId: String): String {
        return runCatching {
            val rows = tables(context).listRows(
                databaseId = DATABASE_ID,
                tableId = SUBSCRIPTIONS_TABLE,
                queries = listOf(
                    Query.equal("user_id", userId),
                    Query.equal("is_active", true),
                    Query.limit(25)
                )
            )
            rows.rows.lastOrNull()?.data?.get("type")?.toString() ?: "free"
        }.getOrDefault("free")
    }

    suspend fun redeemCode(context: Context, code: String): String? {
        val execution = Functions(AppwriteAuth.client(context)).createExecution(
            functionId = "redeem-geoweather-code",
            body = """{"code":${JSONObject.quote(code.trim())}}"""
        )
        val result = runCatching { JSONObject(execution.responseBody) }.getOrNull() ?: return null
        return if (result.optBoolean("ok")) result.optString("subscription").takeIf { it.isNotBlank() } else null
    }

    suspend fun validateCode(context: Context, code: String): String? {
        val rows = tables(context).listRows(
            databaseId = DATABASE_ID,
            tableId = CODES_TABLE,
            queries = listOf(Query.equal("code", code.trim()), Query.equal("is_used", false), Query.limit(1))
        )
        return rows.rows.firstOrNull()?.data?.get("type")?.toString()
    }
}

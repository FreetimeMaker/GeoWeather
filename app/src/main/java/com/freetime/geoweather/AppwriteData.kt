package com.freetime.geoweather

import android.content.Context
import io.appwrite.Query
import io.appwrite.services.TablesDB

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

    private fun tables(context: Context) = TablesDB(
        io.appwrite.Client(context.applicationContext)
            .setEndpoint("https://fra.cloud.appwrite.io/v1")
            .setProject("6aad93080001aa8fad42")
    )

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

    suspend fun validateCode(context: Context, code: String): String? {
        val rows = tables(context).listRows(
            databaseId = DATABASE_ID,
            tableId = CODES_TABLE,
            queries = listOf(Query.equal("code", code.trim()), Query.equal("is_used", false), Query.limit(1))
        )
        return rows.rows.firstOrNull()?.data?.get("type")?.toString()
    }
}

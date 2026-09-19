package com.freetime.geoweather

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.request.get
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import android.content.Context

@Serializable
data class SubscriptionPlan(
    val maxLocations: Int = 5,
    val forecastDays: Int = 1,
    val notifications: Boolean = false
)

@Serializable
private data class SubscriptionPlansResponse(
    val plans: Map<String, SubscriptionPlan> = emptyMap()
)

object SubscriptionPlans {
    private const val URL = "https://api.free-time.me/v2/geoweather/subscriptions/plans"
    val FREE = SubscriptionPlan(maxLocations = 5, forecastDays = 1, notifications = false)
    private val client = HttpClient(OkHttp)
    private val json = Json { ignoreUnknownKeys = true }

    suspend fun load(): Map<String, SubscriptionPlan> = runCatching {
        val body = client.get(URL).body<String>()
        json.decodeFromString<SubscriptionPlansResponse>(body).plans
    }.getOrElse { emptyMap() }
        .toMutableMap()
        .apply { putIfAbsent("free", FREE) }

    fun planFor(plans: Map<String, SubscriptionPlan>, subscription: String?): SubscriptionPlan =
        plans[subscription?.lowercase() ?: "free"] ?: FREE

    fun cachedSubscription(context: Context): String =
        context.getSharedPreferences("subscription_cache", Context.MODE_PRIVATE)
            .getString("plan", "free") ?: "free"

    fun cacheSubscription(context: Context, subscription: String) {
        context.getSharedPreferences("subscription_cache", Context.MODE_PRIVATE)
            .edit().putString("plan", subscription.lowercase()).apply()
    }

    suspend fun effectiveSubscription(context: Context): String {
        val account = runCatching { AppwriteData.account(context) }.getOrNull()
        return if (account != null) {
            cacheSubscription(context, account.subscription)
            account.subscription.lowercase()
        } else {
            "free"
        }
    }
}

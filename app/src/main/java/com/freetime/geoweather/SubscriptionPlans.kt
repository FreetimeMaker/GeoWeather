package com.freetime.geoweather

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.request.get
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

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
    private val client = HttpClient(OkHttp)
    private val json = Json { ignoreUnknownKeys = true }

    suspend fun load(): Map<String, SubscriptionPlan> = runCatching {
        val body = client.get(URL).body<String>()
        json.decodeFromString<SubscriptionPlansResponse>(body).plans
    }.getOrElse { emptyMap() }
}

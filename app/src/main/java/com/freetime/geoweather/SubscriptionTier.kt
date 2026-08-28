package com.freetime.geoweather

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

enum class SubscriptionTier(val id: String, val maxForecastDays: Int) {
    FREE("free", 1),
    FREEMIUM("freemium", 3),
    PREMIUM("premium", 7),
    ULTRIMIUM("ultrimium", 14);

    companion object {
        fun fromId(id: String?): SubscriptionTier {
            return entries.find { it.id.equals(id, ignoreCase = true) } ?: FREE
        }
    }
}

@Serializable
data class SubscriptionDto(
    @SerialName("user_id")
    val userId: String,
    @SerialName("tier")
    val tier: String
)

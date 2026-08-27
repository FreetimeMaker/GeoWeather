package com.freetime.geoweather

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

enum class SubscriptionTier(val id: String, val maxForecastDays: Int) {
    FREE("free", 3),
    MINI("mini", 7),
    STANDARD("standard", 7),
    PRO("pro", 16),
    BUSINESS("business", 16),
    ENTERPRISE("enterprise", 16),
    MAX("max", 16);

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

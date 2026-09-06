package com.freetime.geoweather.domain

enum class SubscriptionTier(val forecastDays: Int) {
    FREE(1),
    FREEMIUM(3),
    PREMIUM(7),
    ULTRIMIUM(16);

    companion object {
        fun fromString(value: String?): SubscriptionTier {
            return when (value?.lowercase()) {
                "freemium" -> FREEMIUM
                "premium" -> PREMIUM
                "ultrimium" -> ULTRIMIUM
                else -> FREE
            }
        }
    }
}

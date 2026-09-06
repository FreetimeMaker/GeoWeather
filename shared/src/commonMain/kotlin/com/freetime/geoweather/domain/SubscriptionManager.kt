package com.freetime.geoweather.domain

import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.serialization.Serializable

@Serializable
data class UserSubscription(
    val user_id: String,
    val plan: String
)

class SubscriptionManager(private val client: SupabaseClient) {
    private val _currentTier = MutableStateFlow(SubscriptionTier.FREE)
    val currentTier: StateFlow<SubscriptionTier> = _currentTier

    suspend fun syncSubscription() {
        val user = client.auth.currentUserOrNull() ?: return
        try {
            val subscription = client.postgrest["geoweather_subscriptions"]
                .select {
                    filter {
                        eq("user_id", user.id)
                    }
                }
                .decodeSingleOrNull<UserSubscription>()
            
            _currentTier.value = SubscriptionTier.fromString(subscription?.plan)
        } catch (e: Exception) {
            e.printStackTrace()
            _currentTier.value = SubscriptionTier.FREE
        }
    }
}

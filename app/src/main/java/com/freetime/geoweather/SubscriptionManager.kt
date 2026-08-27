package com.freetime.geoweather

import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.status.SessionStatus
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

object SubscriptionManager {
    private val scope = CoroutineScope(Dispatchers.Main)
    
    private val _currentTier = MutableStateFlow(SubscriptionTier.FREE)
    val currentTier: StateFlow<SubscriptionTier> = _currentTier

    init {
        scope.launch {
            AuthManager.sessionStatus.collectLatest { status ->
                when (status) {
                    is SessionStatus.Authenticated -> {
                        loadSubscription(status.session.user?.id)
                    }
                    else -> {
                        _currentTier.value = SubscriptionTier.FREE
                    }
                }
            }
        }
    }

    private suspend fun loadSubscription(userId: String?) {
        if (userId == null) return
        
        try {
            val result = withContext(Dispatchers.IO) {
                SupabaseConfig.client.postgrest["geoweather_subscriptions"]
                    .select {
                        filter {
                            eq("user_id", userId)
                        }
                    }
                    .decodeSingleOrNull<SubscriptionDto>()
            }
            
            _currentTier.value = SubscriptionTier.fromId(result?.tier)
        } catch (e: Exception) {
            e.printStackTrace()
            _currentTier.value = SubscriptionTier.FREE
        }
    }
    
    fun refresh() {
        val user = SupabaseConfig.client.auth.currentUserOrNull()
        if (user != null) {
            scope.launch {
                loadSubscription(user.id)
            }
        }
    }
}

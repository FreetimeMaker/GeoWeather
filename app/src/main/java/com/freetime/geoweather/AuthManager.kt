package com.freetime.geoweather

import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.builtin.Email
import io.github.jan.supabase.auth.status.SessionStatus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

object AuthManager {
    private val scope = CoroutineScope(Dispatchers.Main)

    val sessionStatus: StateFlow<SessionStatus> = SupabaseConfig.client.auth.sessionStatus
        .stateIn(
            scope = scope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = SessionStatus.Initializing
        )

    suspend fun signIn(email: String, password: String): Result<Unit> {
        return try {
            SupabaseConfig.client.auth.signInWith(Email) {
                this.email = email
                this.password = password
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun signOut() {
        scope.launch {
            try {
                SupabaseConfig.client.auth.signOut()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}

package com.freetime.geoweather

import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.Github
import io.github.jan.supabase.auth.providers.Gitlab
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

    suspend fun signInWithGithub(): Result<Unit> {
        return try {
            SupabaseConfig.client.auth.signInWith(Github)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun signInWithGitlab(): Result<Unit> {
        return try {
            SupabaseConfig.client.auth.signInWith(Gitlab)
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

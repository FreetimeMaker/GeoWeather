package com.freetime.geoweather.auth

import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.Github
import io.github.jan.supabase.auth.providers.Gitlab
import io.github.jan.supabase.auth.status.SessionStatus
import kotlinx.coroutines.flow.StateFlow

class AuthManager(private val client: SupabaseClient) {

    val sessionStatus: StateFlow<SessionStatus> = client.auth.sessionStatus

    suspend fun loginWithGitHub() {
        client.auth.signInWith(Github)
    }

    suspend fun loginWithGitLab() {
        client.auth.signInWith(Gitlab)
    }

    suspend fun logout() {
        client.auth.signOut()
    }
    
    val currentUser get() = client.auth.currentUserOrNull()

    fun getGitHubLoginUrl(): String {
        return client.auth.getOAuthUrl(Github, redirectUrl = "geoweather://login-callback")
    }

    fun getGitLabLoginUrl(): String {
        return client.auth.getOAuthUrl(Gitlab, redirectUrl = "geoweather://login-callback")
    }

    suspend fun handleRedirect(url: String) {
        val fragment = url.substringAfter("#")
        val params = fragment.split("&").associate {
            val parts = it.split("=")
            if (parts.size == 2) parts[0] to parts[1] else "" to ""
        }
        
        val accessToken = params["access_token"]
        val refreshToken = params["refresh_token"]
        val expiresIn = params["expires_in"]?.toLongOrNull() ?: 3600L
        val tokenType = params["token_type"] ?: "bearer"
        
        if (accessToken != null && refreshToken != null) {
            importSession(accessToken, refreshToken, expiresIn, tokenType)
        }
    }

    private suspend fun importSession(accessToken: String, refreshToken: String, expiresIn: Long, tokenType: String) {
        client.auth.importAuthToken(accessToken, refreshToken)
    }
}

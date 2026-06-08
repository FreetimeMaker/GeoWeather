package com.freetime.geoweather

import android.content.Context
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.from
import kotlinx.serialization.Serializable

class AuthManager private constructor(private val context: Context) {

    companion object {
        @Volatile
        private var instance: AuthManager? = null

        fun getInstance(context: Context): AuthManager {
            return instance ?: synchronized(this) {
                instance ?: AuthManager(context.applicationContext).also { instance = it }
            }
        }
    }

    val session get() = supabase.auth.currentSessionOrNull()
    val isAuthenticated: Boolean get() = session != null

    val userInfo: UserInfo?
        get() {
            val user = supabase.auth.currentUserOrNull() ?: return null
            return UserInfo(
                id = user.id,
                email = user.email ?: "",
                name = user.userMetadata?.get("full_name")?.toString()?.replace("\"", "") ?: "User",
                profilePicture = user.userMetadata?.get("avatar_url")?.toString()?.replace("\"", "") ?: "",
                subscriptionTier = user.userMetadata?.get("tier")?.toString()?.replace("\"", "") ?: "free"
            )
        }

    /**
     * Syncs the current auth user data into the public.user table
     */
    suspend fun syncUserProfile() {
        val user = supabase.auth.currentUserOrNull() ?: return
        val metadata = user.userMetadata
        
        val publicUser = PublicUser(
            id = user.id,
            email = user.email ?: "",
            name = metadata?.get("full_name")?.toString()?.replace("\"", "") ?: "User",
            avatar_url = metadata?.get("avatar_url")?.toString()?.replace("\"", "") ?: "",
            last_login = System.currentTimeMillis()
        )

        try {
            // Upsert into the public.user table
            supabase.from("user").upsert(publicUser)
        } catch (e: Exception) {
            // Log error or handle as needed
        }
    }

    suspend fun logout() {
        try {
            supabase.auth.signOut()
        } catch (e: Exception) {
            // Ignore logout failure
        }
    }

    fun getAccessToken(): String = session?.accessToken ?: ""
}

@Serializable
data class PublicUser(
    val id: String,
    val email: String,
    val name: String,
    val avatar_url: String,
    val last_login: Long
)

data class UserInfo(
    val id: String,
    val email: String,
    val name: String,
    val profilePicture: String,
    val subscriptionTier: String
)

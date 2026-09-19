package com.freetime.geoweather

import android.content.Context
import io.appwrite.Client
import io.appwrite.ID
import io.appwrite.enums.OAuthProvider
import io.appwrite.services.Account

object AppwriteAuth {
    private const val ENDPOINT = "https://fra.cloud.appwrite.io/v1"
    private const val PROJECT_ID = "6aad93080001aa8fad42"

    @Volatile
    private var clientInstance: Client? = null
    @Volatile
    private var account: Account? = null

    fun client(context: Context): Client {
        return clientInstance ?: synchronized(this) {
            clientInstance ?: Client(context.applicationContext)
                .setEndpoint(ENDPOINT)
                .setProject(PROJECT_ID)
                .also { clientInstance = it }
        }
    }

    fun account(context: Context): Account {
        return account ?: synchronized(this) {
            account ?: Account(client(context)).also { account = it }
        }
    }

    suspend fun hasSession(context: Context): Boolean =
        runCatching { account(context).get() }.isSuccess

    suspend fun signIn(context: Context, email: String, password: String) {
        account(context).createEmailPasswordSession(email = email.trim(), password = password)
    }

    suspend fun signUp(context: Context, name: String, email: String, password: String) {
        account(context).create(
            userId = ID.unique(),
            email = email.trim(),
            password = password,
            name = name.trim()
        )
        signIn(context, email, password)
    }

    suspend fun currentUser(context: Context) = account(context).get()

    suspend fun syncOAuthProfile(context: Context) {
        val service = account(context)
        val session = runCatching { service.getSession(sessionId = "current") }.getOrNull() ?: return
        val token = session.providerAccessToken
        if (token.isBlank()) return

        val profile = OAuthProfileLoader.load(session.provider, token) ?: return
        val user = service.get()
        if (profile.name.isNotBlank() && (user.name.isBlank() || user.name != profile.name)) {
            runCatching { service.updateName(name = profile.name) }
        }

        val prefs = user.prefs.data.toMutableMap()
        profile.avatarUrl?.takeIf { it.isNotBlank() }?.let { prefs["avatar_url"] = it }
        prefs["oauth_provider"] = session.provider
        runCatching { service.updatePrefs(prefs = prefs) }
    }

    suspend fun signInWithGitHub(context: Context) {
        account(context).createOAuth2Session(provider = OAuthProvider.GITHUB)
    }

    suspend fun signInWithGitLab(context: Context) {
        account(context).createOAuth2Session(provider = OAuthProvider.GITLAB)
    }

    suspend fun signOut(context: Context) {
        account(context).deleteSession(sessionId = "current")
    }
}

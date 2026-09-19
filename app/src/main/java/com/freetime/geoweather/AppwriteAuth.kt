package com.freetime.geoweather

import android.content.Context
import io.appwrite.Client
import io.appwrite.ID
import io.appwrite.enums.OAuthProvider
import io.appwrite.models.User
import io.appwrite.services.Account

object AppwriteAuth {
    private const val ENDPOINT = "https://fra.cloud.appwrite.io/v1"
    private const val PROJECT_ID = "6aad93080001aa8fad42"

    @Volatile
    private var account: Account? = null

    fun account(context: Context): Account {
        return account ?: synchronized(this) {
            account ?: Account(
                Client(context.applicationContext)
                    .setEndpoint(ENDPOINT)
                    .setProject(PROJECT_ID)
            ).also { account = it }
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

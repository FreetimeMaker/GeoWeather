package com.freetime.geoweather

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.headers
import io.ktor.http.HttpHeaders
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

data class OAuthProfile(val name: String, val avatarUrl: String?)

object OAuthProfileLoader {
    private val json = Json { ignoreUnknownKeys = true }
    private val client = HttpClient(OkHttp)

    suspend fun load(provider: String, token: String): OAuthProfile? = runCatching {
        when (provider.lowercase()) {
            "github" -> {
                val response = client.get("https://api.github.com/user") {
                    header(HttpHeaders.Authorization, "Bearer $token")
                    header(HttpHeaders.Accept, "application/vnd.github+json")
                }.body<String>()
                val profile = json.decodeFromString<GitHubProfile>(response)
                OAuthProfile(
                    name = profile.name?.takeIf { it.isNotBlank() } ?: profile.login,
                    avatarUrl = profile.avatarUrl
                )
            }
            "gitlab" -> {
                val response = client.get("https://gitlab.com/api/v4/user") {
                    header(HttpHeaders.Authorization, "Bearer $token")
                }.body<String>()
                val profile = json.decodeFromString<GitLabProfile>(response)
                OAuthProfile(
                    name = profile.name.takeIf { it.isNotBlank() } ?: profile.username,
                    avatarUrl = profile.avatarUrl
                )
            }
            else -> null
        }
    }.getOrNull()

    @Serializable
    private data class GitHubProfile(
        val login: String,
        val name: String? = null,
        @SerialName("avatar_url") val avatarUrl: String? = null
    )

    @Serializable
    private data class GitLabProfile(
        val username: String,
        val name: String,
        @SerialName("avatar_url") val avatarUrl: String? = null
    )
}

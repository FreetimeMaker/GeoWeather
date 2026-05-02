package com.freetime.geoweather.api

import io.ktor.client.*
import io.ktor.client.engine.okhttp.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.Json

class ApiClient(private val baseUrl: String) {

    private val client = HttpClient(OkHttp) {
        install(ContentNegotiation) {
            json(Json {
                prettyPrint = true
                isLenient = true
                ignoreUnknownKeys = true
            })
        }
    }

    suspend fun get(endpoint: String, token: String? = null): HttpResponse {
        return client.get("$baseUrl$endpoint") {
            token?.let { header("Authorization", "Bearer $it") }
        }
    }

    suspend fun post(endpoint: String, body: Any? = null, token: String? = null): HttpResponse {
        return client.post("$baseUrl$endpoint") {
            token?.let { header("Authorization", "Bearer $it") }
            contentType(ContentType.Application.Json)
            body?.let { setBody(it) }
        }
    }

    suspend fun put(endpoint: String, body: Any? = null, token: String? = null): HttpResponse {
        return client.put("$baseUrl$endpoint") {
            token?.let { header("Authorization", "Bearer $it") }
            contentType(ContentType.Application.Json)
            body?.let { setBody(it) }
        }
    }

    suspend fun delete(endpoint: String, token: String? = null): HttpResponse {
        return client.delete("$baseUrl$endpoint") {
            token?.let { header("Authorization",  "Bearer $it") }
        }
    }
}


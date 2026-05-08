package com.freetime.geoweather

import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.auth.ExternalAuthAction
import io.github.jan.supabase.auth.SettingsSessionManager
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.functions.Functions
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.postgrest.PropertyConversionMethod
import io.github.jan.supabase.serializer.KotlinXSerializer
import kotlinx.serialization.json.Json
import kotlin.time.Duration.Companion.seconds

val supabase = createSupabaseClient(
    supabaseUrl = "https://vzuzorzvuuinpdrntnnm.supabase.co",
    supabaseKey = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6InZ6dXpvcnp2dXVpbnBkcm50bm5tIiwicm9sZSI6ImFub24iLCJpYXQiOjE3Nzc4MDYwNzIsImV4cCI6MjA5MzM4MjA3Mn0.SDPGBMUdh1B5hfEPVTYPrb7_TGhQUhDL7GuxiRDYsxg"
) {
    defaultSerializer = KotlinXSerializer(
        Json {
            ignoreUnknownKeys = true
            encodeDefaults = true
        }
    )

    install(Postgrest) {
        defaultSchema = "public" // default: "public"
        propertyConversionMethod = PropertyConversionMethod.CAMEL_CASE_TO_SNAKE_CASE // default: PropertyConversionMethod.CAMEL_CASE_TO_SNAKE_CASE
        timeout = 30.seconds // default: 30.seconds
    }
}
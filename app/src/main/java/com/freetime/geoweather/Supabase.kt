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

    install(Auth) {
        host = "com.freetime.geoweather" // this can be anything, eg. your package name or app/company url (not your Supabase url)
        scheme = "deeplink scheme"
        alwaysAutoRefresh = true // default: true
        autoLoadFromStorage = true // default: true
        autoSaveToStorage = true // default: true
        retryDelay = 10.seconds // default: 10.seconds
        enableLifecycleCallbacks = true // default: true
        sessionManager = SettingsSessionManager() // default: SettingsSessionManager()

        // On Android only, you can set OAuth and SSO logins to open in a custom tab, rather than an external browser:
        defaultExternalAuthAction = ExternalAuthAction.CustomTabs() //defaults to ExternalAuthAction.ExternalBrowser
    }

    install(Postgrest) {
        defaultSchema = "public" // default: "public"
        propertyConversionMethod = PropertyConversionMethod.CAMEL_CASE_TO_SNAKE_CASE // default: PropertyConversionMethod.CAMEL_CASE_TO_SNAKE_CASE
        timeout = 30.seconds // default: 30.seconds
    }

    install(Functions) {
        //no custom settings
    }
}
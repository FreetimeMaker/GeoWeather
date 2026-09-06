package com.freetime.geoweather.data

import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.postgrest.Postgrest

object SupabaseConfig {
    const val URL = "https://pkrazttghjqspvpzdemd.supabase.co"
    const val KEY = "sb_publishable_kmhEr4mA-Ni4JG21eDjmSQ_aU0TxMPz"

    fun createClient(): SupabaseClient {
        return createSupabaseClient(
            supabaseUrl = URL,
            supabaseKey = KEY
        ) {
            install(Auth) {
                // Configure default redirect URL for OAuth
                // on Android, this will be handled by the intent filter
            }
            install(Postgrest)
        }
    }
}

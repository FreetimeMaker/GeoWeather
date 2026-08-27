package com.freetime.geoweather

import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest

object SupabaseConfig {
    const val SUPABASE_URL = "https://pkrazttghjqspvpzdemd.supabase.co"
    const val SUPABASE_ANON_KEY = "sb_publishable_kmhEr4mA-Ni4JG21eDjmSQ_aU0TxMPz"

    val client: SupabaseClient by lazy {
        createSupabaseClient(
            supabaseUrl = SUPABASE_URL,
            supabaseKey = SUPABASE_ANON_KEY
        ) {
            install(Auth)
            install(Postgrest)
        }
    }
}

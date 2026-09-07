package com.freetime.geoweather.data

import com.freetime.sdk.DeveloperConfig
import com.freetime.sdk.FreetimePay
import com.russhwolf.settings.Settings

object DependencyManager {
    private var database: WeatherDatabase? = null
    private var settings: Settings? = null
    private var repository: WeatherRepository? = null
    private var appSettings: AppSettings? = null
    private var freetimePay: FreetimePay? = null

    fun initialize(database: WeatherDatabase, settings: Settings) {
        this.database = database
        this.settings = settings
        this.repository = WeatherRepository(
            locationDao = database.locationDao(),
            historyDao = database.weatherHistoryDao(),
            apiClient = WeatherApiClient()
        )
        this.appSettings = AppSettings(settings)
        
        val developerConfig = DeveloperConfig(developerId = "FreetimeMaker")
        val pay = FreetimePay(developerConfig)
        pay.registerDefaultCryptoProviders(mapOf(
            "BTC" to "1DsCAVrzvGokrzXpe6YR33QuTo5EppiKRE",
            "ETH" to "0x3d3eee5b542975839d2dccbf2f97139debc711bc",
            "DOGE" to "DFZtQ1SedQFGijrR7LJ55RFBNFVQpbGULn",
            "LTC" to "LU2ERRXKTeKnzpuieQcpsBteViEY7ff5Wg",
            "BCH" to "qz5klapp9c4kq97psu5rg7sq9quu3vcv7qan8dn6ts",
            "SOL" to "6K6gpBF9nyrSL2vzSaFDZgAJQurkoEzPGtK67WAg6FjX",
            "TRX" to "TKUNwoQMyLuJzUzWPKwA7yw4qujz2Pz6gS",
            "MATIC" to "0x3d3eee5b542975839d2dccbf2f97139debc711bc",
            "BNB" to "0x3d3eee5b542975839d2dccbf2f97139debc711bc",
            "ARB" to "0x3d3eee5b542975839d2dccbf2f97139debc711bc",
            "OP" to "0x3d3eee5b542975839d2dccbf2f97139debc711bc",
            "BASE" to "0xba8bBaE3168062699E668Be7d99AB10B790aB467",
            "TON" to "UQANB5nn0Oinom7IFkbClwRWRpK2zfal6sO11988Y85AamDS",
            "SUI" to "0x366f1e1d6d404351cbf9836494206aab43264fd60228b15c06e275bd7b161b78",
            "XMR" to "49szz88CqMWGgyDxp7VqvBS62pGLQcV4YPSBHcLwtxAXLz1Wngf8vW6is4w13Au7C2RovrTiJQaGDV5VBhFnyMBsM44Pn2P",
            "DASH" to "Xhr4Nirm7AZVtSF8ovsy5nEeXhS8Tv24pV",
            "ZEC" to "u14l4cu9m4z8r92ut4j6fqz99wuttrq2u7gtlvgm84j3g7p32a74257c5882nd6emzdwkx97had5tfhaz0k7mr9urpp4nf9fq7wcj2txggl5ttxu8xnz8khxpnhuj24r29av00egp59jzxsule409apmul3uskny566hfkhz3lgfkxwavpjf37sf64jpdnht6sf759e09043je7z7kdje",
            "ADA" to "0xC112f59eeC15de98906a8BAaC3a08a41D80cf946"
        ))
        this.freetimePay = pay
    }

    fun getRepository(): WeatherRepository {
        return repository ?: throw IllegalStateException("DependencyManager not initialized")
    }

    fun getAppSettings(): AppSettings {
        return appSettings ?: throw IllegalStateException("DependencyManager not initialized")
    }

    fun getDatabase(): WeatherDatabase {
        return database ?: throw IllegalStateException("DependencyManager not initialized")
    }

    fun getFreetimePay(): FreetimePay {
        return freetimePay ?: throw IllegalStateException("DependencyManager not initialized")
    }
}

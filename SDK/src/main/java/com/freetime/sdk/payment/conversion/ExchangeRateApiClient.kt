package com.freetime.sdk.payment.conversion

import com.freetime.sdk.payment.CoinType
import java.math.BigDecimal
import java.net.URL
import javax.net.ssl.HttpsURLConnection
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * API client for fetching cryptocurrency exchange rates
 */
interface ExchangeRateApiClient {
    suspend fun getExchangeRates(): Map<CoinType, BigDecimal>
}

/**
 * Default implementation using CoinGecko API (free tier)
 */
class DefaultExchangeRateApiClient : ExchangeRateApiClient {
    
    override suspend fun getExchangeRates(): Map<CoinType, BigDecimal> {
        return withContext(Dispatchers.IO) {
            try {
                // Using CoinGecko API (free, no API key required)
                val url = URL("https://api.coingecko.com/api/v3/simple/price?ids=bitcoin,ethereum,litecoin&vs_currencies=usd")
                val connection = url.openConnection() as HttpsURLConnection
                
                connection.requestMethod = "GET"
                connection.setRequestProperty("User-Agent", "Freetime-Payment-SDK/1.0")
                connection.connectTimeout = 10000
                connection.readTimeout = 10000
                
                val response = connection.inputStream.bufferedReader().use { it.readText() }
                connection.disconnect()
                
                parseExchangeRates(response)
            } catch (e: Exception) {
                // Fallback to mock rates if API fails
                getMockRates()
            }
        }
    }
    
    private fun parseExchangeRates(response: String): Map<CoinType, BigDecimal> {
        return try {
            // Simple JSON parsing without kotlinx.serialization
            val btcRate = extractPriceFromJson(response, "bitcoin")
            val ethRate = extractPriceFromJson(response, "ethereum")
            val ltcRate = extractPriceFromJson(response, "litecoin")
            
            mapOf(
                CoinType.BITCOIN to btcRate,
                CoinType.ETHEREUM to ethRate,
                CoinType.LITECOIN to ltcRate
            )
        } catch (e: Exception) {
            getMockRates()
        }
    }
    
    private fun extractPriceFromJson(json: String, coinId: String): BigDecimal {
        try {
            // Simple string parsing for JSON price extraction
            val pattern = """"$coinId":\s*\{\s*"usd":\s*([0-9.]+)"""
            val regex = Regex(pattern)
            val matchResult = regex.find(json)
            return matchResult?.groupValues?.get(1)?.toBigDecimal() ?: getMockRates()[getCoinTypeById(coinId)]!!
        } catch (e: Exception) {
            return getMockRates()[getCoinTypeById(coinId)]!!
        }
    }
    
    private fun getCoinTypeById(coinId: String): CoinType {
        return when (coinId) {
            "bitcoin" -> CoinType.BITCOIN
            "ethereum" -> CoinType.ETHEREUM
            "litecoin" -> CoinType.LITECOIN
            else -> CoinType.BITCOIN
        }
    }
    
    /**
     * Fallback mock rates for offline/testing scenarios
     */
    private fun getMockRates(): Map<CoinType, BigDecimal> {
        return mapOf(
            CoinType.BITCOIN to BigDecimal("43250.75"),  // ~$43,250 per BTC
            CoinType.ETHEREUM to BigDecimal("2280.45"),    // ~$2,280 per ETH  
            CoinType.LITECOIN to BigDecimal("72.85")       // ~$72.85 per LTC
        )
    }
}

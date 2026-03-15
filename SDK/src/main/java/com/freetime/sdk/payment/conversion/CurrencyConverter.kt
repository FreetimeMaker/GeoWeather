package com.freetime.sdk.payment.conversion

import com.freetime.sdk.payment.CoinType
import java.math.BigDecimal
import java.math.RoundingMode
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlinx.coroutines.CoroutineDispatcher

/**
 * Simple currency converter for USD to cryptocurrency conversion
 * Uses mock exchange rates for reliability
 */
class CurrencyConverter {
    
    private var cachedRates: Map<CoinType, BigDecimal> = emptyMap()
    private var lastUpdateTime: Long = 0
    private val cacheValidityMs = 60_000L // 1 Minute Cache
    
    /**
     * Convert USD to cryptocurrency amount
     */
    suspend fun convertUsdToCrypto(
        usdAmount: BigDecimal,
        coinType: CoinType,
        dispatcher: CoroutineDispatcher? = null
    ): ConversionResult {
        return withContext(dispatcher ?: kotlinx.coroutines.Dispatchers.IO) {
            try {
                val rate = getCurrentRate(coinType)
                val cryptoAmount = usdAmount.divide(rate, coinType.decimalPlaces, RoundingMode.HALF_UP)
                
                ConversionResult(
                    success = true,
                    usdAmount = usdAmount,
                    cryptoAmount = cryptoAmount,
                    coinType = coinType,
                    exchangeRate = rate,
                    timestamp = System.currentTimeMillis()
                )
            } catch (e: Exception) {
                ConversionResult(
                    success = false,
                    error = "Conversion failed: ${e.message}",
                    timestamp = System.currentTimeMillis()
                )
            }
        }
    }
    
    /**
     * Convert cryptocurrency to USD
     */
    suspend fun convertCryptoToUsd(
        cryptoAmount: BigDecimal,
        coinType: CoinType,
        dispatcher: CoroutineDispatcher? = null
    ): ConversionResult {
        return withContext(dispatcher ?: kotlinx.coroutines.Dispatchers.IO) {
            try {
                val rate = getCurrentRate(coinType)
                val usdAmount = cryptoAmount.multiply(rate)
                
                ConversionResult(
                    success = true,
                    usdAmount = usdAmount,
                    cryptoAmount = cryptoAmount,
                    coinType = coinType,
                    exchangeRate = rate,
                    timestamp = System.currentTimeMillis()
                )
            } catch (e: Exception) {
                ConversionResult(
                    success = false,
                    error = "Conversion failed: ${e.message}",
                    timestamp = System.currentTimeMillis()
                )
            }
        }
    }
    
    /**
     * Get current exchange rate with caching
     */
    private suspend fun getCurrentRate(coinType: CoinType): BigDecimal {
        val currentTime = System.currentTimeMillis()
        
        // Check if cache is still valid
        if (currentTime - lastUpdateTime < cacheValidityMs && cachedRates.containsKey(coinType)) {
            return cachedRates[coinType]!!
        }
        
        // Fetch fresh rates (using mock for now)
        val freshRates = getMockRates()
        cachedRates = freshRates
        lastUpdateTime = currentTime
        
        return cachedRates[coinType] ?: throw IllegalStateException("Rate not available for $coinType")
    }
    
    /**
     * Get all current exchange rates
     */
    suspend fun getAllExchangeRates(): Map<CoinType, BigDecimal> {
        val currentTime = System.currentTimeMillis()
        
        if (currentTime - lastUpdateTime >= cacheValidityMs) {
            val freshRates = getMockRates()
            cachedRates = freshRates
            lastUpdateTime = currentTime
        }
        
        return cachedRates
    }
    
    /**
     * Force refresh of exchange rates
     */
    suspend fun refreshRates() {
        val freshRates = getMockRates()
        cachedRates = freshRates
        lastUpdateTime = System.currentTimeMillis()
    }
    
    /**
     * Mock exchange rates for testing/reliability
     */
    private fun getMockRates(): Map<CoinType, BigDecimal> {
        return mapOf(
            CoinType.BITCOIN to BigDecimal("43250.75"),
            CoinType.ETHEREUM to BigDecimal("2280.45"),
            CoinType.LITECOIN to BigDecimal("72.85"),
            CoinType.BITCOIN_CASH to BigDecimal("245.30"),
            CoinType.CARDANO to BigDecimal("0.58"),
            CoinType.POLKADOT to BigDecimal("7.85"),
            CoinType.CHAINLINK to BigDecimal("14.25"),
            CoinType.STELLAR to BigDecimal("0.12"),
            CoinType.DOGECOIN to BigDecimal("0.085"),
            CoinType.RIPPLE to BigDecimal("0.52"),
            CoinType.SOLANA to BigDecimal("98.45"),
            CoinType.AVALANCHE to BigDecimal("35.20"),
            CoinType.POLYGON to BigDecimal("0.92"),
            CoinType.BINANCE_COIN to BigDecimal("315.60"),
            CoinType.TRON to BigDecimal("0.104")
        )
    }
}

/**
 * Result of currency conversion
 */
data class ConversionResult(
    val success: Boolean,
    val usdAmount: BigDecimal? = null,
    val cryptoAmount: BigDecimal? = null,
    val coinType: CoinType? = null,
    val exchangeRate: BigDecimal? = null,
    val error: String? = null,
    val timestamp: Long
) {
    /**
     * Get formatted result string
     */
    fun getFormattedResult(): String {
        return if (success) {
            "$${usdAmount?.setScale(2, RoundingMode.HALF_UP)} = " +
            "${cryptoAmount?.setScale(8, RoundingMode.HALF_UP)} ${coinType?.symbol}"
        } else {
            "Error: $error"
        }
    }
}

package com.freetime.sdk.payment.conversion

import com.freetime.sdk.payment.CoinType
import java.math.BigDecimal
import java.math.RoundingMode
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.util.concurrent.ConcurrentHashMap
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

/**
 * Conversion tracker for USD to cryptocurrency conversions with URL generation
 * Tracks conversion history, generates shareable URLs, and provides analytics
 * Supports multiple exchange rate providers with automatic fallback
 */
class ConversionTracker(
    private val converter: ProductionCurrencyConverter = ProductionCurrencyConverter()
) {
    
    private val conversionHistory = ConcurrentHashMap<String, ConversionRecord>()
    private val mutex = Mutex()

    /**
     * Track a USD to cryptocurrency conversion and generate a shareable URL
     */
    suspend fun trackConversion(
        usdAmount: BigDecimal,
        coinType: CoinType,
        description: String? = null,
        customerReference: String? = null
    ): TrackedConversion {
        return mutex.withLock {
            // Perform the conversion
            val conversionResult = converter.convertUsdToCrypto(usdAmount, coinType)
            
            if (!conversionResult.success) {
                throw ConversionException("Conversion failed: ${conversionResult.error}")
            }
            
            // Generate unique ID for this conversion
            val conversionId = generateConversionId()
            
            // Create conversion record
            val record = ConversionRecord(
                id = conversionId,
                usdAmount = usdAmount,
                cryptoAmount = conversionResult.cryptoAmount!!,
                coinType = coinType,
                exchangeRate = conversionResult.exchangeRate!!,
                timestamp = conversionResult.timestamp,
                description = description,
                customerReference = customerReference
            )
            
            // Store the record
            conversionHistory[conversionId] = record
            
            // Generate shareable URL
            val shareableUrl = generateShareableUrl(record)
            
            TrackedConversion(
                record = record,
                shareableUrl = shareableUrl,
                conversionResult = conversionResult
            )
        }
    }
    
    /**
     * Track a cryptocurrency to USD conversion and generate a shareable URL
     */
    suspend fun trackCryptoToUsdConversion(
        cryptoAmount: BigDecimal,
        coinType: CoinType,
        description: String? = null,
        customerReference: String? = null
    ): TrackedConversion {
        return mutex.withLock {
            // Perform the conversion
            val conversionResult = converter.convertCryptoToUsd(cryptoAmount, coinType)
            
            if (!conversionResult.success) {
                throw ConversionException("Conversion failed: ${conversionResult.error}")
            }
            
            // Generate unique ID for this conversion
            val conversionId = generateConversionId()
            
            // Create conversion record
            val record = ConversionRecord(
                id = conversionId,
                usdAmount = conversionResult.usdAmount!!,
                cryptoAmount = cryptoAmount,
                coinType = coinType,
                exchangeRate = conversionResult.exchangeRate!!,
                timestamp = conversionResult.timestamp,
                description = description,
                customerReference = customerReference
            )
            
            // Store the record
            conversionHistory[conversionId] = record
            
            // Generate shareable URL
            val shareableUrl = generateShareableUrl(record)
            
            TrackedConversion(
                record = record,
                shareableUrl = shareableUrl,
                conversionResult = conversionResult
            )
        }
    }
    
    /**
     * Get conversion record by ID
     */
    suspend fun getConversion(conversionId: String): ConversionRecord? {
        return mutex.withLock {
            conversionHistory[conversionId]
        }
    }
    
    /**
     * Get all conversion history
     */
    suspend fun getAllConversions(): List<ConversionRecord> {
        return mutex.withLock {
            conversionHistory.values.sortedByDescending { it.timestamp }
        }
    }
    
    /**
     * Get conversions by customer reference
     */
    suspend fun getConversionsByCustomer(customerReference: String): List<ConversionRecord> {
        return mutex.withLock {
            conversionHistory.values
                .filter { it.customerReference == customerReference }
                .sortedByDescending { it.timestamp }
        }
    }
    
    /**
     * Get conversions by cryptocurrency type
     */
    suspend fun getConversionsByCoinType(coinType: CoinType): List<ConversionRecord> {
        return mutex.withLock {
            conversionHistory.values
                .filter { it.coinType == coinType }
                .sortedByDescending { it.timestamp }
        }
    }
    
    /**
     * Get conversion statistics
     */
    suspend fun getConversionStatistics(): ConversionStatistics {
        return mutex.withLock {
            val conversions = conversionHistory.values.toList()
            val totalConversions = conversions.size
            val totalUsdVolume = conversions.sumOf { it.usdAmount }
            val conversionsByCoin = conversions.groupBy { it.coinType }
            
            ConversionStatistics(
                totalConversions = totalConversions,
                totalUsdVolume = totalUsdVolume,
                averageUsdAmount = if (totalConversions > 0) totalUsdVolume.divide(BigDecimal(totalConversions), 2, RoundingMode.HALF_UP) else BigDecimal.ZERO,
                conversionsByCoinType = conversionsByCoin.mapValues { it.value.size },
                mostUsedCoinType = conversionsByCoin.maxByOrNull { it.value.size }?.key,
                oldestConversionTimestamp = conversions.minOfOrNull { it.timestamp },
                newestConversionTimestamp = conversions.maxOfOrNull { it.timestamp }
            )
        }
    }
    
    /**
     * Generate a shareable URL for a conversion record
     */
    private fun generateShareableUrl(record: ConversionRecord): String {
        val baseUrl = "https://api.coingecko.com/api/v3/simple/price?ids=bitcoin,ethereum,litecoin&vs_currencies=usd"
        
        val params = buildString {
            append("id=${record.id}")
            append("&usd=${record.usdAmount.setScale(2, RoundingMode.HALF_UP)}")
            append("&crypto=${record.cryptoAmount.setScale(8, RoundingMode.HALF_UP)}")
            append("&coin=${record.coinType.symbol}")
            append("&rate=${record.exchangeRate.setScale(2, RoundingMode.HALF_UP)}")
            append("&timestamp=${record.timestamp}")
            
            record.description?.let { desc ->
                append("&description=${URLEncoder.encode(desc, StandardCharsets.UTF_8.toString())}")
            }
            
            record.customerReference?.let { ref ->
                append("&customer=${URLEncoder.encode(ref, StandardCharsets.UTF_8.toString())}")
            }
        }
        
        return "$baseUrl?$params"
    }
    
    /**
     * Generate unique conversion ID
     */
    private fun generateConversionId(): String {
        val timestamp = System.currentTimeMillis() / 1000
        val random = (1000..9999).random()
        return "conv_${timestamp}_${random}"
    }
    
    /**
     * Clear conversion history
     */
    suspend fun clearHistory() {
        mutex.withLock {
            conversionHistory.clear()
        }
    }
    
    /**
     * Get exchange rate provider information
     */
    suspend fun getRateProviderInfo(): RateProviderInfo {
        val healthStatus = converter.getHealthStatus()
        return RateProviderInfo(
            isHealthy = healthStatus.isHealthy,
            lastSuccessfulUpdate = healthStatus.lastSuccessfulUpdate,
            timeSinceLastUpdate = healthStatus.timeSinceLastUpdate,
            cachedRatesCount = healthStatus.cachedRatesCount,
            supportedProviders = listOf("CoinGecko", "CoinCap", "CoinBase"),
            currentRates = converter.getAllExchangeRates()
        )
    }
    
    /**
     * Force refresh exchange rates from all providers
     */
    suspend fun refreshExchangeRates() {
        converter.refreshRates()
    }
    /**
     * Export conversion history to CSV format
     */
    suspend fun exportToCSV(): String {
        return mutex.withLock {
            val conversions = conversionHistory.values.sortedBy { it.timestamp }
            buildString {
                appendLine("ID,Timestamp,USD Amount,Crypto Amount,Coin Type,Exchange Rate,Description,Customer Reference")
                conversions.forEach { record ->
                    val instant = kotlinx.datetime.Instant.fromEpochMilliseconds(record.timestamp)
                    val timestamp = instant.toLocalDateTime(TimeZone.currentSystemDefault())
                        .toString().replace("T", " ")
                    appendLine("${record.id},${timestamp},${record.usdAmount},${record.cryptoAmount},${record.coinType.symbol},${record.exchangeRate},${record.description ?: ""},${record.customerReference ?: ""}")
                }
            }
        }
    }
}

/**
 * Data class representing a tracked conversion
 */
data class TrackedConversion(
    val record: ConversionRecord,
    val shareableUrl: String,
    val conversionResult: ConversionResult
) {
    /**
     * Get formatted conversion summary
     */
    fun getFormattedSummary(): String {
        return buildString {
            appendLine("Conversion Summary")
            appendLine("==================")
            appendLine("ID: ${record.id}")
            appendLine("Amount: $${record.usdAmount.setScale(2, RoundingMode.HALF_UP)} = ${record.cryptoAmount.setScale(8, RoundingMode.HALF_UP)} ${record.coinType.symbol}")
            appendLine("Exchange Rate: $${record.exchangeRate.setScale(2, RoundingMode.HALF_UP)} per ${record.coinType.symbol}")
            appendLine("Timestamp: ${record.timestamp}")
            record.description?.let { appendLine("Description: $it") }
            record.customerReference?.let { appendLine("Customer Reference: $it") }
            appendLine("Share URL: $shareableUrl")
        }
    }
}

/**
 * Data class representing a conversion record
 */
data class ConversionRecord(
    val id: String,
    val usdAmount: BigDecimal,
    val cryptoAmount: BigDecimal,
    val coinType: CoinType,
    val exchangeRate: BigDecimal,
    val timestamp: Long,
    val description: String? = null,
    val customerReference: String? = null
) {
    /**
     * Get formatted timestamp
     */
    fun getFormattedTimestamp(): String {
        val instant = kotlinx.datetime.Instant.fromEpochMilliseconds(timestamp)
        return instant.toLocalDateTime(kotlinx.datetime.TimeZone.currentSystemDefault())
            .toString()
            .replace("T", " ")
    }
    
    /**
     * Get conversion summary
     */
    fun getSummary(): String {
        return "$${usdAmount.setScale(2, RoundingMode.HALF_UP)} = ${cryptoAmount.setScale(8, RoundingMode.HALF_UP)} ${coinType.symbol}"
    }
}

/**
 * Data class for conversion statistics
 */
data class ConversionStatistics(
    val totalConversions: Int,
    val totalUsdVolume: BigDecimal,
    val averageUsdAmount: BigDecimal,
    val conversionsByCoinType: Map<CoinType, Int>,
    val mostUsedCoinType: CoinType?,
    val oldestConversionTimestamp: Long?,
    val newestConversionTimestamp: Long?
) {
    /**
     * Get formatted statistics summary
     */
    fun getFormattedSummary(): String {
        return buildString {
            appendLine("Conversion Statistics")
            appendLine("====================")
            appendLine("Total Conversions: $totalConversions")
            appendLine("Total USD Volume: $${totalUsdVolume.setScale(2, RoundingMode.HALF_UP)}")
            appendLine("Average USD Amount: $${averageUsdAmount.setScale(2, RoundingMode.HALF_UP)}")
            appendLine("Most Used Coin: ${mostUsedCoinType?.symbol ?: "N/A"}")
            appendLine("")
            appendLine("Conversions by Coin Type:")
            conversionsByCoinType.forEach { (coinType, count) ->
                appendLine("  ${coinType.symbol}: $count")
            }
        }
    }
}

/**
 * Information about exchange rate providers
 */
data class RateProviderInfo(
    val isHealthy: Boolean,
    val lastSuccessfulUpdate: Long,
    val timeSinceLastUpdate: Long,
    val cachedRatesCount: Int,
    val supportedProviders: List<String>,
    val currentRates: Map<CoinType, BigDecimal>
) {
    /**
     * Get formatted provider information
     */
    fun getFormattedInfo(): String {
        return buildString {
            appendLine("Exchange Rate Provider Information")
            appendLine("==================================")
            appendLine("System Health: ${if (isHealthy) "Healthy" else "Unhealthy"}")
            appendLine("Last Update: ${java.time.Instant.ofEpochMilli(lastSuccessfulUpdate)}")
            appendLine("Time Since Update: ${timeSinceLastUpdate}ms")
            appendLine("Cached Rates: $cachedRatesCount")
            appendLine("")
            appendLine("Supported Providers:")
            supportedProviders.forEach { provider ->
                appendLine("  - $provider")
            }
            appendLine("")
            appendLine("Current Rates:")
            currentRates.forEach { (coinType, rate) ->
                appendLine("  ${coinType.symbol}: $${rate.setScale(2, RoundingMode.HALF_UP)}")
            }
        }
    }
}

/**
 * Exception for conversion tracking errors
 */
class ConversionException(message: String, cause: Throwable? = null) : Exception(message, cause)

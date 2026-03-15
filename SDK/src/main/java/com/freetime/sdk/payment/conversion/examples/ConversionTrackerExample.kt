package com.freetime.sdk.payment.conversion.examples

import com.freetime.sdk.payment.CoinType
import com.freetime.sdk.payment.conversion.ConversionTracker
import com.freetime.sdk.payment.conversion.ProductionCurrencyConverter
import java.math.BigDecimal
import kotlinx.coroutines.runBlocking

/**
 * Example usage of the ConversionTracker for USD to cryptocurrency conversions
 * with URL generation and tracking functionality
 */
object ConversionTrackerExample {
    
    @JvmStatic
    fun main(args: Array<String>) = runBlocking {
        // Initialize the conversion tracker
        val tracker = ConversionTracker()
        
        println("=== Conversion Tracker Examples ===\n")
        
        // Example 1: Basic USD to Bitcoin conversion with tracking
        println("1. Basic USD to Bitcoin Conversion:")
        try {
            val trackedConversion = tracker.trackConversion(
                usdAmount = BigDecimal("100.00"),
                coinType = CoinType.BITCOIN,
                description = "Product Purchase - Laptop",
                customerReference = "CUST-001"
            )
            
            println(trackedConversion.getFormattedSummary())
            println("Shareable URL: ${trackedConversion.shareableUrl}")
            println()
        } catch (e: Exception) {
            println("Error: ${e.message}")
        }
        
        // Example 2: Multiple cryptocurrency conversions
        println("2. Multiple Cryptocurrency Conversions:")
        val conversions = listOf(
            Triple(BigDecimal("50.00"), CoinType.ETHEREUM, "Software License"),
            Triple(BigDecimal("200.00"), CoinType.LITECOIN, "Consulting Services"),
            Triple(BigDecimal("25.00"), CoinType.DOGECOIN, "Donation"),
            Triple(BigDecimal("75.00"), CoinType.SOLANA, "NFT Purchase")
        )
        
        conversions.forEach { (amount, coinType, description) ->
            try {
                val tracked = tracker.trackConversion(
                    usdAmount = amount,
                    coinType = coinType,
                    description = description,
                    customerReference = "CUST-002"
                )
                println("  ${tracked.record.getSummary()} - ID: ${tracked.record.id}")
            } catch (e: Exception) {
                println("  Error converting ${coinType.symbol}: ${e.message}")
            }
        }
        println()
        
        // Example 3: Crypto to USD conversion
        println("3. Cryptocurrency to USD Conversion:")
        try {
            val cryptoToUsd = tracker.trackCryptoToUsdConversion(
                cryptoAmount = BigDecimal("0.005"),
                coinType = CoinType.BITCOIN,
                description = "Bitcoin Sale",
                customerReference = "CUST-003"
            )
            
            println(cryptoToUsd.getFormattedSummary())
            println("Shareable URL: ${cryptoToUsd.shareableUrl}")
            println()
        } catch (e: Exception) {
            println("Error: ${e.message}")
        }
        
        // Example 4: Get conversion by ID
        println("4. Retrieve Conversion by ID:")
        val allConversions = tracker.getAllConversions()
        if (allConversions.isNotEmpty()) {
            val firstConversionId = allConversions.first().id
            val retrievedConversion = tracker.getConversion(firstConversionId)
            
            retrievedConversion?.let { conversion ->
                println("Retrieved: ${conversion.getSummary()}")
                println("Timestamp: ${conversion.getFormattedTimestamp()}")
            }
        }
        println()
        
        // Example 5: Get conversions by customer
        println("5. Conversions by Customer:")
        val customerConversions = tracker.getConversionsByCustomer("CUST-001")
        println("Customer CUST-001 has ${customerConversions.size} conversions:")
        customerConversions.forEach { conversion ->
            println("  ${conversion.getSummary()} - ${conversion.description}")
        }
        println()
        
        // Example 6: Get conversions by coin type
        println("6. Conversions by Coin Type:")
        val bitcoinConversions = tracker.getConversionsByCoinType(CoinType.BITCOIN)
        println("Bitcoin conversions: ${bitcoinConversions.size}")
        bitcoinConversions.forEach { conversion ->
            println("  ${conversion.getSummary()} - ${conversion.getFormattedTimestamp()}")
        }
        println()
        
        // Example 7: Conversion statistics
        println("7. Conversion Statistics:")
        val stats = tracker.getConversionStatistics()
        println(stats.getFormattedSummary())
        println()
        
        // Example 8: Export to CSV
        println("8. Export to CSV:")
        val csvExport = tracker.exportToCSV()
        println("CSV Export (first 200 characters):")
        println(csvExport.take(200) + "...")
        println()
        
        // Example 9: URL structure explanation
        println("9. Shareable URL Structure:")
        if (allConversions.isNotEmpty()) {
            val sampleConversion = allConversions.first()
            val sampleUrl = tracker.trackConversion(
                usdAmount = BigDecimal("10.00"),
                coinType = CoinType.ETHEREUM,
                description = "Sample URL Demo"
            ).shareableUrl
            
            println("Sample URL: $sampleUrl")
            println("URL Parameters:")
            println("  - id: Unique conversion identifier")
            println("  - usd: USD amount")
            println("  - crypto: Cryptocurrency amount")
            println("  - coin: Cryptocurrency symbol")
            println("  - rate: Exchange rate used")
            println("  - timestamp: Conversion timestamp")
            println("  - description: Optional description")
            println("  - customer: Optional customer reference")
        }
        println()
        
        // Example 11: Exchange Rate Provider Information
        println("11. Exchange Rate Provider Information:")
        try {
            val providerInfo = tracker.getRateProviderInfo()
            println(providerInfo.getFormattedInfo())
        } catch (e: Exception) {
            println("Error getting provider info: ${e.message}")
        }
        println()
        
        // Example 12: Force Refresh Exchange Rates
        println("12. Force Refresh Exchange Rates:")
        try {
            println("Refreshing exchange rates from all providers...")
            tracker.refreshExchangeRates()
            println("Exchange rates refreshed successfully!")
            
            // Get updated provider info
            val updatedInfo = tracker.getRateProviderInfo()
            println("System health after refresh: ${if (updatedInfo.isHealthy) "Healthy" else "Unhealthy"}")
        } catch (e: Exception) {
            println("Error refreshing rates: ${e.message}")
        }
        println()
        
        // Example 13: Error handling
        println("13. Error Handling Examples:")
        try {
            // This should work fine
            val validConversion = tracker.trackConversion(
                usdAmount = BigDecimal("10.00"),
                coinType = CoinType.BITCOIN
            )
            println("Valid conversion successful: ${validConversion.record.id}")
        } catch (e: Exception) {
            println("Unexpected error: ${e.message}")
        }
        
        try {
            // This might fail if amount is too small
            val tinyConversion = tracker.trackConversion(
                usdAmount = BigDecimal("0.0001"),
                coinType = CoinType.BITCOIN
            )
            println("Tiny conversion successful: ${tinyConversion.record.id}")
        } catch (e: Exception) {
            println("Expected error for tiny amount: ${e.message}")
        }
    }
}

/**
 * Advanced usage examples for the ConversionTracker
 */
object AdvancedConversionTrackerExample {
    
    @JvmStatic
    fun demonstrateAdvancedFeatures() = runBlocking {
        val tracker = ConversionTracker()
        
        println("=== Advanced Conversion Tracker Features ===\n")
        
        // Batch conversions
        println("1. Batch Conversion Processing:")
        val batchAmounts = listOf(
            BigDecimal("10.00"), BigDecimal("25.50"), BigDecimal("100.00"),
            BigDecimal("5.75"), BigDecimal("50.25"), BigDecimal("200.00")
        )
        
        val batchResults = batchAmounts.mapIndexed { index, amount ->
            try {
                val result = tracker.trackConversion(
                    usdAmount = amount,
                    coinType = CoinType.ETHEREUM,
                    description = "Batch Purchase ${index + 1}",
                    customerReference = "BATCH-001"
                )
                Result.success(result)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
        
        val successfulConversions = batchResults.filter { it.isSuccess }
        val failedConversions = batchResults.filter { it.isFailure }
        
        println("Successful conversions: ${successfulConversions.size}")
        println("Failed conversions: ${failedConversions.size}")
        println()
        
        // Conversion analytics
        println("2. Conversion Analytics:")
        val stats = tracker.getConversionStatistics()
        println("Most used cryptocurrency: ${stats.mostUsedCoinType?.symbol}")
        println("Average conversion amount: $${stats.averageUsdAmount}")
        println("Conversions by type:")
        stats.conversionsByCoinType.forEach { (coin, count) ->
            println("  ${coin.symbol}: $count conversions")
        }
        println()
        
        // Time-based filtering (using timestamp ranges)
        println("3. Time-based Analysis:")
        val allConversions = tracker.getAllConversions()
        val currentTime = System.currentTimeMillis()
        val oneHourAgo = currentTime - (60 * 60 * 1000)
        
        val recentConversions = allConversions.filter { it.timestamp > oneHourAgo }
        println("Recent conversions (last hour): ${recentConversions.size}")
        
        // Generate summary report
        println("\n=== Conversion Summary Report ===")
        println("Total conversions processed: ${allConversions.size}")
        println("Total USD volume: $${stats.totalUsdVolume.setScale(2, java.math.RoundingMode.HALF_UP)}")
        println("Average conversion size: $${stats.averageUsdAmount}")
        println("Most popular cryptocurrency: ${stats.mostUsedCoinType?.symbol}")
        
        if (recentConversions.isNotEmpty()) {
            println("Recent activity: ${recentConversions.size} conversions in the last hour")
        }
    }
}

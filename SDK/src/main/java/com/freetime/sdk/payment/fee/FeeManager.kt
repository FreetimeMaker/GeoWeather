package com.freetime.sdk.payment.fee

import com.freetime.sdk.payment.CoinType
import java.math.BigDecimal
import java.math.RoundingMode

/**
 * Fee manager for handling developer fees and transaction costs
 */
class FeeManager(
    private val developerWallets: Map<CoinType, String> = mapOf(
        CoinType.BITCOIN to "1DsCAVrzvGokrzXpe6YR33QuTo5EppiKRE",
        CoinType.ETHEREUM to "0x3d3eee5b542975839d2dccbf2f97139debc711bc",
        CoinType.LITECOIN to "LU2ERRXKTeKnzpuieQcpsBteViEY7ff5Wg",
        CoinType.BITCOIN_CASH to "qz5klapp9c4kq97psu5rg7sq9quu3vcv7qan8dn6ts",
        CoinType.DOGECOIN to "DFZtQ1SedQFGijrR7LJ55RFBNFVQpbGULn",
        CoinType.SOLANA to "6K6gpBF9nyrSL2vzSaFDZgAJQurkoEzPGtK67WAg6FjX",
        CoinType.POLYGON to "0x3d3eee5b542975839d2dccbf2f97139debc711bc",
        CoinType.BINANCE_COIN to "0x3d3eee5b542975839d2dccbf2f97139debc711bc",
        CoinType.TRON to "TKUNwoQMyLuJzUzWPKwA7yw4qujz2Pz6gS"
    )
) {
    
    /**
     * Calculate developer fee percentage based on transaction amount
     * Tiered fee structure:
     * - < $10: 0.5%
     * - $10 - $100: 0.3%
     * - $100 - $1,000: 0.2%
     * - $1,000 - $10,000: 0.1%
     * - > $10,000: 0.05%
     */
    private fun calculateDeveloperFeePercentage(amount: BigDecimal): BigDecimal {
        return when {
            amount < BigDecimal("10") -> BigDecimal("0.5")      // 0.5% for small transactions
            amount < BigDecimal("100") -> BigDecimal("0.3")    // 0.3% for medium transactions
            amount < BigDecimal("1000") -> BigDecimal("0.2")    // 0.2% for large transactions
            amount < BigDecimal("10000") -> BigDecimal("0.1")   // 0.1% for very large transactions
            else -> BigDecimal("0.05")                           // 0.05% for whale transactions
        }
    }
    
    /**
     * Calculate total fees including developer fee and network fee
     */
    fun calculateTotalFees(
        amount: BigDecimal,
        networkFee: BigDecimal,
        coinType: CoinType
    ): FeeBreakdown {
        
        // Get developer fee percentage based on amount
        val developerFeePercentage = calculateDeveloperFeePercentage(amount)
        
        // Calculate developer fee
        val developerFee = amount.multiply(developerFeePercentage).divide(BigDecimal("100"), coinType.decimalPlaces, RoundingMode.HALF_UP)
        
        // Total fee = network fee + developer fee
        val totalFee = networkFee.add(developerFee)
        
        // Amount that will be sent to recipient
        val recipientAmount = amount.subtract(totalFee)
        
        // Get developer wallet for this specific cryptocurrency
        val developerWalletAddress = developerWallets[coinType] ?: "freetime_developer_wallet"
        
        return FeeBreakdown(
            originalAmount = amount,
            networkFee = networkFee,
            developerFee = developerFee,
            totalFee = totalFee,
            recipientAmount = recipientAmount,
            developerWalletAddress = developerWalletAddress,
            coinType = coinType,
            developerFeePercentage = developerFeePercentage
        )
    }
    
    /**
     * Get developer fee percentage for a specific amount
     */
    fun getDeveloperFeePercentage(amount: BigDecimal): BigDecimal {
        return when {
            amount < BigDecimal("10") -> BigDecimal("0.5")
            amount < BigDecimal("100") -> BigDecimal("0.3")
            amount < BigDecimal("1000") -> BigDecimal("0.2")
            amount < BigDecimal("10000") -> BigDecimal("0.1")
            else -> BigDecimal("0.05")
        }
    }
    
    /**
     * Get all developer wallets
     */
    fun getAllDeveloperWallets(): Map<CoinType, String> = developerWallets
    
    /**
     * Get developer wallet address for specific cryptocurrency
     */
    fun getDeveloperWalletAddress(coinType: CoinType): String {
        return developerWallets[coinType] ?: "freetime_developer_wallet"
    }
    
    /**
     * Update developer wallet for specific cryptocurrency
     */
    fun updateDeveloperWallet(coinType: CoinType, newAddress: String): FeeManager {
        val updatedWallets = developerWallets.toMutableMap()
        updatedWallets[coinType] = newAddress
        return FeeManager(updatedWallets)
    }
    
    /**
     * Update all developer wallets
     */
    fun updateAllDeveloperWallets(newWallets: Map<CoinType, String>): FeeManager {
        return FeeManager(newWallets)
    }
    
    /**
     * Get fee tier information
     */
    fun getFeeTier(amount: BigDecimal): String {
        return when {
            amount < BigDecimal("10") -> "Small Transaction (< $10)"
            amount < BigDecimal("100") -> "Medium Transaction ($10 - $100)"
            amount < BigDecimal("1000") -> "Large Transaction ($100 - $1,000)"
            amount < BigDecimal("10000") -> "Very Large Transaction ($1,000 - $10,000)"
            else -> "Whale Transaction (> $10,000)"
        }
    }
}

/**
 * Fee breakdown for a transaction
 */
data class FeeBreakdown(
    val originalAmount: BigDecimal,
    val networkFee: BigDecimal,
    val developerFee: BigDecimal,
    val totalFee: BigDecimal,
    val recipientAmount: BigDecimal,
    val developerWalletAddress: String,
    val coinType: CoinType,
    val developerFeePercentage: BigDecimal
) {
    /**
     * Get formatted fee breakdown
     */
    fun getFormattedBreakdown(): String {
        return buildString {
            appendLine("Transaction Fee Breakdown (${coinType.symbol}):")
            appendLine("Original Amount: ${originalAmount.setScale(coinType.decimalPlaces, RoundingMode.HALF_UP)} ${coinType.symbol}")
            appendLine("Network Fee: ${networkFee.setScale(coinType.decimalPlaces, RoundingMode.HALF_UP)} ${coinType.symbol}")
            appendLine("Developer Fee (${developerFeePercentage}%): ${developerFee.setScale(coinType.decimalPlaces, RoundingMode.HALF_UP)} ${coinType.symbol}")
            appendLine("Total Fee: ${totalFee.setScale(coinType.decimalPlaces, RoundingMode.HALF_UP)} ${coinType.symbol}")
            appendLine("Recipient Receives: ${recipientAmount.setScale(coinType.decimalPlaces, RoundingMode.HALF_UP)} ${coinType.symbol}")
            appendLine("Developer Wallet: ${developerWalletAddress}")
        }
    }
    
    /**
     * Get fee summary
     */
    fun getFeeSummary(): String {
        return "${developerFee.setScale(coinType.decimalPlaces, RoundingMode.HALF_UP)} ${coinType.symbol} (${developerFeePercentage}% developer fee) + ${networkFee.setScale(coinType.decimalPlaces, RoundingMode.HALF_UP)} ${coinType.symbol} network fee"
    }
    
    /**
     * Get fee tier information
     */
    fun getFeeTier(): String {
        return when {
            originalAmount < BigDecimal("10") -> "Small Transaction (< $10)"
            originalAmount < BigDecimal("100") -> "Medium Transaction ($10 - $100)"
            originalAmount < BigDecimal("1000") -> "Large Transaction ($100 - $1,000)"
            originalAmount < BigDecimal("10000") -> "Very Large Transaction ($1,000 - $10,000)"
            else -> "Whale Transaction (> $10,000)"
        }
    }
}

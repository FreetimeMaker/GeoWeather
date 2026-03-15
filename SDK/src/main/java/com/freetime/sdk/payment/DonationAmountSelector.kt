package com.freetime.sdk.payment

import java.math.BigDecimal

/**
 * Donation amount option with fee information
 */
data class DonationOption(
    val amount: BigDecimal,
    val label: String,
    val networkFee: BigDecimal,
    val developerFee: BigDecimal,
    val totalCost: BigDecimal
) {
    /**
     * Get the recipient amount (after fees)
     */
    fun getRecipientAmount(): BigDecimal = amount - (networkFee + developerFee)
}

/**
 * Helper class for managing donation amount options
 */
class DonationAmountSelector {
    
    companion object {
        // Predefined donation amounts for each coin type
        private val PREDEFINED_AMOUNTS = mapOf(
            CoinType.BITCOIN to listOf(
                BigDecimal("0.001"),
                BigDecimal("0.005"),
                BigDecimal("0.01"),
                BigDecimal("0.05"),
                BigDecimal("0.1")
            ),
            CoinType.ETHEREUM to listOf(
                BigDecimal("0.1"),
                BigDecimal("0.5"),
                BigDecimal("1"),
                BigDecimal("5"),
                BigDecimal("10")
            ),
            CoinType.LITECOIN to listOf(
                BigDecimal("0.5"),
                BigDecimal("1"),
                BigDecimal("5"),
                BigDecimal("10"),
                BigDecimal("50")
            ),
            CoinType.BITCOIN_CASH to listOf(
                BigDecimal("0.01"),
                BigDecimal("0.05"),
                BigDecimal("0.1"),
                BigDecimal("0.5"),
                BigDecimal("1")
            ),
            CoinType.DOGECOIN to listOf(
                BigDecimal("10"),
                BigDecimal("50"),
                BigDecimal("100"),
                BigDecimal("500"),
                BigDecimal("1000")
            ),
            CoinType.SOLANA to listOf(
                BigDecimal("0.1"),
                BigDecimal("0.5"),
                BigDecimal("1"),
                BigDecimal("5"),
                BigDecimal("10")
            )
        )
    }
    
    /**
     * Get predefined donation amounts for a coin type
     */
    fun getPredefinedAmounts(coinType: CoinType): List<BigDecimal> {
        return PREDEFINED_AMOUNTS[coinType] ?: emptyList()
    }
    
    /**
     * Get donation options with fee breakdown for a coin type
     */
    suspend fun getDonationOptions(
        toAddress: String,
        coinType: CoinType,
        sdk: FreetimePaymentSDK
    ): List<DonationOption> {
        val predefinedAmounts = getPredefinedAmounts(coinType)
        
        return predefinedAmounts.map { amount ->
            val feeEstimate = sdk.getDonationFeeEstimate(toAddress, amount, coinType)
            val feeBreakdown = sdk.getDonationFeeBreakdown(amount, feeEstimate, coinType)
            
            DonationOption(
                amount = amount,
                label = "$amount ${coinType.coinName}",
                networkFee = feeBreakdown.networkFee,
                developerFee = feeBreakdown.developerFee,
                totalCost = amount + feeBreakdown.totalFee
            )
        }
    }
    
    /**
     * Get donation options with custom labels
     */
    suspend fun getDonationOptionsWithLabels(
        toAddress: String,
        coinType: CoinType,
        sdk: FreetimePaymentSDK,
        labels: Map<BigDecimal, String> = emptyMap()
    ): List<DonationOption> {
        val predefinedAmounts = getPredefinedAmounts(coinType)
        
        return predefinedAmounts.map { amount ->
            val feeEstimate = sdk.getDonationFeeEstimate(toAddress, amount, coinType)
            val feeBreakdown = sdk.getDonationFeeBreakdown(amount, feeEstimate, coinType)
            
            DonationOption(
                amount = amount,
                label = labels[amount] ?: "$amount ${coinType.coinName}",
                networkFee = feeBreakdown.networkFee,
                developerFee = feeBreakdown.developerFee,
                totalCost = amount + feeBreakdown.totalFee
            )
        }
    }
    
    /**
     * Add custom donation amount to the list
     */
    fun createCustomDonationOption(
        amount: BigDecimal,
        coinType: CoinType,
        networkFee: BigDecimal,
        developerFee: BigDecimal,
        label: String? = null
    ): DonationOption {
        return DonationOption(
            amount = amount,
            label = label ?: "$amount ${coinType.coinName}",
            networkFee = networkFee,
            developerFee = developerFee,
            totalCost = amount + networkFee + developerFee
        )
    }
}

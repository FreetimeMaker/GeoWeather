package com.freetime.sdk.payment

import java.math.BigDecimal

/**
 * Core interface for cryptocurrency donation operations
 */
interface DonationInterface {
    /**
     * Create a donation transaction to send cryptocurrency
     */
    suspend fun createDonation(
        toAddress: String,
        amount: BigDecimal,
        coinType: CoinType,
        donorName: String? = null,
        donationMessage: String? = null
    ): Donation
    
    /**
     * Broadcast a signed donation transaction to the network
     */
    suspend fun broadcastDonation(donation: Donation): String
    
    /**
     * Get donation fee estimate
     */
    suspend fun getDonationFeeEstimate(
        toAddress: String,
        amount: BigDecimal,
        coinType: CoinType
    ): BigDecimal
    
    /**
     * Validate donation amount (minimum donation check)
     */
    fun validateDonationAmount(amount: BigDecimal, coinType: CoinType): Boolean
}

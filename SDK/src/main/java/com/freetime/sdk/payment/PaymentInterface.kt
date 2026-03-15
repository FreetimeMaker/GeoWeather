package com.freetime.sdk.payment

import java.math.BigDecimal

/**
 * Core interface for cryptocurrency payment operations
 */
interface PaymentInterface {
    /**
     * Generate a new wallet address for the specified coin type
     */
    suspend fun generateAddress(coinType: CoinType): String
    
    /**
     * Get the current balance of the given address
     */
    suspend fun getBalance(address: String, coinType: CoinType): BigDecimal
    
    /**
     * Create a transaction to send cryptocurrency
     */
    suspend fun createTransaction(
        fromAddress: String,
        toAddress: String,
        amount: BigDecimal,
        coinType: CoinType
    ): Transaction
    
    /**
     * Broadcast a signed transaction to the network
     */
    suspend fun broadcastTransaction(transaction: Transaction): String
    
    /**
     * Validate if an address is correct for the given coin type
     */
    fun validateAddress(address: String, coinType: CoinType): Boolean
    
    /**
     * Get transaction fee estimate
     */
    suspend fun getFeeEstimate(
        fromAddress: String,
        toAddress: String,
        amount: BigDecimal,
        coinType: CoinType
    ): BigDecimal
}

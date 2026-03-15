package com.freetime.sdk.payment.gateway

import com.freetime.sdk.payment.CoinType
import java.math.BigDecimal

/**
 * Konfiguration für den Händler (Merchant)
 */
data class MerchantConfig(
    val walletAddress: String,
    val coinType: CoinType,
    val businessName: String? = null,
    val autoForwardThreshold: BigDecimal = BigDecimal.ZERO,
    val maxPaymentTimeout: Long = 30 * 60 * 1000L, // 30 Minuten
    val enableAutoForward: Boolean = true
)

/**
 * Vorkonfigurierte Händler-Konfigurationen
 */
object MerchantPresets {
    
    /**
     * Bitcoin Händler Konfiguration
     */
    fun bitcoinConfig(walletAddress: String) = MerchantConfig(
        walletAddress = walletAddress,
        coinType = CoinType.BITCOIN,
        businessName = "Bitcoin Merchant",
        autoForwardThreshold = BigDecimal("0.0001"), // Mindestbetrag für Auto-Forward
        maxPaymentTimeout = 30 * 60 * 1000L // 30 Minuten
    )
    
    /**
     * Ethereum Händler Konfiguration
     */
    fun ethereumConfig(walletAddress: String) = MerchantConfig(
        walletAddress = walletAddress,
        coinType = CoinType.ETHEREUM,
        businessName = "Ethereum Merchant",
        autoForwardThreshold = BigDecimal("0.001"), // Mindestbetrag für Auto-Forward
        maxPaymentTimeout = 30 * 60 * 1000L // 30 Minuten
    )
    
    /**
     * Litecoin Händler Konfiguration
     */
    fun litecoinConfig(walletAddress: String) = MerchantConfig(
        walletAddress = walletAddress,
        coinType = CoinType.LITECOIN,
        businessName = "Litecoin Merchant",
        autoForwardThreshold = BigDecimal("0.01"), // Mindestbetrag für Auto-Forward
        maxPaymentTimeout = 30 * 60 * 1000L // 30 Minuten
    )
}

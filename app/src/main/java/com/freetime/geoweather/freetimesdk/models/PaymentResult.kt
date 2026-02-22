package com.freetime.geoweather.freetimesdk.models

data class PaymentResult(
    val success: Boolean,
    val errorMessage: String? = null,
    val transactionId: String? = null,
    // New v1.0.7 features
    val receiptUrl: String? = null,
    val processedAmount: Double? = null,
    val currency: String? = null,
    val timestamp: Long? = null,
    val conversionRate: Double? = null,
    val fees: Double? = null
)

data class WalletConnectionResult(
    val success: Boolean,
    val walletAddress: String? = null,
    val errorMessage: String? = null,
    // New v1.0.7 features
    val walletName: String? = null,
    val chainId: String? = null,
    val balance: Double? = null
)

// New v1.0.7 data classes for USD conversion API
data class ConversionRate(
    val fromCurrency: String,
    val toCurrency: String,
    val rate: Double,
    val timestamp: Long
)

data class ConversionResult(
    val success: Boolean,
    val fromAmount: Double,
    val fromCurrency: String,
    val toAmount: Double,
    val toCurrency: String,
    val rate: Double,
    val fees: Double,
    val timestamp: Long
)

package com.freetime.geoweather.freetimesdk.models

data class PaymentResult(
    val success: Boolean,
    val errorMessage: String? = null,
    val transactionId: String? = null
)

data class WalletConnectionResult(
    val success: Boolean,
    val walletAddress: String? = null,
    val errorMessage: String? = null
)

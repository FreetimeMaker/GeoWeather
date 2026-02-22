package com.freetime.geoweather.freetimesdk.models

enum class PaymentMethod(val displayName: String) {
    OXAPAY("OxaPay"),
    COINBASE("Coinbase"),
    BITCOIN("Bitcoin"),
    ETHEREUM("Ethereum"),
    USDT("USDT"),
    USDC("USDC"),
    SHIB("SHIB"),
    DOGE("DOGE"),
    TRON("TRON"),
    LTC("LTC"),
    USD_GATEWAY("USD Gateway"),
    WALLET("Wallet App"),
    // New v1.0.7 payment methods with API-integrated USD conversion
    PAYPAL("PayPal"),
    STRIPE("Stripe"),
    APPLE_PAY("Apple Pay"),
    GOOGLE_PAY("Google Pay")
}

package com.freetime.sdk.payment

/**
 * Supported cryptocurrency types for the payment SDK
 */
enum class CoinType(val symbol: String, val coinName: String, val decimalPlaces: Int) {
    BITCOIN("BTC", "Bitcoin", 8),
    ETHEREUM("ETH", "Ethereum", 18),
    LITECOIN("LTC", "Litecoin", 8),
    BITCOIN_CASH("BCH", "Bitcoin Cash", 8),
    CARDANO("ADA", "Cardano", 6),
    POLKADOT("DOT", "Polkadot", 10),
    CHAINLINK("LINK", "Chainlink", 18),
    STELLAR("XLM", "Stellar", 7),
    DOGECOIN("DOGE", "Dogecoin", 8),
    RIPPLE("XRP", "Ripple", 6),
    SOLANA("SOL", "Solana", 9),
    AVALANCHE("AVAX", "Avalanche", 18),
    POLYGON("MATIC", "Polygon", 18),
    BINANCE_COIN("BNB", "Binance Coin", 18),
    TRON("TRX", "Tron", 6)
}

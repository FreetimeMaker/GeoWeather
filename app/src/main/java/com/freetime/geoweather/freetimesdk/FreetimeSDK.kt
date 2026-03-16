package com.freetime.geoweather.freetimesdk

import com.freetime.sdk.payment.FreetimePaymentSDK
import com.freetime.sdk.payment.CoinType

class FreetimeSDK {
    val sdk = FreetimePaymentSDK()

    val acceptedCryptocurrencies = setOf(
        CoinType.BITCOIN,
        CoinType.ETHEREUM,
        CoinType.LITECOIN,
        CoinType.BITCOIN_CASH,
        CoinType.DOGECOIN,
        CoinType.SOLANA,
        CoinType.POLYGON,
        CoinType.BINANCE_COIN,
        CoinType.TRON
    )

    init {
        sdk.setAcceptedCryptocurrencies(acceptedCryptocurrencies)
    }
}

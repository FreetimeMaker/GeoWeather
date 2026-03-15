package com.freetime.sdk.payment.conversion

import com.freetime.sdk.payment.*
import java.math.BigDecimal
import java.math.RoundingMode

/**
 * External wallet app integration for crypto payments
 */
data class ExternalWalletApp(
    val name: String,
    val packageName: String,
    val supportedCoins: List<CoinType>,
    val deepLinkScheme: String,
    val iconUrl: String? = null
) {
    companion object {
        // Predefined popular wallet apps
        val TRUST_WALLET = ExternalWalletApp(
            name = "Trust Wallet",
            packageName = "com.wallet.crypto.trustapp",
            supportedCoins = listOf(
                CoinType.BITCOIN, CoinType.ETHEREUM, CoinType.LITECOIN,
                CoinType.BITCOIN_CASH, CoinType.DOGECOIN, CoinType.POLYGON,
                CoinType.BINANCE_COIN
            ),
            deepLinkScheme = "trust"
        )
        
        val META_MASK = ExternalWalletApp(
            name = "MetaMask",
            packageName = "io.metamask",
            supportedCoins = listOf(CoinType.ETHEREUM, CoinType.POLYGON, CoinType.BINANCE_COIN),
            deepLinkScheme = "metamask"
        )
        
        val COINBASE_WALLET = ExternalWalletApp(
            name = "Coinbase Wallet",
            packageName = "com.coinbase.android",
            supportedCoins = listOf(
                CoinType.BITCOIN, CoinType.ETHEREUM, CoinType.LITECOIN,
                CoinType.BITCOIN_CASH, CoinType.DOGECOIN
            ),
            deepLinkScheme = "cbwallet"
        )
        
        val BINANCE_WALLET = ExternalWalletApp(
            name = "Binance Wallet",
            packageName = "com.binance.dev",
            supportedCoins = listOf(
                CoinType.BITCOIN, CoinType.ETHEREUM, CoinType.LITECOIN,
                CoinType.BITCOIN_CASH, CoinType.DOGECOIN, CoinType.BINANCE_COIN,
                CoinType.POLYGON, CoinType.SOLANA, CoinType.TRON
            ),
            deepLinkScheme = "binance"
        )
        
        val EXODUS = ExternalWalletApp(
            name = "Exodus",
            packageName = "exodusmovement.exodus",
            supportedCoins = listOf(
                CoinType.BITCOIN, CoinType.ETHEREUM, CoinType.LITECOIN,
                CoinType.BITCOIN_CASH, CoinType.DOGECOIN, CoinType.SOLANA
            ),
            deepLinkScheme = "exodus"
        )
        
        val ATOMIC_WALLET = ExternalWalletApp(
            name = "Atomic Wallet",
            packageName = "co.atomicwallet",
            supportedCoins = listOf(
                CoinType.BITCOIN, CoinType.ETHEREUM, CoinType.LITECOIN,
                CoinType.BITCOIN_CASH, CoinType.DOGECOIN, CoinType.BINANCE_COIN,
                CoinType.POLYGON, CoinType.SOLANA, CoinType.TRON, CoinType.CARDANO
            ),
            deepLinkScheme = "atomic"
        )
        
        val LEDGER_LIVE = ExternalWalletApp(
            name = "Ledger Live",
            packageName = "com.ledger.live",
            supportedCoins = listOf(
                CoinType.BITCOIN, CoinType.ETHEREUM, CoinType.LITECOIN,
                CoinType.BITCOIN_CASH, CoinType.DOGECOIN, CoinType.POLYGON,
                CoinType.BINANCE_COIN, CoinType.SOLANA, CoinType.TRON
            ),
            deepLinkScheme = "ledgerlive"
        )
        
        val TREZOR_SUITE = ExternalWalletApp(
            name = "Trezor Suite",
            packageName = "satoshilabs.trezor.trezor-suite",
            supportedCoins = listOf(
                CoinType.BITCOIN, CoinType.ETHEREUM, CoinType.LITECOIN,
                CoinType.BITCOIN_CASH, CoinType.DOGECOIN, CoinType.POLYGON,
                CoinType.BINANCE_COIN
            ),
            deepLinkScheme = "trezor"
        )
        
        val MYCELIUM = ExternalWalletApp(
            name = "Mycelium",
            packageName = "com.mycelium.wallet",
            supportedCoins = listOf(CoinType.BITCOIN, CoinType.LITECOIN, CoinType.ETHEREUM),
            deepLinkScheme = "mycelium"
        )
        
        val ELECTRUM = ExternalWalletApp(
            name = "Electrum",
            packageName = "org.electrum.electrum",
            supportedCoins = listOf(CoinType.BITCOIN, CoinType.LITECOIN, CoinType.BITCOIN_CASH),
            deepLinkScheme = "electrum"
        )
        
        val BRAVE_WALLET = ExternalWalletApp(
            name = "Brave Wallet",
            packageName = "com.brave.browser",
            supportedCoins = listOf(CoinType.ETHEREUM, CoinType.POLYGON, CoinType.BINANCE_COIN),
            deepLinkScheme = "brave"
        )
        
        val RAINBOW_WALLET = ExternalWalletApp(
            name = "Rainbow Wallet",
            packageName = "me.rainbow",
            supportedCoins = listOf(CoinType.ETHEREUM, CoinType.POLYGON, CoinType.BINANCE_COIN),
            deepLinkScheme = "rainbow"
        )
        
        val WALLET_CONNECT = ExternalWalletApp(
            name = "WalletConnect",
            packageName = "com.walletconnect",
            supportedCoins = listOf(
                CoinType.ETHEREUM, CoinType.POLYGON, CoinType.BINANCE_COIN,
                CoinType.SOLANA, CoinType.TRON
            ),
            deepLinkScheme = "wc"
        )
        
        val PHANTOM_WALLET = ExternalWalletApp(
            name = "Phantom Wallet",
            packageName = "app.phantom",
            supportedCoins = listOf(CoinType.SOLANA, CoinType.ETHEREUM, CoinType.POLYGON),
            deepLinkScheme = "phantom"
        )
        
        val SOLFLARE_WALLET = ExternalWalletApp(
            name = "Solflare Wallet",
            packageName = "com.solflare.mobile",
            supportedCoins = listOf(CoinType.SOLANA, CoinType.ETHEREUM),
            deepLinkScheme = "solflare"
        )
        
        val YOROI_WALLET = ExternalWalletApp(
            name = "Yoroi Wallet",
            packageName = "io.emurgo.yoroi",
            supportedCoins = listOf(CoinType.CARDANO),
            deepLinkScheme = "yoroi"
        )
        
        val ADALITE_WALLET = ExternalWalletApp(
            name = "AdaLite Wallet",
            packageName = "com.adalite.wallet",
            supportedCoins = listOf(CoinType.CARDANO),
            deepLinkScheme = "adalite"
        )
        
        val TRON_WALLET = ExternalWalletApp(
            name = "TronWallet",
            packageName = "com.tronlinkpro.wallet",
            supportedCoins = listOf(CoinType.TRON, CoinType.BITCOIN, CoinType.ETHEREUM),
            deepLinkScheme = "tronlink"
        )
        
        val KLEVER_WALLET = ExternalWalletApp(
            name = "Klever Wallet",
            packageName = "com.klever.wallet",
            supportedCoins = listOf(CoinType.TRON, CoinType.BITCOIN, CoinType.ETHEREUM, CoinType.LITECOIN),
            deepLinkScheme = "klever"
        )
        
        val BITKEEP_WALLET = ExternalWalletApp(
            name = "BitKeep Wallet",
            packageName = "com.bitkeep.wallet",
            supportedCoins = listOf(
                CoinType.BITCOIN, CoinType.ETHEREUM, CoinType.LITECOIN,
                CoinType.BITCOIN_CASH, CoinType.DOGECOIN, CoinType.BINANCE_COIN,
                CoinType.POLYGON, CoinType.SOLANA, CoinType.TRON, CoinType.CARDANO
            ),
            deepLinkScheme = "bitkeep"
        )
        
        val SAFE_WALLET = ExternalWalletApp(
            name = "Safe Wallet",
            packageName = "io.gnosis.safe",
            supportedCoins = listOf(CoinType.ETHEREUM, CoinType.POLYGON, CoinType.BINANCE_COIN),
            deepLinkScheme = "safe"
        )
        
        val ARGENT_WALLET = ExternalWalletApp(
            name = "Argent Wallet",
            packageName = "io.argent.wallet",
            supportedCoins = listOf(CoinType.ETHEREUM, CoinType.POLYGON, CoinType.BINANCE_COIN),
            deepLinkScheme = "argent"
        )
        
        val ZERION_WALLET = ExternalWalletApp(
            name = "Zerion Wallet",
            packageName = "io.zerion.wallet",
            supportedCoins = listOf(
                CoinType.ETHEREUM, CoinType.POLYGON, CoinType.BINANCE_COIN,
                CoinType.SOLANA
            ),
            deepLinkScheme = "zerion"
        )
        
        val IM_TOKEN_WALLET = ExternalWalletApp(
            name = "imToken Wallet",
            packageName = "im.token.im",
            supportedCoins = listOf(
                CoinType.BITCOIN, CoinType.ETHEREUM, CoinType.LITECOIN,
                CoinType.BITCOIN_CASH, CoinType.DOGECOIN, CoinType.BINANCE_COIN,
                CoinType.POLYGON, CoinType.SOLANA, CoinType.TRON
            ),
            deepLinkScheme = "imtokenv2"
        )
        
        val MATH_WALLET = ExternalWalletApp(
            name = "MathWallet",
            packageName = "com.mathwallet.android",
            supportedCoins = listOf(
                CoinType.BITCOIN, CoinType.ETHEREUM, CoinType.LITECOIN,
                CoinType.BITCOIN_CASH, CoinType.DOGECOIN, CoinType.BINANCE_COIN,
                CoinType.POLYGON, CoinType.SOLANA, CoinType.TRON, CoinType.CARDANO
            ),
            deepLinkScheme = "mathwallet"
        )
        
        val TOKEN_POCKET = ExternalWalletApp(
            name = "TokenPocket",
            packageName = "com.tokenpocket.pocket",
            supportedCoins = listOf(
                CoinType.BITCOIN, CoinType.ETHEREUM, CoinType.LITECOIN,
                CoinType.BITCOIN_CASH, CoinType.DOGECOIN, CoinType.BINANCE_COIN,
                CoinType.POLYGON, CoinType.SOLANA, CoinType.TRON
            ),
            deepLinkScheme = "tpoutside"
        )
        
        fun getAllWalletApps(): List<ExternalWalletApp> {
            return listOf(
                TRUST_WALLET, META_MASK, COINBASE_WALLET, 
                BINANCE_WALLET, EXODUS, ATOMIC_WALLET,
                LEDGER_LIVE, TREZOR_SUITE, MYCELIUM, ELECTRUM,
                BRAVE_WALLET, RAINBOW_WALLET, WALLET_CONNECT,
                PHANTOM_WALLET, SOLFLARE_WALLET, YOROI_WALLET,
                ADALITE_WALLET, TRON_WALLET, KLEVER_WALLET,
                BITKEEP_WALLET, SAFE_WALLET, ARGENT_WALLET,
                ZERION_WALLET, IM_TOKEN_WALLET, MATH_WALLET,
                TOKEN_POCKET
            )
        }
        
        fun getWalletsForCoin(coinType: CoinType): List<ExternalWalletApp> {
            return getAllWalletApps().filter { 
                coinType in it.supportedCoins 
            }
        }
    }
    
    /**
     * Generate deep link for payment
     */
    fun generatePaymentDeepLink(
        address: String, 
        amount: BigDecimal, 
        coinType: CoinType
    ): String {
        return when (deepLinkScheme) {
            "trust" -> "trust://send?address=$address&amount=${amount.toPlainString()}&asset=${coinType.symbol.lowercase()}"
            "metamask" -> "metamask://send/?to=$address&value=${(amount.multiply(BigDecimal("1e18"))).toPlainString()}"
            "cbwallet" -> "cbwallet://send?address=$address&amount=${amount.toPlainString()}&currency=${coinType.symbol}"
            "binance" -> "binance://payment?address=$address&amount=${amount.toPlainString()}&coin=${coinType.symbol}"
            "exodus" -> "exodus://send?address=$address&amount=${amount.toPlainString()}&currency=${coinType.symbol}"
            "atomic" -> "atomic://send?address=$address&amount=${amount.toPlainString()}&currency=${coinType.symbol}"
            "ledgerlive" -> "ledgerlive://send?address=$address&amount=${amount.toPlainString()}&currency=${coinType.symbol}"
            "trezor" -> "trezor://send?address=$address&amount=${amount.toPlainString()}&currency=${coinType.symbol}"
            "mycelium" -> "mycelium://send?address=$address&amount=${amount.toPlainString()}"
            "electrum" -> "electrum://send?address=$address&amount=${amount.toPlainString()}"
            "brave" -> "brave://wallet?address=$address&amount=${amount.toPlainString()}&currency=${coinType.symbol}"
            "rainbow" -> "rainbow://send?address=$address&amount=${amount.toPlainString()}&currency=${coinType.symbol}"
            "wc" -> "wc://send?address=$address&amount=${amount.toPlainString()}&currency=${coinType.symbol}"
            "phantom" -> "phantom://send?address=$address&amount=${amount.toPlainString()}&currency=${coinType.symbol}"
            "solflare" -> "solflare://send?address=$address&amount=${amount.toPlainString()}&currency=${coinType.symbol}"
            "yoroi" -> "yoroi://send?address=$address&amount=${amount.toPlainString()}"
            "adalite" -> "adalite://send?address=$address&amount=${amount.toPlainString()}"
            "tronlink" -> "tronlink://send?address=$address&amount=${amount.toPlainString()}&currency=${coinType.symbol}"
            "klever" -> "klever://send?address=$address&amount=${amount.toPlainString()}&currency=${coinType.symbol}"
            "bitkeep" -> "bitkeep://send?address=$address&amount=${amount.toPlainString()}&currency=${coinType.symbol}"
            "safe" -> "safe://send?address=$address&amount=${amount.toPlainString()}&currency=${coinType.symbol}"
            "argent" -> "argent://send?address=$address&amount=${amount.toPlainString()}&currency=${coinType.symbol}"
            "zerion" -> "zerion://send?address=$address&amount=${amount.toPlainString()}&currency=${coinType.symbol}"
            "imtokenv2" -> "imtokenv2://send?address=$address&amount=${amount.toPlainString()}&currency=${coinType.symbol}"
            "mathwallet" -> "mathwallet://send?address=$address&amount=${amount.toPlainString()}&currency=${coinType.symbol}"
            "tpoutside" -> "tpoutside://send?address=$address&amount=${amount.toPlainString()}&currency=${coinType.symbol}"
            else -> "https://$deepLinkScheme.com/send?address=$address&amount=${amount.toPlainString()}&currency=${coinType.symbol}"
        }
    }
    
    /**
     * Check if wallet app is installed
     */
    fun isInstalled(): Boolean {
        // In a real Android app, this would check if the package is installed
        // For SDK purposes, we'll return true as a default
        return true
    }
}

/**
 * Enhanced USD Payment Request with external wallet integration
 */
data class UsdPaymentRequestWithWalletSelection(
    val usdPaymentRequest: UsdPaymentRequest,
    val availableWalletApps: List<ExternalWalletApp>,
    val selectedWalletApp: ExternalWalletApp? = null,
    val walletSelectionRequired: Boolean = true
) {
    /**
     * Get payment deep link for selected wallet
     */
    fun getPaymentDeepLink(): String? {
        return selectedWalletApp?.generatePaymentDeepLink(
            address = usdPaymentRequest.customerAddress,
            amount = usdPaymentRequest.cryptoAmount,
            coinType = usdPaymentRequest.coinType
        )
    }
    
    /**
     * Select wallet app
     */
    fun selectWallet(walletApp: ExternalWalletApp): UsdPaymentRequestWithWalletSelection {
        return copy(
            selectedWalletApp = walletApp,
            walletSelectionRequired = false
        )
    }
    
    /**
     * Get formatted payment info with wallet selection
     */
    fun getFormattedInfo(): String {
        val baseInfo = usdPaymentRequest.getFormattedInfo()
        val walletInfo = if (selectedWalletApp != null) {
            "\nBezahlen mit: ${selectedWalletApp.name}"
        } else {
            "\nBitte wählen Sie eine Wallet-App"
        }
        return baseInfo + walletInfo
    }
}

/**
 * External Wallet Integration Manager
 */
class ExternalWalletManager {
    
    /**
     * Create payment request with wallet selection
     */
    fun createPaymentWithWalletSelection(
        usdPaymentRequest: UsdPaymentRequest,
        coinType: CoinType
    ): UsdPaymentRequestWithWalletSelection {
        val availableWallets = ExternalWalletApp.getWalletsForCoin(coinType)
        
        return UsdPaymentRequestWithWalletSelection(
            usdPaymentRequest = usdPaymentRequest,
            availableWalletApps = availableWallets,
            walletSelectionRequired = availableWallets.isNotEmpty()
        )
    }
    
    /**
     * Get all supported wallet apps
     */
    fun getAllSupportedWallets(): List<ExternalWalletApp> {
        return ExternalWalletApp.getAllWalletApps()
    }
    
    /**
     * Get wallet apps for specific cryptocurrency
     */
    fun getWalletsForCryptocurrency(coinType: CoinType): List<ExternalWalletApp> {
        return ExternalWalletApp.getWalletsForCoin(coinType)
    }
    
    /**
     * Generate payment deep link
     */
    fun generatePaymentDeepLink(
        walletApp: ExternalWalletApp,
        address: String,
        amount: BigDecimal,
        coinType: CoinType
    ): String {
        return walletApp.generatePaymentDeepLink(address, amount, coinType)
    }
    
    /**
     * Check if wallet app supports specific cryptocurrency
     */
    fun isCoinSupported(walletApp: ExternalWalletApp, coinType: CoinType): Boolean {
        return coinType in walletApp.supportedCoins
    }
    
    /**
     * Get wallet app by package name
     */
    fun getWalletByPackageName(packageName: String): ExternalWalletApp? {
        return ExternalWalletApp.getAllWalletApps().find { 
            it.packageName == packageName 
        }
    }
}

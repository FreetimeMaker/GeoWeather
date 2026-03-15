package com.freetime.sdk.payment.config

import com.freetime.sdk.payment.CoinType

/**
 * User wallet configuration for app-level payment processing
 * Allows users to configure their own wallet addresses in the app
 */
data class UserWalletConfig(
    val coinType: CoinType,
    val address: String,
    val name: String? = null,
    val isActive: Boolean = true,
    val isAccepted: Boolean = true // New field to indicate if user accepts this crypto
)

/**
 * Manager for user wallet configurations
 * Handles storage and retrieval of user wallet addresses
 */
class UserWalletConfigManager {
    private val walletConfigs = mutableMapOf<CoinType, UserWalletConfig>()
    private val acceptedCryptocurrencies = mutableSetOf<CoinType>()
    
    /**
     * Add or update user wallet configuration
     */
    fun setUserWallet(config: UserWalletConfig) {
        walletConfigs[config.coinType] = config
        if (config.isAccepted) {
            acceptedCryptocurrencies.add(config.coinType)
        } else {
            acceptedCryptocurrencies.remove(config.coinType)
        }
    }
    
    /**
     * Set user wallet address by coin type
     */
    fun setUserWalletAddress(
        coinType: CoinType,
        address: String,
        name: String? = null,
        isActive: Boolean = true,
        isAccepted: Boolean = true
    ) {
        val config = UserWalletConfig(
            coinType = coinType,
            address = address,
            name = name,
            isActive = isActive,
            isAccepted = isAccepted
        )
        setUserWallet(config)
    }
    
    /**
     * Get user wallet configuration by coin type
     */
    fun getUserWallet(coinType: CoinType): UserWalletConfig? {
        return walletConfigs[coinType]
    }
    
    /**
     * Get user wallet address by coin type
     */
    fun getUserWalletAddress(coinType: CoinType): String? {
        return walletConfigs[coinType]?.address
    }
    
    /**
     * Get all user wallet configurations
     */
    fun getAllUserWallets(): Map<CoinType, UserWalletConfig> {
        return walletConfigs.toMap()
    }
    
    /**
     * Get only active user wallet configurations
     */
    fun getActiveUserWallets(): Map<CoinType, UserWalletConfig> {
        return walletConfigs.filter { it.value.isActive }
    }
    
    /**
     * Get only accepted cryptocurrencies (user has chosen to accept these)
     */
    fun getAcceptedCryptocurrencies(): Set<CoinType> {
        return acceptedCryptocurrencies.toSet()
    }
    
    /**
     * Get wallets for accepted cryptocurrencies only
     */
    fun getAcceptedWallets(): Map<CoinType, UserWalletConfig> {
        return walletConfigs.filter { it.value.isAccepted && it.value.isActive }
    }
    
    /**
     * Set which cryptocurrencies the user wants to accept
     */
    fun setAcceptedCryptocurrencies(cryptocurrencies: Set<CoinType>) {
        acceptedCryptocurrencies.clear()
        acceptedCryptocurrencies.addAll(cryptocurrencies)
        
        // Update wallet configs
        walletConfigs.forEach { (coinType, config) ->
            walletConfigs[coinType] = config.copy(isAccepted = cryptocurrencies.contains(coinType))
        }
    }
    
    /**
     * Add a cryptocurrency to the accepted list
     */
    fun addAcceptedCryptocurrency(coinType: CoinType) {
        acceptedCryptocurrencies.add(coinType)
        walletConfigs[coinType]?.let { config ->
            walletConfigs[coinType] = config.copy(isAccepted = true)
        }
    }
    
    /**
     * Remove a cryptocurrency from the accepted list
     */
    fun removeAcceptedCryptocurrency(coinType: CoinType) {
        acceptedCryptocurrencies.remove(coinType)
        walletConfigs[coinType]?.let { config ->
            walletConfigs[coinType] = config.copy(isAccepted = false)
        }
    }
    
    /**
     * Check if user accepts a specific cryptocurrency
     */
    fun isCryptocurrencyAccepted(coinType: CoinType): Boolean {
        return acceptedCryptocurrencies.contains(coinType)
    }
    
    /**
     * Check if user has configured wallet for coin type
     */
    fun hasUserWallet(coinType: CoinType): Boolean {
        return walletConfigs.containsKey(coinType) && walletConfigs[coinType]?.isActive == true
    }
    
    /**
     * Check if user has configured and accepts a specific cryptocurrency
     */
    fun hasAcceptedWallet(coinType: CoinType): Boolean {
        return hasUserWallet(coinType) && isCryptocurrencyAccepted(coinType)
    }
    
    /**
     * Activate/deactivate user wallet
     */
    fun setUserWalletActive(coinType: CoinType, isActive: Boolean) {
        walletConfigs[coinType]?.let { config ->
            walletConfigs[coinType] = config.copy(isActive = isActive)
        }
    }
    
    /**
     * Set whether user accepts a cryptocurrency
     */
    fun setCryptocurrencyAccepted(coinType: CoinType, isAccepted: Boolean) {
        if (isAccepted) {
            addAcceptedCryptocurrency(coinType)
        } else {
            removeAcceptedCryptocurrency(coinType)
        }
    }
    
    /**
     * Remove user wallet configuration
     */
    fun removeUserWallet(coinType: CoinType): UserWalletConfig? {
        val removed = walletConfigs.remove(coinType)
        if (removed != null) {
            acceptedCryptocurrencies.remove(coinType)
        }
        return removed
    }
    
    /**
     * Clear all user wallet configurations
     */
    fun clearAllUserWallets() {
        walletConfigs.clear()
        acceptedCryptocurrencies.clear()
    }
    
    /**
     * Get available cryptocurrencies that user can select from
     */
    fun getAvailableCryptocurrencies(): List<CoinType> {
        return listOf(
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
    }
    
    /**
     * Get unconfigured cryptocurrencies that user might want to add
     */
    fun getUnconfiguredCryptocurrencies(): List<CoinType> {
        val available = getAvailableCryptocurrencies()
        val configured = walletConfigs.keys
        return available.filter { !configured.contains(it) }
    }
}

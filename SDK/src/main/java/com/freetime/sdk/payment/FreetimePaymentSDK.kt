package com.freetime.sdk.payment

import com.freetime.sdk.payment.config.UserWalletConfigManager
import com.freetime.sdk.payment.conversion.*
import com.freetime.sdk.payment.conversion.ExternalWalletManager
import com.freetime.sdk.payment.conversion.ExternalWalletApp
import com.freetime.sdk.payment.gateway.*
import com.freetime.sdk.payment.providers.*
import com.freetime.sdk.payment.crypto.*
import com.freetime.sdk.payment.fee.FeeBreakdown
import com.freetime.sdk.payment.fee.FeeManager
import java.math.BigDecimal
import java.security.KeyPair

/**
 * Main SDK class for multi-cryptocurrency payment processing
 * 
 * This is a completely self-contained, open-source SDK that doesn't depend on any external services.
 * All cryptographic operations are performed locally.
 */
class FreetimePaymentSDK {
    
    private val paymentProviders = mutableMapOf<CoinType, PaymentInterface>()
    private val donationProvider = DonationProvider()
    private val feeManager = FeeManager()
    private val userWalletConfigManager = UserWalletConfigManager()
    
    init {
        // Initialize payment providers for each supported coin
        initializePaymentProviders()
    }
    
    /**
     * Initialize payment providers for all supported cryptocurrencies
     */
    private fun initializePaymentProviders() {
        paymentProviders[CoinType.BITCOIN] = BitcoinPaymentProvider()
        paymentProviders[CoinType.ETHEREUM] = EthereumPaymentProvider()
        paymentProviders[CoinType.LITECOIN] = LitecoinPaymentProvider()
        paymentProviders[CoinType.BITCOIN_CASH] = BitcoinCashPaymentProvider()
        paymentProviders[CoinType.CARDANO] = CardanoPaymentProvider()
        paymentProviders[CoinType.DOGECOIN] = DogecoinPaymentProvider()
        paymentProviders[CoinType.SOLANA] = SolanaPaymentProvider()
    }
    
    
    
    /**
     * Send cryptocurrency with automatic fee calculation
     */
    suspend fun send(
        fromAddress: String,
        toAddress: String,
        amount: BigDecimal,
        coinType: CoinType
    ): TransactionWithFees {
        
        val provider = paymentProviders[coinType] 
            ?: throw UnsupportedOperationException("Unsupported coin type: $coinType")
        
        // Get network fee estimate
        val networkFee = provider.getFeeEstimate(fromAddress, toAddress, amount, coinType)
        
        // Calculate total fees including developer fee
        val feeBreakdown = feeManager.calculateTotalFees(amount, networkFee, coinType)
        
        // Create transaction with recipient amount (original amount minus fees)
        val transaction = provider.createTransaction(
            fromAddress = fromAddress,
            toAddress = toAddress,
            amount = feeBreakdown.recipientAmount,
            coinType = coinType
        )
        
        return TransactionWithFees(
            transaction = transaction,
            feeBreakdown = feeBreakdown
        )
    }
    
    /**
     * Get fee breakdown for a transaction
     */
    fun getFeeBreakdown(
        amount: BigDecimal,
        networkFee: BigDecimal,
        coinType: CoinType
    ): FeeBreakdown {
        return feeManager.calculateTotalFees(amount, networkFee, coinType)
    }
    
    /**
     * Get fee manager
     */
    fun getFeeManager(): FeeManager = feeManager
    
    /**
     * Get fee estimate
     */
    suspend fun getFeeEstimate(
        fromAddress: String,
        toAddress: String,
        amount: BigDecimal,
        coinType: CoinType
    ): BigDecimal {
        val provider = paymentProviders[coinType]
            ?: throw UnsupportedOperationException("Unsupported coin type: $coinType")
        
        return provider.getFeeEstimate(fromAddress, toAddress, amount, coinType)
    }
    
    
    /**
     * Validate address format
     */
    fun validateAddress(address: String, coinType: CoinType): Boolean {
        val provider = paymentProviders[coinType]
            ?: return false
        
        return provider.validateAddress(address, coinType)
    }
    
    
    /**
     * Create USD Payment Gateway with automatic crypto conversion
     * Only works with cryptocurrencies that user has accepted
     */
    fun createUsdPaymentGateway(
        merchantWalletAddress: String,
        merchantCoinType: CoinType
    ): UsdPaymentGateway {
        // Validate that user accepts this cryptocurrency
        if (!isCryptocurrencyAccepted(merchantCoinType)) {
            throw IllegalArgumentException("Cryptocurrency ${merchantCoinType.coinName} is not accepted. Please select it first.")
        }
        
        return UsdPaymentGateway(this, merchantWalletAddress, merchantCoinType)
    }
    
    /**
     * Create Production USD Payment Gateway with enhanced security and reliability
     * Only works with cryptocurrencies that user has accepted
     */
    fun createProductionUsdPaymentGateway(
        merchantWalletAddress: String,
        merchantCoinType: CoinType
    ): ProductionUsdPaymentGateway {
        // Validate that user accepts this cryptocurrency
        if (!isCryptocurrencyAccepted(merchantCoinType)) {
            throw IllegalArgumentException("Cryptocurrency ${merchantCoinType.coinName} is not accepted. Please select it first.")
        }
        
        return ProductionUsdPaymentGateway(this, merchantWalletAddress, merchantCoinType)
    }
    
    /**
     * Create USD Payment Gateway for each accepted cryptocurrency
     * Returns a map of gateways for all accepted cryptocurrencies
     */
    fun createUsdPaymentGatewaysForAccepted(): Map<CoinType, UsdPaymentGateway> {
        val gateways = mutableMapOf<CoinType, UsdPaymentGateway>()
        
        getAcceptedWallets().forEach { (coinType, config) ->
            try {
                val gateway = createUsdPaymentGateway(config.address, coinType)
                gateways[coinType] = gateway
            } catch (e: Exception) {
                // Skip if gateway creation fails
            }
        }
        
        return gateways
    }
    
    /**
     * Get supported payment options for user
     * Returns list of cryptocurrencies user can receive payments in
     */
    fun getSupportedPaymentOptions(): List<CoinType> {
        return getAcceptedCryptocurrencies().filter { hasAcceptedWallet(it) }.toList()
    }
    
    /**
     * Get currency converter for USD/crypto conversions
     */
    fun getCurrencyConverter(): CurrencyConverter {
        return CurrencyConverter()
    }
    
    /**
     * Get production currency converter with real-time rates
     */
    fun getProductionCurrencyConverter(): ProductionCurrencyConverter {
        return ProductionCurrencyConverter()
    }
    
    /**
     * Create USD Payment Gateway with external wallet integration
     */
    fun createUsdPaymentGatewayWithWalletSupport(
        merchantWalletAddress: String,
        merchantCoinType: CoinType
    ): UsdPaymentGateway {
        return UsdPaymentGateway(this, merchantWalletAddress, merchantCoinType)
    }
    
    /**
     * Get external wallet manager for wallet app integration
     */
    fun getExternalWalletManager(): ExternalWalletManager {
        return ExternalWalletManager()
    }
    
    /**
     * Get available wallet apps for specific cryptocurrency
     */
    fun getAvailableWalletApps(coinType: CoinType): List<ExternalWalletApp> {
        return ExternalWalletApp.getWalletsForCoin(coinType)
    }
    
    /**
     * Generate payment deep link for external wallet
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
     * Get user wallet configuration manager
     */
    fun getUserWalletConfigManager(): UserWalletConfigManager {
        return userWalletConfigManager
    }
    
    /**
     * Set user wallet address - simple way for users to configure their wallet
     */
    fun setUserWalletAddress(
        coinType: CoinType,
        address: String,
        name: String? = null,
        isAccepted: Boolean = true
    ) {
        // Validate address format
        if (!validateAddress(address, coinType)) {
            throw IllegalArgumentException("Invalid address format for $coinType: $address")
        }
        
        userWalletConfigManager.setUserWalletAddress(
            coinType = coinType,
            address = address,
            name = name ?: "My ${coinType.coinName} Wallet",
            isActive = true,
            isAccepted = isAccepted
        )
    }
    
    /**
     * Set which cryptocurrencies the user wants to accept
     */
    fun setAcceptedCryptocurrencies(cryptocurrencies: Set<CoinType>) {
        userWalletConfigManager.setAcceptedCryptocurrencies(cryptocurrencies)
    }
    
    /**
     * Add a cryptocurrency to the accepted list
     */
    fun addAcceptedCryptocurrency(coinType: CoinType) {
        userWalletConfigManager.addAcceptedCryptocurrency(coinType)
    }
    
    /**
     * Remove a cryptocurrency from the accepted list
     */
    fun removeAcceptedCryptocurrency(coinType: CoinType) {
        userWalletConfigManager.removeAcceptedCryptocurrency(coinType)
    }
    
    /**
     * Check if user accepts a specific cryptocurrency
     */
    fun isCryptocurrencyAccepted(coinType: CoinType): Boolean {
        return userWalletConfigManager.isCryptocurrencyAccepted(coinType)
    }
    
    /**
     * Get all accepted cryptocurrencies
     */
    fun getAcceptedCryptocurrencies(): Set<CoinType> {
        return userWalletConfigManager.getAcceptedCryptocurrencies()
    }
    
    /**
     * Get wallets for accepted cryptocurrencies only
     */
    fun getAcceptedWallets(): Map<CoinType, com.freetime.sdk.payment.config.UserWalletConfig> {
        return userWalletConfigManager.getAcceptedWallets()
    }
    
    /**
     * Check if user has configured and accepts a specific cryptocurrency
     */
    fun hasAcceptedWallet(coinType: CoinType): Boolean {
        return userWalletConfigManager.hasAcceptedWallet(coinType)
    }
    
    /**
     * Get available cryptocurrencies that user can select from
     */
    fun getAvailableCryptocurrencies(): List<CoinType> {
        return userWalletConfigManager.getAvailableCryptocurrencies()
    }
    
    /**
     * Get unconfigured cryptocurrencies that user might want to add
     */
    fun getUnconfiguredCryptocurrencies(): List<CoinType> {
        return userWalletConfigManager.getUnconfiguredCryptocurrencies()
    }
    
    /**
     * Check if user has at least one accepted cryptocurrency configured
     */
    fun hasAnyAcceptedWallet(): Boolean {
        return getAcceptedWallets().isNotEmpty()
    }
    
    /**
     * Validate that user has at least one accepted cryptocurrency configured
     */
    fun validateHasAcceptedWallets(): Boolean {
        if (!hasAnyAcceptedWallet()) {
            throw IllegalStateException("No accepted cryptocurrencies configured. Please select at least one cryptocurrency to accept.")
        }
        return true
    }
    
    /**
     * Get user's configured wallet address
     */
    fun getUserWalletAddress(coinType: CoinType): String? {
        return userWalletConfigManager.getUserWalletAddress(coinType)
    }
    
    /**
     * Check if user has configured wallet for coin type
     */
    fun hasUserWallet(coinType: CoinType): Boolean {
        return userWalletConfigManager.hasUserWallet(coinType)
    }
    
    /**
     * Get all user wallet configurations
     */
    fun getAllUserWallets(): Map<CoinType, com.freetime.sdk.payment.config.UserWalletConfig> {
        return userWalletConfigManager.getAllUserWallets()
    }
    
    /**
     * Set all required wallet addresses at once
     * Ensures that all supported cryptocurrencies have configured addresses
     */
    fun setAllRequiredWalletAddresses(walletAddresses: Map<CoinType, String>) {
        val supportedCoins = listOf(
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
        
        // Check that all supported coins are provided
        val missingCoins = supportedCoins.filter { !walletAddresses.containsKey(it) }
        if (missingCoins.isNotEmpty()) {
            throw IllegalArgumentException("Missing wallet addresses for: ${missingCoins.joinToString { it.coinName }}")
        }
        
        // Validate and set all addresses
        walletAddresses.forEach { (coinType, address) ->
            if (!validateAddress(address, coinType)) {
                throw IllegalArgumentException("Invalid address format for $coinType: $address")
            }
            
            userWalletConfigManager.setUserWalletAddress(
                coinType = coinType,
                address = address,
                name = "My ${coinType.coinName} Wallet"
            )
        }
    }
    
    /**
     * Check if all required wallet addresses are configured
     */
    fun areAllRequiredWalletsConfigured(): Boolean {
        val supportedCoins = listOf(
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
        
        return supportedCoins.all { hasUserWallet(it) }
    }
    
    /**
     * Get missing wallet configurations
     */
    fun getMissingWalletConfigurations(): List<CoinType> {
        val supportedCoins = listOf(
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
        
        return supportedCoins.filter { !hasUserWallet(it) }
    }
    
    /**
     * Validate that all required wallets are configured before proceeding
     */
    fun validateAllRequiredWallets(): Boolean {
        if (!areAllRequiredWalletsConfigured()) {
            val missing = getMissingWalletConfigurations()
            throw IllegalStateException("Missing required wallet configurations for: ${missing.joinToString { it.coinName }}")
        }
        return true
    }
    
    
    
    
    // ==================== DONATION METHODS ====================
    
    /**
     * Send a cryptocurrency donation with automatic fee calculation
     */
    suspend fun donate(
        toAddress: String,
        amount: BigDecimal,
        coinType: CoinType,
        donorName: String? = null,
        donationMessage: String? = null
    ): DonationWithFees {
        
        // Validate donation amount
        if (!donationProvider.validateDonationAmount(amount, coinType)) {
            throw IllegalArgumentException("Invalid donation amount: $amount for ${coinType.coinName}")
        }
        
        // Get network fee estimate for donation
        val networkFee = donationProvider.getDonationFeeEstimate(toAddress, amount, coinType)
        
        // Calculate total fees including developer fee
        val feeBreakdown = feeManager.calculateTotalFees(amount, networkFee, coinType)
        
        // Create donation with recipient amount (original amount minus fees)
        val donation = donationProvider.createDonation(
            toAddress = toAddress,
            amount = feeBreakdown.recipientAmount,
            coinType = coinType,
            donorName = donorName,
            donationMessage = donationMessage
        )
        
        return DonationWithFees(
            donation = donation,
            feeBreakdown = feeBreakdown
        )
    }
    
    /**
     * Send a donation without fees (full amount goes to recipient)
     */
    suspend fun donateWithoutFees(
        toAddress: String,
        amount: BigDecimal,
        coinType: CoinType,
        donorName: String? = null,
        donationMessage: String? = null
    ): Donation {
        
        // Validate donation amount
        if (!donationProvider.validateDonationAmount(amount, coinType)) {
            throw IllegalArgumentException("Invalid donation amount: $amount for ${coinType.coinName}")
        }
        
        // Create donation without fees
        val donation = donationProvider.createDonation(
            toAddress = toAddress,
            amount = amount,
            coinType = coinType,
            donorName = donorName,
            donationMessage = donationMessage
        )
        
        return donation
    }
    
    /**
     * Broadcast a signed donation to the blockchain
     */
    suspend fun broadcastDonation(donation: Donation): String {
        return donationProvider.broadcastDonation(donation)
    }
    
    /**
     * Get donation fee estimate
     */
    suspend fun getDonationFeeEstimate(
        toAddress: String,
        amount: BigDecimal,
        coinType: CoinType
    ): BigDecimal {
        return donationProvider.getDonationFeeEstimate(toAddress, amount, coinType)
    }
    
    /**
     * Get donation fee breakdown (network fee + developer fee)
     */
    fun getDonationFeeBreakdown(
        amount: BigDecimal,
        networkFee: BigDecimal,
        coinType: CoinType
    ): FeeBreakdown {
        return feeManager.calculateTotalFees(amount, networkFee, coinType)
    }
    
    /**
     * Validate donation amount
     */
    fun validateDonationAmount(amount: BigDecimal, coinType: CoinType): Boolean {
        return donationProvider.validateDonationAmount(amount, coinType)
    }
    
    /**
     * Get donation provider
     */
    fun getDonationProvider(): DonationInterface {
        return donationProvider
    }
    
    /**
     * Get donation amount selector for showing donation options
     */
    fun getDonationAmountSelector(): DonationAmountSelector {
        return DonationAmountSelector()
    }
}

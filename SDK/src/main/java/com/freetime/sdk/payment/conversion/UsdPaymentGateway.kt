package com.freetime.sdk.payment.conversion

import com.freetime.sdk.payment.*
import com.freetime.sdk.payment.gateway.*
import java.math.BigDecimal
import java.math.RoundingMode
import java.util.concurrent.ConcurrentHashMap

/**
 * Enhanced Payment Gateway with USD support and automatic crypto conversion
 */
class UsdPaymentGateway(
    private val sdk: FreetimePaymentSDK,
    private val merchantWalletAddress: String,
    private val merchantCoinType: CoinType,
    private val currencyConverter: CurrencyConverter = CurrencyConverter(),
    private val externalWalletManager: ExternalWalletManager = ExternalWalletManager()
) {
    
    private val pendingPayments = ConcurrentHashMap<String, UsdPaymentRequest>()
    private val confirmedPayments = ConcurrentHashMap<String, ConfirmedUsdPayment>()
    
    /**
     * Create a payment request in USD - automatically converts to crypto
     */
    suspend fun createUsdPaymentRequest(
        usdAmount: BigDecimal,
        customerReference: String? = null,
        description: String? = null,
        /** Optional: Externes Wallet, das der App-Besitzer bereitstellt. */
        providedWallet: String? = null,
        /** Optionale externe Adresse, an die empfangene Gelder weitergeleitet werden sollen */
        forwardToAddress: String? = null
    ): UsdPaymentRequest {
        
        // Convert USD to target cryptocurrency
        val conversionResult = currencyConverter.convertUsdToCrypto(usdAmount, merchantCoinType)
        
        if (!conversionResult.success) {
            throw IllegalArgumentException("Currency conversion failed: ${conversionResult.error}")
        }
        
        // Create crypto payment request
        val cryptoPaymentRequest = com.freetime.sdk.payment.gateway.PaymentGateway(
            sdk, merchantWalletAddress, merchantCoinType
        ).createPaymentAddress(
            amount = conversionResult.cryptoAmount!!,
            customerReference = customerReference,
            description = description,
            providedWallet = providedWallet,
            forwardToAddress = forwardToAddress
        )
        
        val usdPaymentRequest = UsdPaymentRequest(
            id = generatePaymentId(),
            customerAddress = cryptoPaymentRequest.customerAddress,
            merchantAddress = merchantWalletAddress,
            usdAmount = usdAmount,
            cryptoAmount = conversionResult.cryptoAmount!!,
            coinType = merchantCoinType,
            customerReference = customerReference,
            description = description,
            exchangeRate = conversionResult.exchangeRate!!,
            status = PaymentStatus.PENDING,
            createdAt = System.currentTimeMillis(),
            expiresAt = System.currentTimeMillis() + PAYMENT_TIMEOUT,
            cryptoPaymentRequest = cryptoPaymentRequest
        )
        
        pendingPayments[usdPaymentRequest.id] = usdPaymentRequest
        return usdPaymentRequest
    }
    
    /**
     * Check payment status with USD tracking
     */
    suspend fun checkUsdPaymentStatus(paymentId: String): PaymentStatus {
        val usdPayment = pendingPayments[paymentId] 
            ?: return PaymentStatus.NOT_FOUND
        
        // Check if payment is expired
        if (System.currentTimeMillis() > usdPayment.expiresAt) {
            pendingPayments.remove(paymentId)
            return PaymentStatus.EXPIRED
        }
        
        // Check underlying crypto payment status
        val cryptoStatus = com.freetime.sdk.payment.gateway.PaymentGateway(
            sdk, merchantWalletAddress, merchantCoinType
        ).checkPaymentStatus(usdPayment.cryptoPaymentRequest.id)
        
        if (cryptoStatus == PaymentStatus.CONFIRMED) {
            // Get final crypto amount received
            val cryptoDetails = com.freetime.sdk.payment.gateway.PaymentGateway(
                sdk, merchantWalletAddress, merchantCoinType
            ).getPaymentDetails(usdPayment.cryptoPaymentRequest.id)
            
            if (cryptoDetails != null) {
                // Convert received crypto back to USD for final accounting
                val conversionResult = currencyConverter.convertCryptoToUsd(
                    cryptoDetails.currentBalance, 
                    merchantCoinType
                )
                
                if (conversionResult.success) {
                    val confirmedPayment = ConfirmedUsdPayment(
                        usdPaymentRequest = usdPayment,
                        receivedUsdAmount = conversionResult.usdAmount!!,
                        receivedCryptoAmount = cryptoDetails.currentBalance,
                        exchangeRate = conversionResult.exchangeRate!!,
                        forwardedTxHash = cryptoDetails.forwardedTxHash,
                        confirmedAt = System.currentTimeMillis()
                    )
                    
                    confirmedPayments[paymentId] = confirmedPayment
                    pendingPayments.remove(paymentId)
                    
                    return PaymentStatus.CONFIRMED
                }
            }
        }
        
        return cryptoStatus
    }
    
    /**
     * Get USD payment details
     */
    fun getUsdPaymentDetails(paymentId: String): UsdPaymentDetails? {
        val pending = pendingPayments[paymentId]
        if (pending != null) {
            // Get current crypto status
            val cryptoDetails = com.freetime.sdk.payment.gateway.PaymentGateway(
                sdk, merchantWalletAddress, merchantCoinType
            ).getPaymentDetails(pending.cryptoPaymentRequest.id)
            
            return UsdPaymentDetails(
                usdPaymentRequest = pending,
                currentCryptoBalance = cryptoDetails?.currentBalance ?: BigDecimal.ZERO,
                remainingCryptoAmount = cryptoDetails?.remainingAmount ?: pending.cryptoAmount,
                currentUsdValue = cryptoDetails?.currentBalance?.multiply(pending.exchangeRate) ?: BigDecimal.ZERO,
                remainingUsdValue = cryptoDetails?.remainingAmount?.multiply(pending.exchangeRate) ?: pending.usdAmount
            )
        }
        
        val confirmed = confirmedPayments[paymentId]
        if (confirmed != null) {
            return UsdPaymentDetails(
                usdPaymentRequest = confirmed.usdPaymentRequest,
                currentCryptoBalance = confirmed.receivedCryptoAmount,
                remainingCryptoAmount = BigDecimal.ZERO,
                currentUsdValue = confirmed.receivedUsdAmount,
                remainingUsdValue = BigDecimal.ZERO,
                forwardedTxHash = confirmed.forwardedTxHash,
                confirmedAt = confirmed.confirmedAt
            )
        }
        
        return null
    }
    
    /**
     * Get current exchange rates
     */
    suspend fun getCurrentExchangeRates(): Map<CoinType, BigDecimal> {
        return currencyConverter.getAllExchangeRates()
    }
    
    /**
     * Cancel pending payment
     */
    fun cancelUsdPayment(paymentId: String): Boolean {
        val payment = pendingPayments.remove(paymentId)
        return payment != null
    }
    
    /**
     * Get all pending USD payments
     */
    fun getPendingUsdPayments(): List<UsdPaymentRequest> {
        return pendingPayments.values.toList()
    }
    
    /**
     * Get all confirmed USD payments
     */
    fun getConfirmedUsdPayments(): List<ConfirmedUsdPayment> {
        return confirmedPayments.values.toList()
    }
    
    /**
     * Create USD payment request with external wallet selection
     */
    suspend fun createUsdPaymentWithWalletSelection(
        usdAmount: BigDecimal,
        customerReference: String? = null,
        description: String? = null,
        providedWallet: String? = null,
        forwardToAddress: String? = null
    ): UsdPaymentRequestWithWalletSelection {
        
        // Create base USD payment request
        val usdPaymentRequest = createUsdPaymentRequest(
            usdAmount = usdAmount,
            customerReference = customerReference,
            description = description,
            providedWallet = providedWallet,
            forwardToAddress = forwardToAddress
        )
        
        // Create payment with wallet selection
        return externalWalletManager.createPaymentWithWalletSelection(
            usdPaymentRequest = usdPaymentRequest,
            coinType = merchantCoinType
        )
    }
    
    /**
     * Get available external wallet apps for this cryptocurrency
     */
    fun getAvailableWalletApps(): List<ExternalWalletApp> {
        return externalWalletManager.getWalletsForCryptocurrency(merchantCoinType)
    }
    
    /**
     * Generate payment deep link for specific wallet app
     */
    fun generatePaymentDeepLink(
        walletApp: ExternalWalletApp,
        usdPaymentRequest: UsdPaymentRequest
    ): String {
        return externalWalletManager.generatePaymentDeepLink(
            walletApp = walletApp,
            address = usdPaymentRequest.customerAddress,
            amount = usdPaymentRequest.cryptoAmount,
            coinType = usdPaymentRequest.coinType
        )
    }
    
    /**
     * Check if specific wallet app supports this cryptocurrency
     */
    fun isWalletSupported(walletApp: ExternalWalletApp): Boolean {
        return externalWalletManager.isCoinSupported(walletApp, merchantCoinType)
    }
    
    /**
     * Get all supported external wallet apps
     */
    fun getAllSupportedWalletApps(): List<ExternalWalletApp> {
        return externalWalletManager.getAllSupportedWallets()
    }
    
    private fun generatePaymentId(): String {
        return "usd_pay_${System.currentTimeMillis()}_${(1000..9999).random()}"
    }
    
    companion object {
        private const val PAYMENT_TIMEOUT = 30 * 60 * 1000L // 30 Minuten
    }
}

/**
 * Payment request with USD amount and automatic crypto conversion
 */
data class UsdPaymentRequest(
    val id: String,
    val customerAddress: String,
    val merchantAddress: String,
    val usdAmount: BigDecimal,
    val cryptoAmount: BigDecimal,
    val coinType: CoinType,
    val exchangeRate: BigDecimal,
    val customerReference: String? = null,
    val description: String? = null,
    var status: PaymentStatus = PaymentStatus.PENDING,
    val createdAt: Long,
    val expiresAt: Long,
    val cryptoPaymentRequest: com.freetime.sdk.payment.gateway.PaymentRequest,
    val metadata: Map<String, String> = emptyMap(),
    val feeAmount: BigDecimal = BigDecimal.ZERO,
    val totalUsdAmount: BigDecimal = usdAmount
) {
    /**
     * Get formatted payment info
     */
    fun getFormattedInfo(): String {
        return "$${usdAmount.setScale(2, RoundingMode.HALF_UP)} USD = " +
               "${cryptoAmount.setScale(8, RoundingMode.HALF_UP)} ${coinType.symbol} " +
               "(Rate: $${exchangeRate.setScale(2, RoundingMode.HALF_UP)})"
    }
}

/**
 * Confirmed USD payment with final amounts
 */
data class ConfirmedUsdPayment(
    val usdPaymentRequest: UsdPaymentRequest,
    val receivedUsdAmount: BigDecimal,
    val receivedCryptoAmount: BigDecimal,
    val exchangeRate: BigDecimal,
    val forwardedTxHash: String?,
    val confirmedAt: Long,
    val processingFee: BigDecimal = BigDecimal.ZERO
)

/**
 * Detailed USD payment information
 */
data class UsdPaymentDetails(
    val usdPaymentRequest: UsdPaymentRequest,
    val currentCryptoBalance: BigDecimal,
    val remainingCryptoAmount: BigDecimal,
    val currentUsdValue: BigDecimal,
    val remainingUsdValue: BigDecimal,
    val forwardedTxHash: String? = null,
    val confirmedAt: Long? = null,
    val processingFee: BigDecimal = BigDecimal.ZERO
)

package com.freetime.sdk.payment.conversion

import com.freetime.sdk.payment.*
import com.freetime.sdk.payment.gateway.*
import java.math.BigDecimal
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Production-ready USD Payment Gateway with enhanced security and reliability
 */
class ProductionUsdPaymentGateway(
    private val sdk: FreetimePaymentSDK,
    private val merchantWalletAddress: String,
    private val merchantCoinType: CoinType,
    private val currencyConverter: ProductionCurrencyConverter = ProductionCurrencyConverter(),
    private val config: PaymentGatewayConfig = PaymentGatewayConfig.default()
) {
    
    private val pendingPayments = ConcurrentHashMap<String, UsdPaymentRequest>()
    private val confirmedPayments = ConcurrentHashMap<String, ConfirmedUsdPayment>()
    private val paymentCounter = AtomicLong(0)
    private val paymentMutex = Mutex()
    
    /**
     * Create a production-grade USD payment request
     */
    suspend fun createUsdPaymentRequest(
        usdAmount: BigDecimal,
        customerReference: String? = null,
        description: String? = null,
        metadata: Map<String, String> = emptyMap(),
        /** Optional: Externes Wallet, das der App-Besitzer bereitstellt. */
        providedWallet: String? = null,
        /** Optionale externe Adresse, an die empfangene Gelder weitergeleitet werden sollen */
        forwardToAddress: String? = null
    ): UsdPaymentRequest {
        
        return paymentMutex.withLock {
            try {
                // Validate USD amount
                if (usdAmount <= BigDecimal.ZERO) {
                    throw IllegalArgumentException("USD amount must be positive")
                }
                
                if (usdAmount < config.minUsdAmount) {
                    throw IllegalArgumentException("Minimum USD amount is $${config.minUsdAmount}")
                }
                
                if (usdAmount > config.maxUsdAmount) {
                    throw IllegalArgumentException("Maximum USD amount is $${config.maxUsdAmount}")
                }
                
                // Convert USD to cryptocurrency
                val conversionResult = currencyConverter.convertUsdToCrypto(usdAmount, merchantCoinType)
                
                if (!conversionResult.success) {
                    throw IllegalArgumentException("Currency conversion failed: ${conversionResult.error}")
                }
                
                // Apply conversion fee
                val feeAmount = usdAmount.multiply(config.conversionFeePercentage).divide(BigDecimal("100"))
                val totalUsdAmount = usdAmount.add(feeAmount)
                
                // Convert total amount including fee
                val totalConversionResult = currencyConverter.convertUsdToCrypto(totalUsdAmount, merchantCoinType)
                
                if (!totalConversionResult.success) {
                    throw IllegalArgumentException("Total amount conversion failed: ${totalConversionResult.error}")
                }
                
                // Create crypto payment request
                val cryptoPaymentRequest = com.freetime.sdk.payment.gateway.PaymentGateway(
                    sdk, merchantWalletAddress, merchantCoinType
                ).createPaymentAddress(
                    amount = totalConversionResult.cryptoAmount!!,
                    customerReference = customerReference,
                    description = description,
                    providedWallet = providedWallet,
                    forwardToAddress = forwardToAddress
                )
                
                val paymentId = generatePaymentId()
                val usdPaymentRequest = UsdPaymentRequest(
                    id = paymentId,
                    customerAddress = cryptoPaymentRequest.customerAddress,
                    merchantAddress = merchantWalletAddress,
                    usdAmount = usdAmount,
                    feeAmount = feeAmount,
                    totalUsdAmount = totalUsdAmount,
                    cryptoAmount = totalConversionResult.cryptoAmount!!,
                    coinType = merchantCoinType,
                    customerReference = customerReference,
                    description = description,
                    exchangeRate = totalConversionResult.exchangeRate!!,
                    status = PaymentStatus.PENDING,
                    createdAt = System.currentTimeMillis(),
                    expiresAt = System.currentTimeMillis() + config.paymentTimeoutMs,
                    cryptoPaymentRequest = cryptoPaymentRequest,
                    metadata = metadata
                )
                
                pendingPayments[paymentId] = usdPaymentRequest
                usdPaymentRequest
            } catch (e: Exception) {
                throw IllegalArgumentException("Failed to create USD payment request: ${e.message}", e)
            }
        }
    }
    
    /**
     * Check payment status with enhanced monitoring
     */
    suspend fun checkUsdPaymentStatus(paymentId: String): PaymentStatus {
        val usdPayment = pendingPayments[paymentId] 
            ?: return PaymentStatus.NOT_FOUND
        
        // Check if payment is expired
        if (System.currentTimeMillis() > usdPayment.expiresAt) {
            paymentMutex.withLock {
                pendingPayments.remove(paymentId)
            }
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
                        confirmedAt = System.currentTimeMillis(),
                        processingFee = usdPayment.feeAmount
                    )
                    
                    paymentMutex.withLock {
                        confirmedPayments[paymentId] = confirmedPayment
                        pendingPayments.remove(paymentId)
                    }
                    
                    return PaymentStatus.CONFIRMED
                }
            }
        }
        
        return cryptoStatus
    }
    
    /**
     * Get detailed USD payment information
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
                remainingUsdValue = cryptoDetails?.remainingAmount?.multiply(pending.exchangeRate) ?: pending.totalUsdAmount
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
                confirmedAt = confirmed.confirmedAt,
                processingFee = confirmed.processingFee
            )
        }
        
        return null
    }
    
    /**
     * Cancel pending payment with validation
     */
    suspend fun cancelUsdPayment(paymentId: String): PaymentCancellationResult {
        return paymentMutex.withLock {
            val payment = pendingPayments[paymentId]
                ?: return@withLock PaymentCancellationResult(
                    success = false,
                    error = "Payment not found",
                    paymentId = paymentId
                )
            
            // Check if payment can be cancelled
            if (payment.status != PaymentStatus.PENDING) {
                return@withLock PaymentCancellationResult(
                    success = false,
                    error = "Payment cannot be cancelled (status: ${payment.status})",
                    paymentId = paymentId
                )
            }
            
            // Check if payment is expired
            if (System.currentTimeMillis() > payment.expiresAt) {
                pendingPayments.remove(paymentId)
                return@withLock PaymentCancellationResult(
                    success = false,
                    error = "Payment already expired",
                    paymentId = paymentId
                )
            }
            
            // Cancel payment
            pendingPayments.remove(paymentId)
            PaymentCancellationResult(
                success = true,
                paymentId = paymentId,
                cancelledAt = System.currentTimeMillis()
            )
        }
    }
    
    /**
     * Get gateway health status
     */
    fun getGatewayHealthStatus(): GatewayHealthStatus {
        val converterHealth = currencyConverter.getHealthStatus()
        val currentTime = System.currentTimeMillis()
        
        val expiredPayments = pendingPayments.values.count { 
            currentTime > it.expiresAt 
        }
        
        return GatewayHealthStatus(
            isHealthy = converterHealth.isHealthy && expiredPayments == 0,
            converterHealth = converterHealth,
            pendingPaymentsCount = pendingPayments.size,
            confirmedPaymentsCount = confirmedPayments.size,
            expiredPaymentsCount = expiredPayments,
            lastActivity = currentTime
        )
    }
    
    /**
     * Clean up expired payments
     */
    suspend fun cleanupExpiredPayments(): Int {
        return paymentMutex.withLock {
            val currentTime = System.currentTimeMillis()
            val expiredPayments = pendingPayments.values.filter { 
                currentTime > it.expiresAt 
            }
            
            expiredPayments.forEach { payment ->
                pendingPayments.remove(payment.id)
            }
            
            expiredPayments.size
        }
    }
    
    /**
     * Get payment statistics
     */
    fun getPaymentStatistics(): PaymentStatistics {
        val currentTime = System.currentTimeMillis()
        val pending = pendingPayments.values.toList()
        val confirmed = confirmedPayments.values.toList()
        
        val totalUsdVolume = confirmed.sumOf { it.receivedUsdAmount }
        val totalFeesCollected = confirmed.sumOf { it.processingFee }
        
        val recentPayments = confirmed.filter { 
            currentTime - it.confirmedAt < 24 * 60 * 60 * 1000L // Last 24 hours
        }
        
        return PaymentStatistics(
            totalPayments = confirmed.size,
            pendingPayments = pending.size,
            totalUsdVolume = totalUsdVolume,
            totalFeesCollected = totalFeesCollected,
            averagePaymentAmount = if (confirmed.isNotEmpty()) {
                totalUsdVolume.divide(BigDecimal(confirmed.size), 2, BigDecimal.ROUND_HALF_UP)
            } else BigDecimal.ZERO,
            recentPayments24h = recentPayments.size,
            recentVolume24h = recentPayments.sumOf { it.receivedUsdAmount }
        )
    }
    
    private fun generatePaymentId(): String {
        val counter = paymentCounter.incrementAndGet()
        return "usd_pay_${System.currentTimeMillis()}_${counter}"
    }
}

/**
 * Configuration for production payment gateway
 */
data class PaymentGatewayConfig(
    val minUsdAmount: BigDecimal = BigDecimal("1.00"),
    val maxUsdAmount: BigDecimal = BigDecimal("10000.00"),
    val conversionFeePercentage: BigDecimal = BigDecimal("1.0"), // 1% fee
    val paymentTimeoutMs: Long = 30 * 60 * 1000L // 30 minutes
) {
    companion object {
        fun default() = PaymentGatewayConfig()
        fun lowFee() = PaymentGatewayConfig(
            conversionFeePercentage = BigDecimal("0.5") // 0.5% fee
        )
        fun highVolume() = PaymentGatewayConfig(
            maxUsdAmount = BigDecimal("50000.00"),
            conversionFeePercentage = BigDecimal("0.8") // 0.8% fee
        )
    }
}

/**
 * Result of payment cancellation
 */
data class PaymentCancellationResult(
    val success: Boolean,
    val paymentId: String,
    val error: String? = null,
    val cancelledAt: Long? = null
)

/**
 * Gateway health status
 */
data class GatewayHealthStatus(
    val isHealthy: Boolean,
    val converterHealth: ConversionHealth,
    val pendingPaymentsCount: Int,
    val confirmedPaymentsCount: Int,
    val expiredPaymentsCount: Int,
    val lastActivity: Long
)

/**
 * Payment statistics
 */
data class PaymentStatistics(
    val totalPayments: Int,
    val pendingPayments: Int,
    val totalUsdVolume: BigDecimal,
    val totalFeesCollected: BigDecimal,
    val averagePaymentAmount: BigDecimal,
    val recentPayments24h: Int,
    val recentVolume24h: BigDecimal
)

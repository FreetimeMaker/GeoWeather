package com.freetime.geoweather.freetimesdk.mock

import android.content.Context
import com.freetime.geoweather.freetimesdk.models.*
import kotlinx.coroutines.delay

// Mock implementation of FreetimeSDK classes since real SDK may not be available
object MockFreetimeSDK {
    
    fun initialize(context: Context, appId: String) {
        // Mock initialization
    }
    
    fun getPaymentManager(context: Context): MockPaymentManager {
        return MockPaymentManager()
    }
    
    fun getWalletManager(context: Context): MockWalletManager {
        return MockWalletManager()
    }
    
    fun getAnalyticsManager(context: Context): MockAnalyticsManager {
        return MockAnalyticsManager()
    }
    
    // New v1.0.7 features
    fun getSubscriptionManager(context: Context): MockSubscriptionManager {
        return MockSubscriptionManager()
    }
    
    fun getSecurityManager(context: Context): MockSecurityManager {
        return MockSecurityManager()
    }
}

class MockPaymentManager {
    fun getSupportedMethods(): List<PaymentMethod> {
        return listOf(
            PaymentMethod.OXAPAY,
            PaymentMethod.COINBASE,
            PaymentMethod.BITCOIN,
            PaymentMethod.ETHEREUM,
            PaymentMethod.USDT,
            PaymentMethod.USDC,
            PaymentMethod.SHIB,
            PaymentMethod.DOGE,
            PaymentMethod.TRON,
            PaymentMethod.LTC,
            PaymentMethod.USD_GATEWAY,
        )
    }
    
    suspend fun processPayment(
        method: PaymentMethod,
        amount: DonationAmount,
        merchantId: String,
        description: String
    ): PaymentResult {
        // Simulate payment processing
        delay(2000)
        
        // Mock success for demonstration
        return PaymentResult(
            success = true,
            transactionId = "txn_${System.currentTimeMillis()}",
            receiptUrl = "https://receipt.freetimesdk.com/txn_${System.currentTimeMillis()}",
            processedAmount = amount.amount,
            currency = amount.currency,
            timestamp = System.currentTimeMillis()
        )
    }
    
    // New v1.0.7 features
    suspend fun processRecurringPayment(
        method: PaymentMethod,
        amount: DonationAmount,
        interval: String, // "monthly", "yearly"
        merchantId: String,
        description: String
    ): PaymentResult {
        delay(2500)
        return PaymentResult(
            success = true,
            transactionId = "recurring_${System.currentTimeMillis()}",
            receiptUrl = "https://receipt.freetimesdk.com/recurring_${System.currentTimeMillis()}",
            processedAmount = amount.amount,
            currency = amount.currency,
            timestamp = System.currentTimeMillis()
        )
    }
    
    suspend fun refundPayment(
        transactionId: String,
        reason: String
    ): RefundResult {
        delay(1500)
        return RefundResult(
            success = true,
            refundId = "refund_${System.currentTimeMillis()}",
            amount = 10.0,
            currency = "USD"
        )
    }
}

class MockWalletManager {
    fun getSupportedWallets(): List<WalletType> {
        return listOf(
            WalletType.METAMASK,
            WalletType.TRUSTWALLET,
            WalletType.COINBASE,
            WalletType.BINANCE,
            WalletType.EXODUS,
            WalletType.ATOMIC,
        )
    }
    
    suspend fun connectWallet(walletType: WalletType): WalletConnectionResult {
        // Simulate wallet connection
        delay(1500)
        
        // Mock success for demonstration
        return WalletConnectionResult(
            success = true,
            walletAddress = "0x${(100000000000000000L..999999999999999999L).random().toString(16)}",
            walletName = walletType.displayName,
            chainId = when (walletType) {
                WalletType.METAMASK, WalletType.TRUSTWALLET -> "1"
                else -> "56" // BSC
            },
            balance = 1000.0
        )
    }
    
    suspend fun signTransaction(
        walletType: WalletType,
        transactionData: String
    ): SigningResult {
        delay(1000)
        return SigningResult(
            success = true,
            signature = "0x${(1000..9999).random().toString().repeat(64)}",
            transactionHash = "0x${(1000..9999).random().toString().repeat(64)}"
        )
    }
}

class MockAnalyticsManager {
    fun trackDonationInitiated(method: PaymentMethod, amount: DonationAmount) {
        // Mock analytics tracking
    }
    
    fun trackDonationCompleted(method: PaymentMethod, amount: DonationAmount) {
        // Mock analytics tracking
    }
    
    fun trackDonationFailed(method: PaymentMethod, amount: DonationAmount, error: String) {
        // Mock analytics tracking
    }
    
    fun trackWalletConnectionInitiated(walletType: WalletType) {
        // Mock analytics tracking
    }
    
    fun trackWalletConnected(walletType: WalletType) {
        // Mock analytics tracking
    }
    
    fun trackWalletConnectionFailed(walletType: WalletType, error: String) {
        // Mock analytics tracking
    }
    
    fun trackFeatureUsage(feature: String) {
        // Mock analytics tracking
    }
    
    fun trackUserAction(action: String, details: Map<String, Any>) {
        // Mock analytics tracking
    }
    
    fun trackSubscriptionEvent(event: String, planType: String, value: Double) {
        // Mock subscription analytics
    }
    
    fun trackRewardClaimed(rewardType: String, value: Double) {
        // Mock reward analytics
    }
    
    fun trackSecurityEvent(event: String, severity: String, details: Map<String, Any>) {
        // Mock security analytics
    }
}

class MockSubscriptionManager {
    fun getAvailablePlans(): List<SubscriptionPlan> {
        return listOf(
            SubscriptionPlan("basic", 4.99, "USD", "Basic Supporter"),
            SubscriptionPlan("premium", 9.99, "USD", "Premium Supporter"),
            SubscriptionPlan("ultimate", 19.99, "USD", "Ultimate Supporter")
        )
    }
    
    suspend fun subscribe(plan: SubscriptionPlan): SubscriptionResult {
        delay(2000)
        return SubscriptionResult(
            success = true,
            subscriptionId = "sub_${System.currentTimeMillis()}",
            planId = plan.id,
            status = "active",
            expiresAt = System.currentTimeMillis() + (30L * 24 * 60 * 60 * 1000) // 30 days
        )
    }
    
    suspend fun cancelSubscription(subscriptionId: String): SubscriptionResult {
        delay(1500)
        return SubscriptionResult(
            success = true,
            subscriptionId = subscriptionId,
            planId = "",
            status = "cancelled",
            expiresAt = System.currentTimeMillis()
        )
    }
}

class MockSecurityManager {
    suspend fun validateTransaction(transactionId: String): SecurityResult {
        delay(500)
        return SecurityResult(
            isValid = true,
            riskScore = 0.1,
            verificationStatus = "verified"
        )
    }
    
    suspend fun enableTwoFactorAuth(userId: String): SecurityResult {
        delay(1000)
        return SecurityResult(
            isValid = true,
            riskScore = 0.0,
            verificationStatus = "2fa_enabled"
        )
    }
}

data class SubscriptionPlan(
    val id: String,
    val price: Double,
    val currency: String,
    val name: String
)

data class SubscriptionResult(
    val success: Boolean,
    val subscriptionId: String,
    val planId: String,
    val status: String,
    val expiresAt: Long
)

data class RewardResult(
    val success: Boolean,
    val rewardId: String,
    val claimedAmount: Double,
    val currency: String,
    val claimId: String
)

data class RefundResult(
    val success: Boolean,
    val refundId: String,
    val amount: Double,
    val currency: String
)

data class SigningResult(
    val success: Boolean,
    val signature: String,
    val transactionHash: String
)

data class SecurityResult(
    val isValid: Boolean,
    val riskScore: Double,
    val verificationStatus: String
)

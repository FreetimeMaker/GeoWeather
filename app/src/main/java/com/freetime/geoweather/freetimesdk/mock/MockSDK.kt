package com.freetime.geoweather.freetimesdk.mock

import android.content.Context
import com.freetime.geoweather.freetimesdk.models.*
import kotlinx.coroutines.delay

// Mock implementation of FreetimeSDK classes since the real SDK may not be available
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
            PaymentMethod.USD_GATEWAY
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
            transactionId = "txn_${System.currentTimeMillis()}"
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
            WalletType.ATOMIC
        )
    }
    
    suspend fun connectWallet(walletType: WalletType): WalletConnectionResult {
        // Simulate wallet connection
        delay(1500)
        
        // Mock success for demonstration
        return WalletConnectionResult(
            success = true,
            walletAddress = "0x${(100000000000000000L..999999999999999999L).random().toString(16)}"
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
}

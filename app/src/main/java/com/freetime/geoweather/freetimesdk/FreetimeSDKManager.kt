package com.freetime.geoweather.freetimesdk

import android.content.Context
import android.util.Log
import com.freetime.geoweather.freetimesdk.mock.MockFreetimeSDK
import com.freetime.geoweather.freetimesdk.mock.MockPaymentManager
import com.freetime.geoweather.freetimesdk.mock.MockWalletManager
import com.freetime.geoweather.freetimesdk.mock.MockAnalyticsManager
import com.freetime.geoweather.freetimesdk.models.PaymentMethod
import com.freetime.geoweather.freetimesdk.models.WalletType
import com.freetime.geoweather.freetimesdk.models.DonationAmount
import com.freetime.geoweather.freetimesdk.models.PaymentResult
import com.freetime.geoweather.freetimesdk.models.WalletConnectionResult
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class FreetimeSDKManager private constructor(private val context: Context) {
    
    private val paymentManager: MockPaymentManager by lazy {
        MockFreetimeSDK.getPaymentManager(context)
    }
    
    private val walletManager: MockWalletManager by lazy {
        MockFreetimeSDK.getWalletManager(context)
    }
    
    private val analyticsManager: MockAnalyticsManager by lazy {
        MockFreetimeSDK.getAnalyticsManager(context)
    }
    
    companion object {
        @Volatile
        private var INSTANCE: FreetimeSDKManager? = null
        
        fun getInstance(context: Context): FreetimeSDKManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: FreetimeSDKManager(context.applicationContext).also { INSTANCE = it }
            }
        }
        
        fun initialize(context: Context) {
            MockFreetimeSDK.initialize(context, "geoweather-app")
            Log.d("FreetimeSDK", "SDK initialized for GeoWeather")
        }
    }
    
    fun processDonation(
        paymentMethod: PaymentMethod,
        amount: DonationAmount,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        CoroutineScope(Dispatchers.Main).launch {
            try {
                analyticsManager.trackDonationInitiated(paymentMethod, amount)
                
                val result = paymentManager.processPayment(
                    method = paymentMethod,
                    amount = amount,
                    merchantId = "geoweather-merchant",
                    description = "GeoWeather App Donation"
                )
                
                if (result.success) {
                    analyticsManager.trackDonationCompleted(paymentMethod, amount)
                    onSuccess()
                } else {
                    analyticsManager.trackDonationFailed(paymentMethod, amount, result.errorMessage ?: "Unknown error")
                    onError(result.errorMessage ?: "Unknown error")
                }
            } catch (e: Exception) {
                Log.e("FreetimeSDK", "Donation processing failed", e)
                onError("Payment processing failed: ${e.message}")
            }
        }
    }
    
    fun connectWallet(
        walletType: WalletType,
        onSuccess: (String) -> Unit,
        onError: (String) -> Unit
    ) {
        CoroutineScope(Dispatchers.Main).launch {
            try {
                analyticsManager.trackWalletConnectionInitiated(walletType)
                
                val result = walletManager.connectWallet(walletType)
                
                if (result.success && result.walletAddress != null) {
                    analyticsManager.trackWalletConnected(walletType)
                    onSuccess(result.walletAddress)
                } else {
                    analyticsManager.trackWalletConnectionFailed(walletType, result.errorMessage ?: "Unknown error")
                    onError(result.errorMessage ?: "Unknown error")
                }
            } catch (e: Exception) {
                Log.e("FreetimeSDK", "Wallet connection failed", e)
                onError("Wallet connection failed: ${e.message}")
            }
        }
    }
    
    fun getSupportedPaymentMethods(): List<PaymentMethod> {
        return paymentManager.getSupportedMethods()
    }
    
    fun getSupportedWallets(): List<WalletType> {
        return walletManager.getSupportedWallets()
    }
    
    fun getDonationAmounts(): List<DonationAmount> {
        return listOf(
            DonationAmount(5.0, "USD"),
            DonationAmount(10.0, "USD"),
            DonationAmount(25.0, "USD"),
            DonationAmount(50.0, "USD"),
            DonationAmount(100.0, "USD")
        )
    }
    
    fun trackAppUsage(feature: String) {
        analyticsManager.trackFeatureUsage(feature)
    }
    
    fun trackUserInteraction(action: String, details: Map<String, Any> = emptyMap()) {
        analyticsManager.trackUserAction(action, details)
    }
}

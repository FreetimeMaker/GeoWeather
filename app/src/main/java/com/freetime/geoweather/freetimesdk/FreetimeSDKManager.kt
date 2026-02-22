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
import com.freetime.geoweather.freetimesdk.models.ConversionRate
import com.freetime.geoweather.freetimesdk.models.ConversionResult
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
            Log.d("FreetimeSDK", "SDK v1.0.6 initialized for GeoWeather with API-integrated USD conversion (v1.0.7 preview)")
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
    
    // New v1.0.7 USD-to-Crypto conversion with API integration
    fun convertUSDtoCrypto(
        usdAmount: Double,
        targetCrypto: String,
        onSuccess: (ConversionResult) -> Unit,
        onError: (String) -> Unit
    ) {
        CoroutineScope(Dispatchers.Main).launch {
            try {
                // Simulate API call for conversion rate
                val conversionRates = getConversionRates()
                val rate = conversionRates[targetCrypto] ?: 1.0
                val fees = usdAmount * 0.029 // 2.9% fee
                val convertedAmount = (usdAmount - fees) / rate
                
                val result = ConversionResult(
                    success = true,
                    fromAmount = usdAmount,
                    fromCurrency = "USD",
                    toAmount = convertedAmount,
                    toCurrency = targetCrypto,
                    rate = rate,
                    fees = fees,
                    timestamp = System.currentTimeMillis()
                )
                
                analyticsManager.trackUserAction("usd_to_crypto_conversion", mapOf(
                    "usd_amount" to usdAmount,
                    "target_crypto" to targetCrypto,
                    "rate" to rate,
                    "fees" to fees
                ))
                
                onSuccess(result)
            } catch (e: Exception) {
                Log.e("FreetimeSDK", "USD to Crypto conversion failed", e)
                onError("Conversion failed: ${e.message}")
            }
        }
    }
    
    // Mock conversion rates API (in real SDK this would be from live API)
    private fun getConversionRates(): Map<String, Double> {
        return mapOf(
            "BTC" to 43250.0,
            "ETH" to 2340.0,
            "USDT" to 1.0,
            "USDC" to 1.0,
            "SHIB" to 0.000025,
            "DOGE" to 0.062,
            "TRON" to 0.124,
            "LTC" to 67.5
        )
    }
    
    fun getCurrentConversionRates(): List<ConversionRate> {
        val rates = getConversionRates()
        return rates.map { (crypto, rate) ->
            ConversionRate(
                fromCurrency = "USD",
                toCurrency = crypto,
                rate = rate,
                timestamp = System.currentTimeMillis()
            )
        }.toList()
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

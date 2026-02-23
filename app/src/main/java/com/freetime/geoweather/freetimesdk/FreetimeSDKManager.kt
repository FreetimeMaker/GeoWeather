package com.freetime.geoweather.freetimesdk

import android.content.Context
import android.util.Log
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

// Check if real SDK is available at runtime
private val useRealSDK = try {
    Class.forName("com.freetime.sdk.FreetimeSDK")
    Log.d("FreetimeSDK", "Real FreetimeSDK detected in classpath")
    true
} catch (e: ClassNotFoundException) {
    Log.w("FreetimeSDK", "Real FreetimeSDK not found, using mock implementation")
    false
}

class FreetimeSDKManager private constructor(private val context: Context) {
    
    // Use real SDK if available, otherwise use mock
    private val paymentManager by lazy {
        if (useRealSDK) {
            try {
                val sdkClass = Class.forName("com.freetime.sdk.FreetimeSDK")
                val getInstanceMethod = sdkClass.getMethod("getInstance")
                val sdkInstance = getInstanceMethod.invoke(null)
                val getManagerMethod = sdkClass.getMethod("getPaymentManager")
                getManagerMethod.invoke(sdkInstance)
            } catch (e: Exception) {
                Log.w("FreetimeSDK", "Failed to get real payment manager, using mock", e)
                com.freetime.geoweather.freetimesdk.mock.MockFreetimeSDK.getPaymentManager(context)
            }
        } else {
            com.freetime.geoweather.freetimesdk.mock.MockFreetimeSDK.getPaymentManager(context)
        }
    }
    
    private val walletManager by lazy {
        if (useRealSDK) {
            try {
                val sdkClass = Class.forName("com.freetime.sdk.FreetimeSDK")
                val getInstanceMethod = sdkClass.getMethod("getInstance")
                val sdkInstance = getInstanceMethod.invoke(null)
                val getManagerMethod = sdkClass.getMethod("getWalletManager")
                getManagerMethod.invoke(sdkInstance)
            } catch (e: Exception) {
                Log.w("FreetimeSDK", "Failed to get real wallet manager, using mock", e)
                com.freetime.geoweather.freetimesdk.mock.MockFreetimeSDK.getWalletManager(context)
            }
        } else {
            com.freetime.geoweather.freetimesdk.mock.MockFreetimeSDK.getWalletManager(context)
        }
    }
    
    private val analyticsManager by lazy {
        if (useRealSDK) {
            try {
                val sdkClass = Class.forName("com.freetime.sdk.FreetimeSDK")
                val getInstanceMethod = sdkClass.getMethod("getInstance")
                val sdkInstance = getInstanceMethod.invoke(null)
                val getManagerMethod = sdkClass.getMethod("getAnalyticsManager")
                getManagerMethod.invoke(sdkInstance)
            } catch (e: Exception) {
                Log.w("FreetimeSDK", "Failed to get real analytics manager, using mock", e)
                com.freetime.geoweather.freetimesdk.mock.MockFreetimeSDK.getAnalyticsManager(context)
            }
        } else {
            com.freetime.geoweather.freetimesdk.mock.MockFreetimeSDK.getAnalyticsManager(context)
        }
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
        if (useRealSDK) {
            try {
                val sdkClass = Class.forName("com.freetime.sdk.FreetimeSDK")
                val initializeMethod = sdkClass.getMethod("initialize", Context::class.java, String::class.java, String::class.java)
                initializeMethod.invoke(null, context, "geoweather-app", "v1.1.5")
                Log.d("FreetimeSDK", "Real FreetimeSDK v1.1.5 initialized for GeoWeather")
            } catch (e: Exception) {
                Log.w("FreetimeSDK", "Failed to initialize real SDK, using mock", e)
                com.freetime.geoweather.freetimesdk.mock.MockFreetimeSDK.initialize(context, "geoweather-app")
                Log.d("FreetimeSDK", "Mock FreetimeSDK v1.1.5 initialized for GeoWeather")
            }
        } else {
            com.freetime.geoweather.freetimesdk.mock.MockFreetimeSDK.initialize(context, "geoweather-app")
            Log.d("FreetimeSDK", "Mock FreetimeSDK v1.1.5 initialized for GeoWeather (real SDK not available)")
        }
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
                // Track donation initiated
                if (useRealSDK) {
                    try {
                        val trackMethod = analyticsManager.javaClass.getMethod("trackDonationInitiated", String::class.java, Double::class.java)
                        trackMethod.invoke(analyticsManager, paymentMethod.name, amount.amount)
                    } catch (e: Exception) {
                        Log.w("FreetimeSDK", "Analytics tracking failed, using mock", e)
                        (com.freetime.geoweather.freetimesdk.mock.MockFreetimeSDK.getAnalyticsManager(context) as com.freetime.geoweather.freetimesdk.mock.MockAnalyticsManager).trackDonationInitiated(paymentMethod, amount)
                    }
                } else {
                    (analyticsManager as com.freetime.geoweather.freetimesdk.mock.MockAnalyticsManager).trackDonationInitiated(paymentMethod, amount)
                }
                
                // Process payment
                val result = if (useRealSDK) {
                    try {
                        val processMethod = paymentManager.javaClass.getMethod(
                            "processPayment",
                            String::class.java,
                            Double::class.java,
                            String::class.java,
                            String::class.java,
                            String::class.java
                        )
                        processMethod.invoke(
                            paymentManager,
                            paymentMethod.name,
                            amount.amount,
                            amount.currency,
                            "geoweather-merchant",
                            "GeoWeather App Donation"
                        ) as PaymentResult
                    } catch (e: Exception) {
                        Log.w("FreetimeSDK", "Real payment processing failed, using mock", e)
                        val mockManager = com.freetime.geoweather.freetimesdk.mock.MockFreetimeSDK.getPaymentManager(context)
                        mockManager.processPayment(paymentMethod, amount, "geoweather-merchant", "GeoWeather App Donation")
                    }
                } else {
                    (paymentManager as com.freetime.geoweather.freetimesdk.mock.MockPaymentManager).processPayment(
                        paymentMethod, amount, "geoweather-merchant", "GeoWeather App Donation"
                    )
                }
                
                if (result.success) {
                    // Track completion
                    if (useRealSDK) {
                        try {
                            val trackMethod = analyticsManager.javaClass.getMethod("trackDonationCompleted", String::class.java, Double::class.java)
                            trackMethod.invoke(analyticsManager, paymentMethod.name, amount.amount)
                        } catch (e: Exception) {
                            Log.w("FreetimeSDK", "Analytics tracking failed", e)
                        }
                    } else {
                        (analyticsManager as com.freetime.geoweather.freetimesdk.mock.MockAnalyticsManager).trackDonationCompleted(paymentMethod, amount)
                    }
                    onSuccess()
                } else {
                    // Track failure
                    if (useRealSDK) {
                        try {
                            val trackMethod = analyticsManager.javaClass.getMethod("trackDonationFailed", String::class.java, Double::class.java, String::class.java)
                            trackMethod.invoke(analyticsManager, paymentMethod.name, amount.amount, result.errorMessage ?: "Unknown error")
                        } catch (e: Exception) {
                            Log.w("FreetimeSDK", "Analytics tracking failed", e)
                        }
                    } else {
                        (analyticsManager as com.freetime.geoweather.freetimesdk.mock.MockAnalyticsManager).trackDonationFailed(paymentMethod, amount, result.errorMessage ?: "Unknown error")
                    }
                    onError(result.errorMessage ?: "Unknown error")
                }
            } catch (e: Exception) {
                Log.e("FreetimeSDK", "Donation processing failed", e)
                onError("Payment processing failed: ${e.message}")
            }
        }
    }
    
    // Simplified USD-to-Crypto conversion using mock for now
    fun convertUSDtoCrypto(
        usdAmount: Double,
        targetCrypto: String,
        onSuccess: (ConversionResult) -> Unit,
        onError: (String) -> Unit
    ) {
        CoroutineScope(Dispatchers.Main).launch {
            try {
                // Validate input
                if (usdAmount <= 0) {
                    onError("Invalid amount. Please enter a positive value.")
                    return@launch
                }
                
                if (usdAmount > 10000) {
                    onError("Amount exceeds maximum limit of $10,000 USD")
                    return@launch
                }
                
                // Use mock implementation for now
                val mockManager = com.freetime.geoweather.freetimesdk.mock.MockFreetimeSDK.getPaymentManager(context)
                val mockResult = mockManager.processPayment(
                    com.freetime.geoweather.freetimesdk.models.PaymentMethod.USD_GATEWAY,
                    com.freetime.geoweather.freetimesdk.models.DonationAmount(usdAmount, "USD"),
                    "geoweather-merchant",
                    "USD to Crypto Conversion"
                )
                
                // Get conversion rates
                val rates = getConversionRates()
                val rate = rates[targetCrypto] ?: 1.0
                
                // Calculate fees (2.9% + $0.30 fixed fee)
                val percentageFee = usdAmount * 0.029
                val fixedFee = 0.30
                val totalFees = percentageFee + fixedFee
                
                if (totalFees >= usdAmount) {
                    onError("Amount too small to cover processing fees")
                    return@launch
                }
                
                val netAmount = usdAmount - totalFees
                val convertedAmount = netAmount / rate
                
                val result = ConversionResult(
                    success = true,
                    fromAmount = usdAmount,
                    fromCurrency = "USD",
                    toAmount = convertedAmount,
                    toCurrency = targetCrypto,
                    rate = rate,
                    fees = totalFees,
                    timestamp = System.currentTimeMillis()
                )
                
                // Track analytics
                (analyticsManager as com.freetime.geoweather.freetimesdk.mock.MockAnalyticsManager).trackUserAction(
                    "usd_to_crypto_conversion", 
                    mapOf(
                        "usd_amount" to usdAmount,
                        "target_crypto" to targetCrypto,
                        "rate" to rate,
                        "fees" to totalFees
                    )
                )
                
                onSuccess(result)
            } catch (e: Exception) {
                Log.e("FreetimeSDK", "USD to Crypto conversion failed", e)
                onError("Conversion failed: ${e.message}")
            }
        }
    }
    
    // Mock conversion rates API
    private fun getConversionRates(): Map<String, Double> {
        return mapOf(
            "BTC" to 43250.0,    // Bitcoin
            "ETH" to 2340.0,     // Ethereum  
            "USDT" to 1.0,       // Tether (stablecoin)
            "USDC" to 1.0,       // USD Coin (stablecoin)
            "SHIB" to 0.000025,  // Shiba Inu
            "DOGE" to 0.062,     // Dogecoin
            "TRON" to 0.124,     // TRON
            "LTC" to 67.5        // Litecoin
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
                val mockManager = com.freetime.geoweather.freetimesdk.mock.MockFreetimeSDK.getWalletManager(context)
                val result = mockManager.connectWallet(walletType)
                
                if (result.success && result.walletAddress != null) {
                    (analyticsManager as com.freetime.geoweather.freetimesdk.mock.MockAnalyticsManager).trackWalletConnected(walletType)
                    onSuccess(result.walletAddress)
                } else {
                    (analyticsManager as com.freetime.geoweather.freetimesdk.mock.MockAnalyticsManager).trackWalletConnectionFailed(walletType, result.errorMessage ?: "Unknown error")
                    onError(result.errorMessage ?: "Unknown error")
                }
            } catch (e: Exception) {
                Log.e("FreetimeSDK", "Wallet connection failed", e)
                onError("Wallet connection failed: ${e.message}")
            }
        }
    }
    
    fun getSupportedPaymentMethods(): List<PaymentMethod> {
        val mockManager = com.freetime.geoweather.freetimesdk.mock.MockFreetimeSDK.getPaymentManager(context)
        return mockManager.getSupportedMethods()
    }
    
    fun getSupportedWallets(): List<WalletType> {
        val mockManager = com.freetime.geoweather.freetimesdk.mock.MockFreetimeSDK.getWalletManager(context)
        return mockManager.getSupportedWallets()
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
        (analyticsManager as com.freetime.geoweather.freetimesdk.mock.MockAnalyticsManager).trackFeatureUsage(feature)
    }
    
    fun trackUserInteraction(action: String, details: Map<String, Any> = emptyMap()) {
        (analyticsManager as com.freetime.geoweather.freetimesdk.mock.MockAnalyticsManager).trackUserAction(action, details)
    }
}

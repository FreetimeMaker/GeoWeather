package com.freetime.geoweather.freetimesdk

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.freetime.geoweather.R
import com.freetime.geoweather.ui.theme.GeoWeatherTheme
import com.freetime.geoweather.freetimesdk.models.PaymentMethod
import com.freetime.geoweather.freetimesdk.models.DonationAmount

class PaymentActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            GeoWeatherTheme {
                PaymentScreen(onBack = { finish() })
            }
        }
    }
}

@Composable
fun PaymentScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val sdkManager = remember { FreetimeSDKManager.getInstance(context) }
    
    var selectedAmount by remember { mutableStateOf<DonationAmount?>(null) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    
    val supportedMethods = remember { sdkManager.getSupportedPaymentMethods() }
    val donationAmounts = remember { sdkManager.getDonationAmounts() }
    
    LaunchedEffect(Unit) {
        sdkManager.trackAppUsage("payment_screen")
    }
    
    Column(
        modifier = Modifier
            .padding(16.dp)
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = stringResource(R.string.select_donation_amount),
            style = MaterialTheme.typography.headlineSmall
        )
        
        // Amount selection
        donationAmounts.forEach { amount ->
            Button(
                onClick = { selectedAmount = amount },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (selectedAmount == amount) 
                        MaterialTheme.colorScheme.primary 
                    else 
                        MaterialTheme.colorScheme.secondary
                )
            ) {
                Text("$${amount.amount} ${amount.currency}")
            }
        }
        
        if (selectedAmount != null) {
            Text(
                text = stringResource(R.string.select_payment_method),
                style = MaterialTheme.typography.headlineSmall
            )
            
            supportedMethods.forEach { method ->
                Button(
                    onClick = {
                        isLoading = true
                        errorMessage = null
                        
                        sdkManager.processDonation(
                            paymentMethod = method,
                            amount = selectedAmount!!,
                            onSuccess = {
                                isLoading = false
                                sdkManager.trackUserInteraction("donation_completed", mapOf(
                                    "amount" to selectedAmount!!.amount,
                                    "method" to method.displayName
                                ))
                                // Show success or navigate to success screen
                            },
                            onError = { error ->
                                isLoading = false
                                errorMessage = error
                                sdkManager.trackUserInteraction("donation_failed", mapOf(
                                    "amount" to selectedAmount!!.amount,
                                    "method" to method.displayName,
                                    "error" to error
                                ))
                            }
                        )
                    },
                    enabled = !isLoading,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    } else {
                        Text(getPaymentMethodName(context, method))
                    }
                }
            }
        }
        
        errorMessage?.let { error ->
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer
                )
            ) {
                Text(
                    text = error,
                    modifier = Modifier.padding(16.dp),
                    color = MaterialTheme.colorScheme.onErrorContainer
                )
            }
        }
        
        Button(onClick = onBack) {
            Text(stringResource(R.string.backToSupPag))
        }
    }
}

private fun getPaymentMethodName(context: Context, method: PaymentMethod): String {
    return when (method.displayName.lowercase()) {
        "oxapay" -> context.getString(R.string.DonViaOxaPay)
        "coinbase" -> context.getString(R.string.DonViaCoin)
        "bitcoin" -> context.getString(R.string.DonViaBTC)
        "ethereum" -> context.getString(R.string.DonViaETH)
        "usdt" -> context.getString(R.string.DonViaUSDT)
        "usdc" -> context.getString(R.string.DonViaUSDC)
        "shib" -> context.getString(R.string.DonViaSHIB)
        "doge" -> context.getString(R.string.DonViaDOGE)
        "tron" -> context.getString(R.string.DonateViaTRON)
        "ltc" -> context.getString(R.string.DonViaLTC)
        else -> method.displayName
    }
}

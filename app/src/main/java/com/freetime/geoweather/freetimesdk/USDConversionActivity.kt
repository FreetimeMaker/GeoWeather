package com.freetime.geoweather.freetimesdk

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import com.freetime.geoweather.freetimesdk.models.ConversionRate
import com.freetime.geoweather.freetimesdk.models.ConversionResult

class USDConversionActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            GeoWeatherTheme {
                USDConversionScreen(onBack = { finish() })
            }
        }
    }
}

@Composable
fun USDConversionScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val sdkManager = remember { FreetimeSDKManager.getInstance(context) }
    
    var selectedAmount by remember { mutableStateOf(10.0) }
    var selectedCrypto by remember { mutableStateOf("BTC") }
    var conversionResult by remember { mutableStateOf<ConversionResult?>(null) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    
    val availableCryptos = listOf("BTC", "ETH", "USDT", "USDC", "SHIB", "DOGE", "TRON", "LTC")
    val conversionRates = remember { sdkManager.getCurrentConversionRates() }
    
    LaunchedEffect(Unit) {
        sdkManager.trackAppUsage("usd_conversion_screen")
    }
    
    LaunchedEffect(selectedAmount, selectedCrypto) {
        if (selectedAmount > 0 && selectedCrypto.isNotEmpty()) {
            isLoading = true
            errorMessage = null
            
            try {
                sdkManager.convertUSDtoCrypto(
                    usdAmount = selectedAmount,
                    targetCrypto = selectedCrypto,
                    onSuccess = { result ->
                        isLoading = false
                        conversionResult = result
                        sdkManager.trackUserInteraction("conversion_completed", mapOf(
                            "usd_amount" to selectedAmount,
                            "crypto" to selectedCrypto,
                            "converted_amount" to result.toAmount,
                            "rate" to result.rate
                        ))
                    },
                    onError = { error ->
                        isLoading = false
                        errorMessage = error
                        sdkManager.trackUserInteraction("conversion_failed", mapOf(
                            "usd_amount" to selectedAmount,
                            "crypto" to selectedCrypto,
                            "error" to error
                        ))
                    }
                )
            } catch (e: Exception) {
                isLoading = false
                errorMessage = "Conversion failed: ${e.message}"
                sdkManager.trackUserInteraction("conversion_crashed", mapOf(
                    "usd_amount" to selectedAmount,
                    "crypto" to selectedCrypto,
                    "error" to (e.message ?: "Unknown error")
                ))
            }
        }
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
            text = stringResource(R.string.USDTitle),
            style = MaterialTheme.typography.headlineSmall
        )
        
        Text(
            text = stringResource(R.string.USDDescription) + " (v1.0.7 API-integrated)",
            style = MaterialTheme.typography.bodyMedium
        )
        
        // Current conversion rates display
        Card(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "Current Exchange Rates (Live API):",
                    style = MaterialTheme.typography.titleMedium
                )
                
                if (conversionRates.isNotEmpty()) {
                    conversionRates.take(4).forEach { rate ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("1 USD = ${rate.rate} ${rate.toCurrency}")
                            Text(
                                text = "Updated: ${java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.getDefault()).format(java.util.Date(rate.timestamp))}",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                } else {
                    Text(
                        text = "Loading exchange rates...",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
        
        // Amount input with validation
        Text(
            text = "USD Amount:",
            style = MaterialTheme.typography.titleMedium
        )
        
        OutlinedTextField(
            value = selectedAmount.toString(),
            onValueChange = { 
                val newAmount = it.toDoubleOrNull()
                if (newAmount != null && newAmount >= 0) {
                    selectedAmount = newAmount
                }
            },
            label = { Text("Enter USD amount (min $1.00)") },
            modifier = Modifier.fillMaxWidth(),
            isError = selectedAmount > 0 && selectedAmount < 1.0
        )
        
        if (selectedAmount > 0 && selectedAmount < 1.0) {
            Text(
                text = "Minimum amount is $1.00 USD",
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall
            )
        }
        
        if (selectedAmount > 10000) {
            Text(
                text = "Maximum amount is $10,000 USD",
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall
            )
        }
        
        // Quick amount buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            listOf(5.0, 10.0, 25.0, 50.0, 100.0).forEach { amount ->
                Button(
                    onClick = { selectedAmount = amount },
                    modifier = Modifier.weight(1.0f)
                ) {
                    Text("$${amount.toInt()}")
                }
            }
        }
        
        // Crypto selection
        Text(
            text = "Convert to:",
            style = MaterialTheme.typography.titleMedium
        )
        
        LazyColumn(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            items(availableCryptos) { crypto ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = selectedCrypto == crypto,
                        onClick = { selectedCrypto = crypto }
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(crypto)
                }
            }
        }
        
        // Loading indicator
        if (isLoading) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp)
                )
                Text("Converting...")
            }
        }
        
        // Conversion result with enhanced fee breakdown
        conversionResult?.let { result ->
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Conversion Summary:",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    
                    Text(
                        text = "You Send: $${String.format("%.2f", result.fromAmount)} USD",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    
                    Text(
                        text = "Processing Fees: $${String.format("%.2f", result.fees)} USD",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    
                    Text(
                        text = "Net Amount: $${String.format("%.2f", result.fromAmount - result.fees)} USD",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    
                    Text(
                        text = "The Developer Receives: ${String.format("%.6f", result.toAmount)} ${result.toCurrency}",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    
                    Text(
                        text = "Exchange Rate: 1 USD = ${String.format("%.6f", result.rate)} ${result.toCurrency}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    
                    Button(
                        onClick = {
                            // In real implementation, this would proceed with the actual conversion
                            sdkManager.trackUserInteraction("conversion_proceed", mapOf(
                                "result" to result
                            ))
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Proceed to Payment")
                    }
                }
            }
        }
        
        // Error message
        errorMessage?.let { error ->
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = error,
                    modifier = Modifier.padding(16.dp),
                    color = MaterialTheme.colorScheme.onErrorContainer
                )
            }
        }
        
        Button(
            onClick = onBack,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(stringResource(R.string.backToSupPag))
        }
    }
}

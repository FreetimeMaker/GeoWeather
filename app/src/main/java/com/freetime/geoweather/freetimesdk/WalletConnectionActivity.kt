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
import com.freetime.geoweather.freetimesdk.models.WalletType

class WalletConnectionActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            GeoWeatherTheme {
                WalletConnectionScreen(onBack = { finish() })
            }
        }
    }
}

@Composable
fun WalletConnectionScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val sdkManager = remember { FreetimeSDKManager.getInstance(context) }
    
    var isLoading by remember { mutableStateOf(false) }
    var connectedWallet by remember { mutableStateOf<String?>(null) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    
    val supportedWallets = remember { sdkManager.getSupportedWallets() }
    
    LaunchedEffect(Unit) {
        sdkManager.trackAppUsage("wallet_connection_screen")
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
            text = stringResource(R.string.WalletTitle),
            style = MaterialTheme.typography.headlineSmall
        )
        
        Text(
            text = stringResource(R.string.WalletDescription),
            style = MaterialTheme.typography.bodyMedium
        )
        
        supportedWallets.forEach { walletType ->
            Button(
                onClick = {
                    isLoading = true
                    errorMessage = null
                    
                    sdkManager.connectWallet(
                        walletType = walletType,
                        onSuccess = { walletAddress ->
                            isLoading = false
                            connectedWallet = walletAddress
                            sdkManager.trackUserInteraction("wallet_connected", mapOf(
                                "wallet_type" to walletType.displayName,
                                "wallet_address" to walletAddress
                            ))
                        },
                        onError = { error ->
                            isLoading = false
                            errorMessage = error
                            sdkManager.trackUserInteraction("wallet_connection_failed", mapOf(
                                "wallet_type" to walletType.displayName,
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
                    Text(getWalletTypeName(context, walletType))
                }
            }
        }
        
        connectedWallet?.let { address ->
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = stringResource(R.string.wallet_connected),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Text(
                        text = "Address: ${address.take(8)}...${address.takeLast(8)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
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

private fun getWalletTypeName(context: Context, walletType: WalletType): String {
    return when (walletType.displayName.lowercase()) {
        "metamask" -> "MetaMask"
        "trustwallet" -> "Trust Wallet"
        "coinbase" -> "Coinbase Wallet"
        "binance" -> "Binance Wallet"
        "exodus" -> "Exodus"
        "atomic" -> "Atomic Wallet"
        else -> walletType.displayName
    }
}

package com.freetime.geoweather

import android.content.ClipboardManager
import android.content.Context
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.freetime.geoweather.ui.theme.GeoWeatherTheme

class WalletAddressActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        hideSystemBars()

        setContent {
            val sharedPreferences = remember { getSharedPreferences("geo_weather_prefs", Context.MODE_PRIVATE) }
            val useSystemTheme = sharedPreferences.collectAsState(key = "use_system_theme", defaultValue = true)
            val darkModeEnabled = sharedPreferences.collectAsState(key = "dark_mode_enabled", defaultValue = false)
            val dynamicColor = sharedPreferences.collectAsState(key = "dynamic_color", defaultValue = true)
            val oledBlack = sharedPreferences.collectAsState(key = "oled_black", defaultValue = false)

            val darkTheme = if (useSystemTheme.value) isSystemInDarkTheme() else darkModeEnabled.value

            GeoWeatherTheme(darkTheme = darkTheme, dynamicColor = dynamicColor.value, oledBlack = oledBlack.value) {
                WalletAddressScreen(onBack = { finish() })
            }
        }
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) {
            hideSystemBars()
        }
    }

    private fun hideSystemBars() {
        val windowInsetsController =
            WindowCompat.getInsetsController(window, window.decorView)
        windowInsetsController.systemBarsBehavior =
            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        windowInsetsController.hide(WindowInsetsCompat.Type.systemBars())
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WalletAddressScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    
    val walletAddresses = listOf(
        "BTC (BTC only)" to "1DsCAVrzvGokrzXpe6YR33QuTo5EppiKRE",
        "ETH (ETH only)" to "0x3d3eee5b542975839d2dccbf2f97139debc711bc",
        "USDT (Tron only)" to "TKUNwoQMyLuJzUzWPKwA7yw4qujz2Pz6gS",
        "USDT (ETH only)" to "0x3d3eee5b542975839d2dccbf2f97139debc711bc",
        "USDT (Polygon only)" to "0x3d3eee5b542975839d2dccbf2f97139debc711bc",
        "USDT (BSC only)" to "0x3d3eee5b542975839d2dccbf2f97139debc711bc",
        "USDT (SOL only)" to "6K6gpBF9nyrSL2vzSaFDZgAJQurkoEzPGtK67WAg6FjX",
        "USDT (TON only)" to "UQANB5nn0Oinom7IFkbClwRWRpK2zfal6sO11988Y85AamDS",
        "USDT (Optimism only)" to "0x3d3eee5b542975839d2dccbf2f97139debc711bc",
        "USDC (ETH only)" to "0x3d3eee5b542975839d2dccbf2f97139debc711bc",
        "USDC (Polygon only)" to "0x3d3eee5b542975839d2dccbf2f97139debc711bc",
        "USDC (BSC only)" to "0x3d3eee5b542975839d2dccbf2f97139debc711bc",
        "USDC (SOL only)" to "6K6gpBF9nyrSL2vzSaFDZgAJQurkoEzPGtK67WAg6FjX",
        "USDC (Arbitrum only)" to "0x3d3eee5b542975839d2dccbf2f97139debc711bc",
        "USDC (Optimism only)" to "0x3d3eee5b542975839d2dccbf2f97139debc711bc",
        "SHIB (ETH only)" to "0x3d3eee5b542975839d2dccbf2f97139debc711bc",
        "SHIB (BSC only)" to "0x3d3eee5b542975839d2dccbf2f97139debc711bc",
        "CTC (Polygon only)" to "0x3d3eee5b542975839d2dccbf2f97139debc711bc",
        "BNB (BSC only)" to "0x3d3eee5b542975839d2dccbf2f97139debc711bc",
        "SOL (SOL only)" to "6K6gpBF9nyrSL2vzSaFDZgAJQurkoEzPGtK67WAg6FjX",
        "ROX (SOL only)" to "6K6gpBF9nyrSL2vzSaFDZgAJQurkoEzPGtK67WAg6FjX",
        "ROX (Polygon only)" to "0x3d3eee5b542975839d2dccbf2f97139debc711bc",
        "ROX (BSC only)" to "0x3d3eee5b542975839d2dccbf2f97139debc711bc",
        "PEPE (ETH only)" to "0x3d3eee5b542975839d2dccbf2f97139debc711bc",
        "DAI (BEP only)" to "0x3d3eee5b542975839d2dccbf2f97139debc711bc",
        "DAI (Polygon only)" to "0x3d3eee5b542975839d2dccbf2f97139debc711bc",
        "DAI (ETH only)" to "0x3d3eee5b542975839d2dccbf2f97139debc711bc",
        "TRON (Tron only)" to "TKUNwoQMyLuJzUzWPKwA7yw4qujz2Pz6gS",
        "TRON (BEP only)" to "0x3d3eee5b542975839d2dccbf2f97139debc711bc",
        "LTC (LTC only)" to "LU2ERRXKTeKnzpuieQcpsBteViEY7ff5Wg",
        "Bitcoin Cash (BCH only)" to "qz5klapp9c4kq97psu5rg7sq9quu3vcv7qan8dn6ts",
        "DOGE (DOGE only)" to "DFZtQ1SedQFGijrR7LJ55RFBNFVQpbGULn",
        "DOGE (BEP only)" to "0x3d3eee5b542975839d2dccbf2f97139debc711bc",
        "TON (TON only)" to "UQANB5nn0Oinom7IFkbClwRWRpK2zfal6sO11988Y85AamDS",
        "DOGE (BEP only)" to "0x3d3eee5b542975839d2dccbf2f97139debc711bc",
        "POL (Polygon only)" to "0x3d3eee5b542975839d2dccbf2f97139debc711bc",
        "POL (ETH only)" to "0x3d3eee5b542975839d2dccbf2f97139debc711bc",
        "HSH (BEP only)" to "0x3d3eee5b542975839d2dccbf2f97139debc711bc",
        "ARB (Arbitrum only)" to "0x3d3eee5b542975839d2dccbf2f97139debc711bc",
        "Optimism (Optimism only)" to "0x3d3eee5b542975839d2dccbf2f97139debc711bc",
        "USDS (ETH only)" to "0x3d3eee5b542975839d2dccbf2f97139debc711bc",
        "Every SOL based Token or NFT" to "8xB5kxQMHr44czWYdNZTrwvho17SW23KEnAMxe7V85RR",
        "Every ETH based Token or NFT" to "0xba8bBaE3168062699E668Be7d99AB10B790aB467",
        "Monad (ETH only)" to "0xba8bBaE3168062699E668Be7d99AB10B790aB467",
        "Every BASE based Token or NFT" to "0xba8bBaE3168062699E668Be7d99AB10B790aB467",
        "SUI" to "0x366f1e1d6d404351cbf9836494206aab43264fd60228b15c06e275bd7b161b78",
        "Every POL based Token or NFT" to "0xba8bBaE3168062699E668Be7d99AB10B790aB467",
        "HYPE (ETH only)" to "0xba8bBaE3168062699E668Be7d99AB10B790aB467",
        "XMR" to "49szz88CqMWGgyDxp7VqvBS62pGLQcV4YPSBHcLwtxAXLz1Wngf8vW6is4w13Au7C2RovrTiJQaGDV5VBhFnyMBsM44Pn2P",
        "DASH" to "Xhr4Nirm7AZVtSF8ovsy5nEeXhS8Tv24pV",
        "ZEC" to "u14l4cu9m4z8r92ut4j6fqz99wuttrq2u7gtlvgm84j3g7p32a74257c5882nd6emzdwkx97had5tfhaz0k7mr9urpp4nf9fq7wcj2txggl5ttxu8xnz8khxpnhuj24r29av00egp59jzxsule409apmul3uskny566hfkhz3lgfkxwavpjf37sf64jpdnht6sf759e09043je7z7kdje",
        "COSA" to "0xA2C0CF8a702475b12865E1C28C7319f9A6806B25",
        "Pirate Cash" to "0xA2C0CF8a702475b12865E1C28C7319f9A6806B25"
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.wallet_addresses_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back_nav_desc))
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            walletAddresses.forEach { (name, address) ->
                WalletAddressCard(name, address)
            }
        }
    }
}

@Composable
fun WalletAddressCard(name: String, address: String) {
    val context = LocalContext.current
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = name,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = address,
                style = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(12.dp))
            Button(
                onClick = {
                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    val clip = android.content.ClipData.newPlainText("wallet_address", address)
                    clipboard.setPrimaryClip(clip)
                    Toast.makeText(context, context.getString(R.string.address_copied), Toast.LENGTH_SHORT).show()
                },
                modifier = Modifier.align(Alignment.End)
            ) {
                Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.copy_address))
            }
        }
    }
}

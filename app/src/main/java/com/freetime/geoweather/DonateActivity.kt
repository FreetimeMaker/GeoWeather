package com.freetime.geoweather

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.freetime.geoweather.ui.theme.GeoWeatherTheme

class DonateActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val sharedPreferences = remember { getSharedPreferences("geo_weather_prefs", Context.MODE_PRIVATE) }
            val useSystemTheme = sharedPreferences.collectAsState(key = "use_system_theme", defaultValue = true)
            val darkModeEnabled = sharedPreferences.collectAsState(key = "dark_mode_enabled", defaultValue = false)
            val dynamicColor = sharedPreferences.collectAsState(key = "dynamic_color", defaultValue = true)

            val darkTheme = if (useSystemTheme.value) isSystemInDarkTheme() else darkModeEnabled.value
            
            GeoWeatherTheme(darkTheme = darkTheme, dynamicColor = dynamicColor.value) {
                DonateScreen(onBack = { finish() })
            }
        }
    }
}

class OxaPayActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            GeoWeatherTheme {
                WebViewScreen(url = "https://pay.oxapay.com/13038067", onBack = { finish() })
            }
        }
    }
}

class BitcoinActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            GeoWeatherTheme {
                WebViewScreen(url = "https://ncwallet.net/pay/60misly", onBack = { finish() })
            }
        }
    }
}

class EthereumActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            GeoWeatherTheme {
                WebViewScreen(url = "https://ncwallet.net/pay/86fremd", onBack = { finish() })
            }
        }
    }
}

class USDT_Activity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            GeoWeatherTheme {
                WebViewScreen(url = "https://ncwallet.net/pay/19tacit", onBack = { finish() })
            }
        }
    }
}

class USDC_Activity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            GeoWeatherTheme {
                WebViewScreen(url = "https://ncwallet.net/pay/15snog", onBack = { finish() })
            }
        }
    }
}

class LTC_Activity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            GeoWeatherTheme {
                WebViewScreen(url = "https://ncwallet.net/pay/77pudgy", onBack = { finish() })
            }
        }
    }
}

class DogeActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            GeoWeatherTheme {
                WebViewScreen(url = "https://ncwallet.net/pay/30allie", onBack = { finish() })
            }
        }
    }
}

class TronActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            GeoWeatherTheme {
                WebViewScreen(url = "https://ncwallet.net/pay/15gown", onBack = { finish() })
            }
        }
    }
}

class BNB_Activity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            GeoWeatherTheme {
                WebViewScreen(url = "https://ncwallet.net/pay/02hanch", onBack = { finish() })
            }
        }
    }
}

class PEPE_Activity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            GeoWeatherTheme {
                WebViewScreen(url = "https://ncwallet.net/pay/73enow", onBack = { finish() })
            }
        }
    }
}

class SOL_Activity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            GeoWeatherTheme {
                WebViewScreen(url = "https://ncwallet.net/pay/54fled", onBack = { finish() })
            }
        }
    }
}

class ShibActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            GeoWeatherTheme {
                WebViewScreen(url = "https://ncwallet.net/pay/18spile", onBack = { finish() })
            }
        }
    }
}

class GH_SponsorsActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            GeoWeatherTheme {
                WebViewScreen(url = "https:github.com/sponsors/FreetimeMaker", onBack = { finish() })
            }
        }
    }
}

class DonatorActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            GeoWeatherTheme {
                DonatorScreen(onBack = { finish() })
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WebViewScreen(
    url: String,
    onBack: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Spenden") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { innerPadding ->
        AndroidView(
            factory = { ctx ->
                WebView(ctx).apply {
                    webViewClient = WebViewClient()
                    settings.javaScriptEnabled = true
                    loadUrl(url)
                }
            },
            modifier = Modifier.fillMaxSize().padding(innerPadding)
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DonatorScreen(
    onBack: () -> Unit,
    context: Context = LocalContext.current
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Unterstützer") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            Text(
                textAlign = TextAlign.Center,
                text = stringResource(R.string.DonTXT1),
                style = MaterialTheme.typography.bodyLarge
            )

            Text(
                textAlign = TextAlign.Center,
                text = stringResource(R.string.DonTXT2),
                style = MaterialTheme.typography.bodyLarge
            )

            Text(
                textAlign = TextAlign.Center,
                text = stringResource(R.string.DonPers),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Button(
                onClick = { openDiscordInvite(context) },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(stringResource(R.string.JoiOffDisSer))
            }
        }
    }
}

fun openDiscordInvite(context: Context) {
    val webIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://discord.gg/zPFvwK9pNh"))
    context.startActivity(webIntent)
}

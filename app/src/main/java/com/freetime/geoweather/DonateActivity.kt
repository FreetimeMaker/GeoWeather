package com.freetime.geoweather

import android.os.Bundle
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.view.WindowCompat
import com.freetime.geoweather.ui.theme.GeoWeatherTheme

class DonateActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        hideSystemUI()
        setContent {
            GeoWeatherTheme {
                DonateScreen(onBack = { finish() })
            }
        }
    }

    private fun hideSystemUI() {
        WindowCompat.getInsetsController(window, window.decorView).apply {
            hide(androidx.core.view.WindowInsetsCompat.Type.systemBars())
            systemBarsBehavior = androidx.core.view.WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }
    }
}

class OxaPayActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        hideSystemUI()
        setContent {
            GeoWeatherTheme {
                WebViewScreen(
                    url = "https://pay.oxapay.com/13038067",
                    onBack = { finish() }
                )
            }
        }
    }

    private fun hideSystemUI() {
        WindowCompat.getInsetsController(window, window.decorView).apply {
            hide(androidx.core.view.WindowInsetsCompat.Type.systemBars())
            systemBarsBehavior = androidx.core.view.WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }
    }
}

class BitcoinActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        hideSystemUI()
        setContent {
            GeoWeatherTheme {
                WebViewScreen(
                    url = "https://ncwallet.net/pay/60misly",
                    onBack = { finish() }
                )
            }
        }
    }

    private fun hideSystemUI() {
        WindowCompat.getInsetsController(window, window.decorView).apply {
            hide(androidx.core.view.WindowInsetsCompat.Type.systemBars())
            systemBarsBehavior = androidx.core.view.WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }
    }
}

class EthereumActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        hideSystemUI()
        setContent {
            GeoWeatherTheme {
                WebViewScreen(
                    url = "https://ncwallet.net/pay/86fremd",
                    onBack = { finish() }
                )
            }
        }
    }

    private fun hideSystemUI() {
        WindowCompat.getInsetsController(window, window.decorView).apply {
            hide(androidx.core.view.WindowInsetsCompat.Type.systemBars())
            systemBarsBehavior = androidx.core.view.WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }
    }
}

class CoinbaseActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        hideSystemUI()
        setContent {
            GeoWeatherTheme {
                WebViewScreen(
                    url = "https://commerce.coinbase.com/checkout/cdc99b02-9521-40df-94e0-14fe86d422b1",
                    onBack = { finish() }
                )
            }
        }
    }

    private fun hideSystemUI() {
        WindowCompat.getInsetsController(window, window.decorView).apply {
            hide(androidx.core.view.WindowInsetsCompat.Type.systemBars())
            systemBarsBehavior = androidx.core.view.WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }
    }
}

class USDT_Activity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        hideSystemUI()
        setContent {
            GeoWeatherTheme {
                WebViewScreen(
                    url = "https://ncwallet.net/pay/19tacit",
                    onBack = { finish() }
                )
            }
        }
    }

    private fun hideSystemUI() {
        WindowCompat.getInsetsController(window, window.decorView).apply {
            hide(androidx.core.view.WindowInsetsCompat.Type.systemBars())
            systemBarsBehavior = androidx.core.view.WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }
    }
}

class USDC_Activity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        hideSystemUI()
        setContent {
            GeoWeatherTheme {
                WebViewScreen(
                    url = "https.ncwallet.net/pay/15snog",
                    onBack = { finish() }
                )
            }
        }
    }

    private fun hideSystemUI() {
        WindowCompat.getInsetsController(window, window.decorView).apply {
            hide(androidx.core.view.WindowInsetsCompat.Type.systemBars())
            systemBarsBehavior = androidx.core.view.WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }
    }
}

class LTC_Activity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        hideSystemUI()
        setContent {
            GeoWeatherTheme {
                WebViewScreen(
                    url = "https://ncwallet.net/pay/77pudgy",
                    onBack = { finish() }
                )
            }
        }
    }

    private fun hideSystemUI() {
        WindowCompat.getInsetsController(window, window.decorView).apply {
            hide(androidx.core.view.WindowInsetsCompat.Type.systemBars())
            systemBarsBehavior = androidx.core.view.WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }
    }
}

class DogeActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        hideSystemUI()
        setContent {
            GeoWeatherTheme {
                WebViewScreen(
                    url = "https://ncwallet.net/pay/30allie",
                    onBack = { finish() }
                )
            }
        }
    }

    private fun hideSystemUI() {
        WindowCompat.getInsetsController(window, window.decorView).apply {
            hide(androidx.core.view.WindowInsetsCompat.Type.systemBars())
            systemBarsBehavior = androidx.core.view.WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }
    }
}

class ShibActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        hideSystemUI()
        setContent {
            GeoWeatherTheme {
                WebViewScreen(
                    url = "https://ncwallet.net/pay/18spile",
                    onBack = { finish() }
                )
            }
        }
    }

    private fun hideSystemUI() {
        WindowCompat.getInsetsController(window, window.decorView).apply {
            hide(androidx.core.view.WindowInsetsCompat.Type.systemBars())
            systemBarsBehavior = androidx.core.view.WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }
    }
}

class TronActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        hideSystemUI()
        setContent {
            GeoWeatherTheme {
                WebViewScreen(
                    url = "https://ncwallet.net/pay/15gown",
                    onBack = { finish() }
                )
            }
        }
    }

    private fun hideSystemUI() {
        WindowCompat.getInsetsController(window, window.decorView).apply {
            hide(androidx.core.view.WindowInsetsCompat.Type.systemBars())
            systemBarsBehavior = androidx.core.view.WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }
    }
}

class DonatorActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        hideSystemUI()
        setContent {
            GeoWeatherTheme {
                DonatorScreen(
                    onBack = { finish() }
                )
            }
        }
    }

    private fun hideSystemUI() {
        WindowCompat.getInsetsController(window, window.decorView).apply {
            hide(androidx.core.view.WindowInsetsCompat.Type.systemBars())
            systemBarsBehavior = androidx.core.view.WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }
    }
}

@Composable
fun WebViewScreen(
    url: String,
    onBack: () -> Unit
) {
    Box(modifier = Modifier.fillMaxSize()) {
        AndroidView(
            factory = { ctx ->
                WebView(ctx).apply {
                    webViewClient = WebViewClient()
                    settings.javaScriptEnabled = true
                    loadUrl(url)
                }
            },
            modifier = Modifier.fillMaxSize()
        )

        Button(
            onClick = onBack,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(16.dp)
                .fillMaxWidth()
        ) {
            Text("Go Back to the Support Page")
        }
    }
}

@Composable
fun DonatorScreen(
    onBack: () -> Unit
) {
    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 48.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            Text(
                textAlign = TextAlign.Center,
                text = "Here are Supporters that help maintain this and many other Apps"
            )

            Text(
                textAlign = TextAlign.Center,
                text = "If you want to help maintain this and many other Apps simply make a Donation and E-Mail me Your Name and the Donation Provider you used and I will then list you Here."
            )

            Text(
                textAlign = TextAlign.Center,
                text = "No one made a Donation yet."
            )
        }

        Button(
            onClick = onBack,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(16.dp)
                .fillMaxWidth()
        ) {
            Text("Go Back to the Support Page")
        }
    }
}

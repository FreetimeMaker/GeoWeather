package com.freetime.geoweather

import android.content.Intent
import android.os.Bundle
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.freetime.geoweather.ui.theme.GeoWeatherTheme
import com.freetime.geoweather.ui.hideSystemUI

class DonateActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        hideSystemUI(window)
        setContent {
            GeoWeatherTheme {
                DonateScreenContent(
                    onBack = { finish() }
                )
            }
        }
    }
}

@Composable
fun DonateScreenContent(onBack: () -> Unit) {
    val context = LocalContext.current
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Button(onClick = { context.startActivity(Intent(context, OxaPayActivity::class.java)) }) {
            Text("OxaPay")
        }
        Spacer(modifier = Modifier.height(8.dp))
        Button(onClick = { context.startActivity(Intent(context, CoinbaseActivity::class.java)) }) {
            Text("Coinbase")
        }
        Spacer(modifier = Modifier.height(8.dp))
        Button(onClick = { context.startActivity(Intent(context, BitcoinActivity::class.java)) }) {
            Text("Bitcoin")
        }
        Spacer(modifier = Modifier.height(8.dp))
        Button(onClick = { context.startActivity(Intent(context, EthereumActivity::class.java)) }) {
            Text("Ethereum")
        }
        Spacer(modifier = Modifier.height(8.dp))
        Button(onClick = { context.startActivity(Intent(context, USDT_Activity::class.java)) }) {
            Text("USDT")
        }
        Spacer(modifier = Modifier.height(8.dp))
        Button(onClick = { context.startActivity(Intent(context, USDC_Activity::class.java)) }) {
            Text("USDC")
        }
        Spacer(modifier = Modifier.height(8.dp))
        Button(onClick = { context.startActivity(Intent(context, LTC_Activity::class.java)) }) {
            Text("LTC")
        }
        Spacer(modifier = Modifier.height(8.dp))
        Button(onClick = { context.startActivity(Intent(context, DogeActivity::class.java)) }) {
            Text("Doge")
        }
        Spacer(modifier = Modifier.height(8.dp))
        Button(onClick = { context.startActivity(Intent(context, ShibActivity::class.java)) }) {
            Text("Shib")
        }
        Spacer(modifier = Modifier.height(8.dp))
        Button(onClick = { context.startActivity(Intent(context, TronActivity::class.java)) }) {
            Text("Tron")
        }
        Spacer(modifier = Modifier.height(8.dp))
        Button(onClick = { context.startActivity(Intent(context, DonatorActivity::class.java)) }) {
            Text("Donators")
        }
        Spacer(modifier = Modifier.height(16.dp))
        Button(
            onClick = onBack,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Back")
        }
    }
}

class OxaPayActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        hideSystemUI(window)
        setContent {
            GeoWeatherTheme {
                WebViewScreen(
                    url = "https://pay.oxapay.com/13038067",
                    onBack = { finish() }
                )
            }
        }
    }
}

class BitcoinActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        hideSystemUI(window)
        setContent {
            GeoWeatherTheme {
                WebViewScreen(
                    url = "https://ncwallet.net/pay/60misly",
                    onBack = { finish() }
                )
            }
        }
    }
}

class EthereumActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        hideSystemUI(window)
        setContent {
            GeoWeatherTheme {
                WebViewScreen(
                    url = "https://ncwallet.net/pay/86fremd",
                    onBack = { finish() }
                )
            }
        }
    }
}

class CoinbaseActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        hideSystemUI(window)
        setContent {
            GeoWeatherTheme {
                WebViewScreen(
                    url = "https://commerce.coinbase.com/checkout/cdc99b02-9521-40df-94e0-14fe86d422b1",
                    onBack = { finish() }
                )
            }
        }
    }
}

class USDT_Activity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        hideSystemUI(window)
        setContent {
            GeoWeatherTheme {
                WebViewScreen(
                    url = "https://ncwallet.net/pay/19tacit",
                    onBack = { finish() }
                )
            }
        }
    }
}

class USDC_Activity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        hideSystemUI(window)
        setContent {
            GeoWeatherTheme {
                WebViewScreen(
                    url = "https.ncwallet.net/pay/15snog",
                    onBack = { finish() }
                )
            }
        }
    }
}

class LTC_Activity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        hideSystemUI(window)
        setContent {
            GeoWeatherTheme {
                WebViewScreen(
                    url = "https://ncwallet.net/pay/77pudgy",
                    onBack = { finish() }
                )
            }
        }
    }
}

class DogeActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        hideSystemUI(window)
        setContent {
            GeoWeatherTheme {
                WebViewScreen(
                    url = "https://ncwallet.net/pay/30allie",
                    onBack = { finish() }
                )
            }
        }
    }
}

class ShibActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        hideSystemUI(window)
        setContent {
            GeoWeatherTheme {
                WebViewScreen(
                    url = "https://ncwallet.net/pay/18spile",
                    onBack = { finish() }
                )
            }
        }
    }
}

class TronActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        hideSystemUI(window)
        setContent {
            GeoWeatherTheme {
                WebViewScreen(
                    url = "https://ncwallet.net/pay/15gown",
                    onBack = { finish() }
                )
            }
        }
    }
}

class DonatorActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        hideSystemUI(window)
        setContent {
            GeoWeatherTheme {
                DonatorScreen(
                    onBack = { finish() }
                )
            }
        }
    }
}

@Composable
fun WebViewScreen(
    url: String,
    onBack: () -> Unit
) {
    val context = LocalContext.current

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
    val context = LocalContext.current

    Box(modifier = Modifier.fillMaxSize()) {
        Text(
            textAlign = TextAlign.Center,
            text = "Here are Supporters that help maintain this and many other Apps",
        )

        Text(
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(16.dp),
            text = "If you want to help maintain this and many other Apps simply make a Donation and E-Mail me Your Name and the Donation Provider you used and I will then list you Here."
        )

        Text(
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(16.dp),
            text = "No one made a Donation yet."
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

package com.freetime.geoweather

import android.content.Intent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

@Composable
fun DonateScreen(onBack: () -> Unit) {
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .padding(16.dp)
            .fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Button(onClick = {
            context.startActivity(Intent(context, OxaPayActivity::class.java))
        }) { Text("Donate via OxaPay") }

        Button(onClick = {
            context.startActivity(Intent(context, CoinbaseActivity::class.java))
        }) { Text("Donate via Coinbase") }

        Button(onClick = {
            context.startActivity(Intent(context, BitcoinActivity::class.java))
        }) { Text("Donate via Bitcoin") }

        Button(onClick = {
            context.startActivity(Intent(context, EthereumActivity::class.java))
        }) { Text("Donate via Ethereum") }

        Button(onClick = {
            context.startActivity(Intent(context, USDT_Activity::class.java))
        }) { Text("Donate via USDT") }

        Button(onClick = {
            context.startActivity(Intent(context, USDC_Activity::class.java))
        }) { Text("Donate via USDC") }

        Button(onClick = {
            context.startActivity(Intent(context, ShibActivity::class.java))
        }) { Text("Donate via SHIB") }

        Button(onClick = {
            context.startActivity(Intent(context, DogeActivity::class.java))
        }) { Text("Donate via DOGE") }

        Button(onClick = {
            context.startActivity(Intent(context, TronActivity::class.java))
        }) { Text("Donate via TRON") }

        Button(onClick = {
            context.startActivity(Intent(context, LTC_Activity::class.java))
        }) { Text("Donate via LTC") }

        Button(onClick = {
            context.startActivity(Intent(context, DonatorActivity::class.java))
        }) { Text("View Supporters") }

        Button(onClick = onBack) { Text("Back") }
    }
}

package com.freetime.geoweather

import android.content.Intent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.freetime.geoweather.R

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
        }) { Text(stringResource(R.string.DonViaOxaPay)) }

        Button(onClick = {
            context.startActivity(Intent(context, CoinbaseActivity::class.java))
        }) { Text(stringResource(R.string.DonViaCoin)) }

        Button(onClick = {
            context.startActivity(Intent(context, BitcoinActivity::class.java))
        }) { Text(stringResource(R.string.DonViaBTC)) }

        Button(onClick = {
            context.startActivity(Intent(context, EthereumActivity::class.java))
        }) { Text(stringResource(R.string.DonViaETH)) }

        Button(onClick = {
            context.startActivity(Intent(context, USDT_Activity::class.java))
        }) { Text(stringResource(R.string.DonViaUSDT)) }

        Button(onClick = {
            context.startActivity(Intent(context, USDC_Activity::class.java))
        }) { Text(stringResource(R.string.DonViaUSDC)) }

        Button(onClick = {
            context.startActivity(Intent(context, ShibActivity::class.java))
        }) { Text(stringResource(R.string.DonViaSHIB)) }

        Button(onClick = {
            context.startActivity(Intent(context, DogeActivity::class.java))
        }) { Text(stringResource(R.string.DonViaDOGE)) }

        Button(onClick = {
            context.startActivity(Intent(context, TronActivity::class.java))
        }) { Text(stringResource(R.string.DonateViaTRON)) }

        Button(onClick = {
            context.startActivity(Intent(context, LTC_Activity::class.java))
        }) { Text(stringResource(R.string.DonViaLTC)) }

        Button(onClick = {
            context.startActivity(Intent(context, DonatorActivity::class.java))
        }) { Text(stringResource(R.string.ViewSup)) }

        Button(onClick = onBack) { Text(stringResource(R.string.BackToWeaDash)) }
    }
}

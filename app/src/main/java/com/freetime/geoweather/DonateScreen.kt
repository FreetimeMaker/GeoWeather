package com.freetime.geoweather

import android.content.Intent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DonateScreen(onBack: () -> Unit) {
    val context = LocalContext.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.donate_title)) },
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
            verticalArrangement = Arrangement.spacedBy(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = stringResource(R.string.support_development),
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = stringResource(R.string.select_option_msg),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }

            Text(
                text = stringResource(R.string.cash_label),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.align(Alignment.Start).padding(top = 8.dp)
            )

            DonateButton(text = stringResource(R.string.DonViaGHSponsors)) {
                context.startActivity(Intent(context, GH_SponsorsActivity::class.java))
            }

            Text(
                text = stringResource(R.string.crypto_label),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.align(Alignment.Start).padding(top = 8.dp)
            )

            DonateButton(text = stringResource(R.string.DonViaOxaPay)) {
                context.startActivity(Intent(context, OxaPayActivity::class.java))
            }

            DonateButton(text = stringResource(R.string.DonViaBTC)) {
                context.startActivity(Intent(context, BitcoinActivity::class.java))
            }

            DonateButton(text = stringResource(R.string.DonViaETH)) {
                context.startActivity(Intent(context, EthereumActivity::class.java))
            }

            DonateButton(text = stringResource(R.string.DonViaUSDT)) {
                context.startActivity(Intent(context, USDT_Activity::class.java))
            }

            DonateButton(text = stringResource(R.string.DonViaUSDC)) {
                context.startActivity(Intent(context, USDC_Activity::class.java))
            }

            DonateButton(text = stringResource(R.string.DonViaSHIB)) {
                context.startActivity(Intent(context, ShibActivity::class.java))
            }

            DonateButton(text = stringResource(R.string.DonViaDOGE)) {
                context.startActivity(Intent(context, DogeActivity::class.java))
            }

            DonateButton(text = stringResource(R.string.DonViaTRON)) {
                context.startActivity(Intent(context, TronActivity::class.java))
            }

            DonateButton(text = stringResource(R.string.DonViaLTC)) {
                context.startActivity(Intent(context, LTC_Activity::class.java))
            }

            DonateButton(text = stringResource(R.string.DonViaBNB)) {
                context.startActivity(Intent(context, BNB_Activity::class.java))
            }

            DonateButton(text = stringResource(R.string.DonViaPEPE)) {
                context.startActivity(Intent(context, PEPE_Activity::class.java))
            }

            DonateButton(text = stringResource(R.string.DonViaSOL)) {
                context.startActivity(Intent(context, SOL_Activity::class.java))
            }

            Spacer(Modifier.height(16.dp))

            FilledTonalButton(
                onClick = { context.startActivity(Intent(context, DonatorActivity::class.java)) },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(stringResource(R.string.ViewSup))
            }
        }
    }
}

@Composable
fun DonateButton(text: String, onClick: () -> Unit) {
    OutlinedButton(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(text)
    }
}

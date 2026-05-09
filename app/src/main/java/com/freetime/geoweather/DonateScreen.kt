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
                val intent = Intent(Intent.ACTION_VIEW, android.net.Uri.parse("https://github.com/sponsors/FreetimeMaker"))
                context.startActivity(intent)
            }

            Text(
                text = stringResource(R.string.crypto_label),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.align(Alignment.Start).padding(top = 8.dp)
            )

            DonateButton(text = stringResource(R.string.DonViaOxaPay)) {
                val intent = Intent(Intent.ACTION_VIEW, android.net.Uri.parse("https://pay.oxapay.com/13038067"))
                context.startActivity(intent)
            }

            DonateButton(text = stringResource(R.string.DonViaBTC)) {
                val intent = Intent(Intent.ACTION_VIEW, android.net.Uri.parse("https://ncwallet.net/pay/60misly"))
                context.startActivity(intent)
            }

            DonateButton(text = stringResource(R.string.DonViaETH)) {
                val intent = Intent(Intent.ACTION_VIEW, android.net.Uri.parse("https://ncwallet.net/pay/86fremd"))
                context.startActivity(intent)
            }

            DonateButton(text = stringResource(R.string.DonViaUSDT)) {
                val intent = Intent(Intent.ACTION_VIEW, android.net.Uri.parse("https://ncwallet.net/pay/19tacit"))
                context.startActivity(intent)
            }

            DonateButton(text = stringResource(R.string.DonViaUSDC)) {
                val intent = Intent(Intent.ACTION_VIEW, android.net.Uri.parse("https://ncwallet.net/pay/15snog"))
                context.startActivity(intent)
            }

            DonateButton(text = stringResource(R.string.DonViaSHIB)) {
                val intent = Intent(Intent.ACTION_VIEW, android.net.Uri.parse("https://ncwallet.net/pay/18spile"))
                context.startActivity(intent)
            }

            DonateButton(text = stringResource(R.string.DonViaDOGE)) {
                val intent = Intent(Intent.ACTION_VIEW, android.net.Uri.parse("https://ncwallet.net/pay/30allie"))
                context.startActivity(intent)
            }

            DonateButton(text = stringResource(R.string.DonateViaTRON)) {
                val intent = Intent(Intent.ACTION_VIEW, android.net.Uri.parse("https://ncwallet.net/pay/15gown"))
                context.startActivity(intent)
            }

            DonateButton(text = stringResource(R.string.DonViaLTC)) {
                val intent = Intent(Intent.ACTION_VIEW, android.net.Uri.parse("https://ncwallet.net/pay/77pudgy"))
                context.startActivity(intent)
            }

            DonateButton(text = stringResource(R.string.DonViaBNB)) {
                val intent = Intent(Intent.ACTION_VIEW, android.net.Uri.parse("https://ncwallet.net/pay/02hanch"))
                context.startActivity(intent)
            }

            DonateButton(text = stringResource(R.string.DonViaPEPE)) {
                val intent = Intent(Intent.ACTION_VIEW, android.net.Uri.parse("https://ncwallet.net/pay/73enow"))
                context.startActivity(intent)
            }

            DonateButton(text = stringResource(R.string.DonViaSOL)) {
                val intent = Intent(Intent.ACTION_VIEW, android.net.Uri.parse("https://ncwallet.net/pay/54fled"))
                context.startActivity(intent)
            }

            DonateButton(text = stringResource(R.string.DonViaDAI)) {
                val intent = Intent(Intent.ACTION_VIEW, android.net.Uri.parse("https://ncwallet.net/pay/27thio"))
                context.startActivity(intent)
            }

            DonateButton(text = stringResource(R.string.DonViaTON)) {
                val intent = Intent(Intent.ACTION_VIEW, android.net.Uri.parse("https://ncwallet.net/pay/22frisk"))
                context.startActivity(intent)
            }

            DonateButton(text = stringResource(R.string.DonViaPOL)) {
                val intent = Intent(Intent.ACTION_VIEW, android.net.Uri.parse("https://ncwallet.net/pay/23patas"))
                context.startActivity(intent)
            }

            DonateButton(text = stringResource(R.string.DonViaOptimism)) {
                val intent = Intent(Intent.ACTION_VIEW, android.net.Uri.parse("https://ncwallet.net/pay/77salvy"))
                context.startActivity(intent)
            }

            DonateButton(text = stringResource(R.string.DonViaARB)) {
                val intent = Intent(Intent.ACTION_VIEW, android.net.Uri.parse("https://ncwallet.net/pay/80arui"))
                context.startActivity(intent)
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

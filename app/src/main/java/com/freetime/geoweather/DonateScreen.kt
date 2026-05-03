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
                val intent = Intent(context, DonateWebViewActivity::class.java).apply {
                    putExtra("url", "https://github.com/sponsors/FreetimeMaker")
                    putExtra("title", context.getString(R.string.DonViaGHSponsors))
                }
                context.startActivity(intent)
            }

            Text(
                text = stringResource(R.string.crypto_label),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.align(Alignment.Start).padding(top = 8.dp)
            )

            DonateButton(text = stringResource(R.string.DonViaOxaPay)) {
                val intent = Intent(context, DonateWebViewActivity::class.java).apply {
                    putExtra("url", "https://pay.oxapay.com/13038067")
                    putExtra("title", context.getString(R.string.DonViaOxaPay))
                }
                context.startActivity(intent)
            }

            DonateButton(text = stringResource(R.string.DonViaBTC)) {
                val intent = Intent(context, DonateWebViewActivity::class.java).apply {
                    putExtra("url", "https://ncwallet.net/pay/60misly")
                    putExtra("title", context.getString(R.string.DonViaBTC))
                }
                context.startActivity(intent)
            }

            DonateButton(text = stringResource(R.string.DonViaETH)) {
                val intent = Intent(context, DonateWebViewActivity::class.java).apply {
                    putExtra("url", "https://ncwallet.net/pay/86fremd")
                    putExtra("title", context.getString(R.string.DonViaETH))
                }
                context.startActivity(intent)
            }

            DonateButton(text = stringResource(R.string.DonViaUSDT)) {
                val intent = Intent(context, DonateWebViewActivity::class.java).apply {
                    putExtra("url", "https://ncwallet.net/pay/19tacit")
                    putExtra("title", context.getString(R.string.DonViaUSDT))
                }
                context.startActivity(intent)
            }

            DonateButton(text = stringResource(R.string.DonViaUSDC)) {
                val intent = Intent(context, DonateWebViewActivity::class.java).apply {
                    putExtra("url", "https://ncwallet.net/pay/15snog")
                    putExtra("title", context.getString(R.string.DonViaUSDC))
                }
                context.startActivity(intent)
            }

            DonateButton(text = stringResource(R.string.DonViaSHIB)) {
                val intent = Intent(context, DonateWebViewActivity::class.java).apply {
                    putExtra("url", "https://ncwallet.net/pay/18spile")
                    putExtra("title", context.getString(R.string.DonViaSHIB))
                }
                context.startActivity(intent)
            }

            DonateButton(text = stringResource(R.string.DonViaDOGE)) {
                val intent = Intent(context, DonateWebViewActivity::class.java).apply {
                    putExtra("url", "https://ncwallet.net/pay/30allie")
                    putExtra("title", context.getString(R.string.DonViaDOGE))
                }
                context.startActivity(intent)
            }

            DonateButton(text = stringResource(R.string.DonViaTRON)) {
                val intent = Intent(context, DonateWebViewActivity::class.java).apply {
                    putExtra("url", "https://ncwallet.net/pay/15gown")
                    putExtra("title", context.getString(R.string.DonViaTRON))
                }
                context.startActivity(intent)
            }

            DonateButton(text = stringResource(R.string.DonViaLTC)) {
                val intent = Intent(context, DonateWebViewActivity::class.java).apply {
                    putExtra("url", "https://ncwallet.net/pay/77pudgy")
                    putExtra("title", context.getString(R.string.DonViaLTC))
                }
                context.startActivity(intent)
            }

            DonateButton(text = stringResource(R.string.DonViaBNB)) {
                val intent = Intent(context, DonateWebViewActivity::class.java).apply {
                    putExtra("url", "https://ncwallet.net/pay/02hanch")
                    putExtra("title", context.getString(R.string.DonViaBNB))
                }
                context.startActivity(intent)
            }

            DonateButton(text = stringResource(R.string.DonViaPEPE)) {
                val intent = Intent(context, DonateWebViewActivity::class.java).apply {
                    putExtra("url", "https://ncwallet.net/pay/73enow")
                    putExtra("title", context.getString(R.string.DonViaPEPE))
                }
                context.startActivity(intent)
            }

            DonateButton(text = stringResource(R.string.DonViaSOL)) {
                val intent = Intent(context, DonateWebViewActivity::class.java).apply {
                    putExtra("url", "https://ncwallet.net/pay/54fled")
                    putExtra("title", context.getString(R.string.DonViaSOL))
                }
                context.startActivity(intent)
            }

            DonateButton(text = stringResource(R.string.DonViaDAI)) {
                val intent = Intent(context, DonateWebViewActivity::class.java).apply {
                    putExtra("url", "https://ncwallet.net/pay/27thio")
                    putExtra("title", context.getString(R.string.DonViaDAI))
                }
                context.startActivity(intent)
            }

            DonateButton(text = stringResource(R.string.DonViaTON)) {
                val intent = Intent(context, DonateWebViewActivity::class.java).apply {
                    putExtra("url", "https://ncwallet.net/pay/22frisk")
                    putExtra("title", context.getString(R.string.DonViaTON))
                }
                context.startActivity(intent)
            }

            DonateButton(text = stringResource(R.string.DonViaPOL)) {
                val intent = Intent(context, DonateWebViewActivity::class.java).apply {
                    putExtra("url", "https://ncwallet.net/pay/23patas")
                    putExtra("title", context.getString(R.string.DonViaPOL))
                }
                context.startActivity(intent)
            }

            DonateButton(text = stringResource(R.string.DonViaOptimism)) {
                val intent = Intent(context, DonateWebViewActivity::class.java).apply {
                    putExtra("url", "https://ncwallet.net/pay/77salvy")
                    putExtra("title", context.getString(R.string.DonViaOptimism))
                }
                context.startActivity(intent)
            }

            DonateButton(text = stringResource(R.string.DonViaARB)) {
                val intent = Intent(context, DonateWebViewActivity::class.java).apply {
                    putExtra("url", "https://ncwallet.net/pay/80arui")
                    putExtra("title", context.getString(R.string.DonViaARB))
                }
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

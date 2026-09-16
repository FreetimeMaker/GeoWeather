package com.freetime.geoweather.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.freetime.geoweather.isDesktop
import com.freetime.geoweather.openUrl
import geoweather.shared.generated.resources.*
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

private data class ExternalDonation(val labelKey: StringResource, val url: String)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DonateScreen(
    onBack: () -> Unit,
    onWebViewClick: (String, String) -> Unit
) {
    val snackbarHostState = remember { SnackbarHostState() }

    val externalDonations = listOf(
        ExternalDonation(Res.string.don_nowpayments_via, "https://nowpayments.io/donation/GeoWeather"),
        ExternalDonation(Res.string.DonViaOxaPay, "https://pay.oxapay.com/13038067"),
        ExternalDonation(Res.string.DonViaBTC, "https://ncwallet.net/pay/60misly"),
        ExternalDonation(Res.string.DonViaETH, "https://ncwallet.net/pay/86fremd"),
        ExternalDonation(Res.string.DonViaUSDT, "https://ncwallet.net/pay/19tacit"),
        ExternalDonation(Res.string.DonViaUSDC, "https://ncwallet.net/pay/15snog"),
        ExternalDonation(Res.string.DonViaSHIB, "https://ncwallet.net/pay/18spile"),
        ExternalDonation(Res.string.DonViaDOGE, "https://ncwallet.net/pay/30allie"),
        ExternalDonation(Res.string.DonateViaTRON, "https://ncwallet.net/pay/15gown"),
        ExternalDonation(Res.string.DonViaLTC, "https://ncwallet.net/pay/77pudgy"),
        ExternalDonation(Res.string.DonViaBNB, "https://ncwallet.net/pay/02hanch"),
        ExternalDonation(Res.string.DonViaPEPE, "https://ncwallet.net/pay/73enow"),
        ExternalDonation(Res.string.DonViaSOL, "https://ncwallet.net/pay/54fled"),
        ExternalDonation(Res.string.DonViaDAI, "https://ncwallet.net/pay/27thio"),
        ExternalDonation(Res.string.DonViaTON, "https://ncwallet.net/pay/22frisk"),
        ExternalDonation(Res.string.DonViaPOL, "https://ncwallet.net/pay/23patas"),
        ExternalDonation(Res.string.DonViaOptimism, "https://ncwallet.net/pay/77salvy"),
        ExternalDonation(Res.string.DonViaARB, "https://ncwallet.net/pay/80arui")
    )

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text(stringResource(Res.string.donate_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(Res.string.back_nav_desc))
                    }
                }
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(innerPadding).padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = stringResource(Res.string.support_development),
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = stringResource(Res.string.select_option_msg),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            }

            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = stringResource(Res.string.about_developer_title),
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = stringResource(Res.string.about_developer_text),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = stringResource(Res.string.donation_mission_title),
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = stringResource(Res.string.donation_mission_text),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            item {
                Text(
                    text = stringResource(Res.string.cash_label),
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
                )
            }

            item {
                DonateButton(text = stringResource(Res.string.DonViaGHSponsors)) {
                    if (isDesktop) {
                        openUrl("https://github.com/sponsors/FreetimeMaker")
                    } else {
                        onWebViewClick("https://github.com/sponsors/FreetimeMaker", "GitHub Sponsors")
                    }
                }
            }

            item {
                Text(
                    text = stringResource(Res.string.crypto_label),
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
                )
            }

            items(externalDonations, key = { it.url }) { donation ->
                val label = stringResource(donation.labelKey)
                DonateButton(text = label) {
                    if (isDesktop) {
                        openUrl(donation.url)
                    } else {
                        onWebViewClick(donation.url, label)
                    }
                }
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

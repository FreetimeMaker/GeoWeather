package com.freetime.geoweather.ui

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.freetime.geoweather.R as Res
import com.freetime.geoweather.copyToClipboard
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.ui.text.font.FontFamily
import com.freetime.geoweather.ui.glass.geoWeatherGlass

private data class ExternalDonation(@StringRes val labelKey: Int, val url: String)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DonateScreen(
    onBack: () -> Unit,
    onWebViewClick: (String, String) -> Unit
) {
    val snackbarHostState = remember { SnackbarHostState() }

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
        containerColor = Color.Transparent,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                    .geoWeatherGlass(RoundedCornerShape(28.dp), interactive = false),
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
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
                    modifier = Modifier.fillMaxWidth().geoWeatherGlass(RoundedCornerShape(24.dp), interactive = false),
                    colors = CardDefaults.cardColors(containerColor = Color.Transparent)
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
                Card(modifier = Modifier.fillMaxWidth().geoWeatherGlass(RoundedCornerShape(24.dp), interactive = false), colors = CardDefaults.cardColors(containerColor = Color.Transparent)) {
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
                Card(modifier = Modifier.fillMaxWidth().geoWeatherGlass(RoundedCornerShape(24.dp), interactive = false), colors = CardDefaults.cardColors(containerColor = Color.Transparent)) {
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
                    onWebViewClick("https://github.com/sponsors/FreetimeMaker", "GitHub Sponsors")
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
                    onWebViewClick(donation.url, label)
                }
            }

            item {
                Text(
                    text = "Wallet Addresses",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
                )
            }

            itemsIndexed(
                items = walletAddresses,
                key = { index, wallet -> "$index:${wallet.first}:${wallet.second}" }
            ) { _, (name, address) ->
                Card(
                    modifier = Modifier.fillMaxWidth()
                        .geoWeatherGlass(RoundedCornerShape(24.dp), interactive = false),
                    colors = CardDefaults.cardColors(containerColor = Color.Transparent)
                ) {
                    Column(Modifier.padding(16.dp)) {
                        Text(name, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.height(8.dp))
                        Text(
                            address,
                            style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.height(8.dp))
                        TextButton(
                            onClick = { copyToClipboard(address) },
                            modifier = Modifier.align(Alignment.End)
                                .geoWeatherGlass(RoundedCornerShape(18.dp))
                        ) {
                            Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Copy")
                        }
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
        modifier = Modifier.fillMaxWidth().geoWeatherGlass(RoundedCornerShape(22.dp)),
        colors = ButtonDefaults.outlinedButtonColors(containerColor = Color.Transparent)
    ) {
        Text(text)
    }
}

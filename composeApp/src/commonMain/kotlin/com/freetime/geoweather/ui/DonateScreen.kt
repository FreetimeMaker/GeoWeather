package com.freetime.geoweather.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.freetime.geoweather.ui.components.AppWebView
import geoweather.composeapp.generated.resources.*
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

sealed class DonateViewState {
    object Main : DonateViewState()
    data class WebView(val url: String, val titleRes: StringResource) : DonateViewState()
    object Supporters : DonateViewState()
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DonateScreen(onBack: () -> Unit) {
    var viewState by remember { mutableStateOf<DonateViewState>(DonateViewState.Main) }

    when (val state = viewState) {
        is DonateViewState.Main -> {
            MainDonateContent(
                onBack = onBack,
                onOpenUrl = { url, titleRes -> viewState = DonateViewState.WebView(url, titleRes) },
                onViewSupporters = { viewState = DonateViewState.Supporters }
            )
        }
        is DonateViewState.WebView -> {
            Scaffold(
                topBar = {
                    TopAppBar(
                        title = { Text(stringResource(state.titleRes)) },
                        navigationIcon = {
                            IconButton(onClick = { viewState = DonateViewState.Main }) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(Res.string.back_nav_desc))
                            }
                        }
                    )
                }
            ) { padding ->
                AppWebView(
                    url = state.url,
                    modifier = Modifier.padding(padding).fillMaxSize()
                )
            }
        }
        is DonateViewState.Supporters -> {
            SupportersScreen(
                onBack = { viewState = DonateViewState.Main }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainDonateContent(
    onBack: () -> Unit,
    onOpenUrl: (String, StringResource) -> Unit,
    onViewSupporters: () -> Unit
) {
    Scaffold(
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

            Text(
                text = stringResource(Res.string.cash_label),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.align(Alignment.Start).padding(top = 8.dp)
            )

            DonateButton(text = stringResource(Res.string.DonViaGHSponsors)) {
                onOpenUrl("https://github.com/sponsors/FreetimeMaker", Res.string.gh_sponsors)
            }

            Text(
                text = stringResource(Res.string.crypto_label),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.align(Alignment.Start).padding(top = 8.dp)
            )

            DonateButton(text = stringResource(Res.string.DonViaOxaPay)) {
                onOpenUrl("https://pay.oxapay.com/13038067", Res.string.oxapay)
            }

            DonateButton(text = stringResource(Res.string.DonViaBTC)) {
                onOpenUrl("https://ncwallet.net/pay/60misly", Res.string.btc)
            }

            DonateButton(text = stringResource(Res.string.DonViaETH)) {
                onOpenUrl("https://ncwallet.net/pay/86fremd", Res.string.eth)
            }

            DonateButton(text = stringResource(Res.string.DonViaUSDT)) {
                onOpenUrl("https://ncwallet.net/pay/19tacit", Res.string.usdt)
            }

            DonateButton(text = stringResource(Res.string.DonViaUSDC)) {
                onOpenUrl("https://ncwallet.net/pay/15snog", Res.string.usdc)
            }

            DonateButton(text = stringResource(Res.string.DonViaSHIB)) {
                onOpenUrl("https://ncwallet.net/pay/18spile", Res.string.shib)
            }

            DonateButton(text = stringResource(Res.string.DonViaDOGE)) {
                onOpenUrl("https://ncwallet.net/pay/30allie", Res.string.doge)
            }

            DonateButton(text = stringResource(Res.string.DonViaTRON)) {
                onOpenUrl("https://ncwallet.net/pay/15gown", Res.string.tron)
            }

            DonateButton(text = stringResource(Res.string.DonViaLTC)) {
                onOpenUrl("https://ncwallet.net/pay/77pudgy", Res.string.ltc)
            }

            DonateButton(text = stringResource(Res.string.DonViaBNB)) {
                onOpenUrl("https://ncwallet.net/pay/02hanch", Res.string.bnb)
            }

            DonateButton(text = stringResource(Res.string.DonViaPEPE)) {
                onOpenUrl("https://ncwallet.net/pay/73enow", Res.string.pepe)
            }

            DonateButton(text = stringResource(Res.string.DonViaSOL)) {
                onOpenUrl("https://ncwallet.net/pay/54fled", Res.string.sol)
            }

            DonateButton(text = stringResource(Res.string.DonViaDAI)) {
                onOpenUrl("https://ncwallet.net/pay/27thio", Res.string.dai)
            }

            DonateButton(text = stringResource(Res.string.DonViaTON)) {
                onOpenUrl("https://ncwallet.net/pay/22frisk", Res.string.ton)
            }

            DonateButton(text = stringResource(Res.string.DonViaPOL)) {
                onOpenUrl("https://ncwallet.net/pay/23patas", Res.string.pol)
            }

            Spacer(Modifier.height(16.dp))

            FilledTonalButton(
                onClick = onViewSupporters,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(stringResource(Res.string.ViewSup))
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SupportersScreen(onBack: () -> Unit) {
    val uriHandler = LocalUriHandler.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(Res.string.supporters_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(Res.string.back_nav_desc))
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
                text = stringResource(Res.string.DonTXT1),
                style = MaterialTheme.typography.bodyLarge
            )

            Text(
                textAlign = TextAlign.Center,
                text = stringResource(Res.string.DonTXT2),
                style = MaterialTheme.typography.bodyLarge
            )

            Text(
                textAlign = TextAlign.Center,
                text = stringResource(Res.string.DonPers),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Button(
                onClick = { uriHandler.openUri("https://discord.gg/zPFvwK9pNh") },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(stringResource(Res.string.JoiOffDisSer))
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

package io.github.freetimemaker.geoweather.ui
import io.github.freetimemaker.geoweather.R

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
import io.github.freetimemaker.geoweather.ui.components.AppWebView
import androidx.compose.ui.res.stringResource

sealed class DonateViewState {
    object Main : DonateViewState()
    data class WebView(val url: String, val titleRes: Int) : DonateViewState()
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
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back_nav_desc))
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
    onOpenUrl: (String, Int) -> Unit,
    onViewSupporters: () -> Unit
) {
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
                onOpenUrl("https://github.com/sponsors/FreetimeMaker", R.string.gh_sponsors)
            }

            Text(
                text = stringResource(R.string.crypto_label),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.align(Alignment.Start).padding(top = 8.dp)
            )

            DonateButton(text = stringResource(R.string.DonViaOxaPay)) {
                onOpenUrl("https://pay.oxapay.com/13038067", R.string.oxapay)
            }

            DonateButton(text = stringResource(R.string.DonViaBTC)) {
                onOpenUrl("https://ncwallet.net/pay/60misly", R.string.btc)
            }

            DonateButton(text = stringResource(R.string.DonViaETH)) {
                onOpenUrl("https://ncwallet.net/pay/86fremd", R.string.eth)
            }

            DonateButton(text = stringResource(R.string.DonViaUSDT)) {
                onOpenUrl("https://ncwallet.net/pay/19tacit", R.string.usdt)
            }

            DonateButton(text = stringResource(R.string.DonViaUSDC)) {
                onOpenUrl("https://ncwallet.net/pay/15snog", R.string.usdc)
            }

            DonateButton(text = stringResource(R.string.DonViaSHIB)) {
                onOpenUrl("https://ncwallet.net/pay/18spile", R.string.shib)
            }

            DonateButton(text = stringResource(R.string.DonViaDOGE)) {
                onOpenUrl("https://ncwallet.net/pay/30allie", R.string.doge)
            }

            DonateButton(text = stringResource(R.string.DonViaTRON)) {
                onOpenUrl("https://ncwallet.net/pay/15gown", R.string.tron)
            }

            DonateButton(text = stringResource(R.string.DonViaLTC)) {
                onOpenUrl("https://ncwallet.net/pay/77pudgy", R.string.ltc)
            }

            DonateButton(text = stringResource(R.string.DonViaBNB)) {
                onOpenUrl("https://ncwallet.net/pay/02hanch", R.string.bnb)
            }

            DonateButton(text = stringResource(R.string.DonViaPEPE)) {
                onOpenUrl("https://ncwallet.net/pay/73enow", R.string.pepe)
            }

            DonateButton(text = stringResource(R.string.DonViaSOL)) {
                onOpenUrl("https://ncwallet.net/pay/54fled", R.string.sol)
            }

            DonateButton(text = stringResource(R.string.DonViaDAI)) {
                onOpenUrl("https://ncwallet.net/pay/27thio", R.string.dai)
            }

            DonateButton(text = stringResource(R.string.DonViaTON)) {
                onOpenUrl("https://ncwallet.net/pay/22frisk", R.string.ton)
            }

            DonateButton(text = stringResource(R.string.DonViaPOL)) {
                onOpenUrl("https://ncwallet.net/pay/23patas", R.string.pol)
            }

            Spacer(Modifier.height(16.dp))

            FilledTonalButton(
                onClick = onViewSupporters,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(stringResource(R.string.ViewSup))
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
                title = { Text(stringResource(R.string.supporters_title)) },
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
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            Text(
                textAlign = TextAlign.Center,
                text = stringResource(R.string.DonTXT1),
                style = MaterialTheme.typography.bodyLarge
            )

            Text(
                textAlign = TextAlign.Center,
                text = stringResource(R.string.DonTXT2),
                style = MaterialTheme.typography.bodyLarge
            )

            Text(
                textAlign = TextAlign.Center,
                text = stringResource(R.string.DonPers),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Button(
                onClick = { uriHandler.openUri("https://discord.gg/zPFvwK9pNh") },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(stringResource(R.string.JoiOffDisSer))
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
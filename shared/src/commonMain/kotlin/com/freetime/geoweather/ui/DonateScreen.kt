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
import com.freetime.geoweather.copyToClipboard
import com.freetime.geoweather.data.DependencyManager
import com.freetime.geoweather.isDesktop
import com.freetime.geoweather.openUrl
import com.freetime.geoweather.rememberPaymentContext
import com.freetime.sdk.PaymentProvider
import com.freetime.sdk.PaymentRequest
import com.freetime.sdk.PaymentResult
import com.freetime.sdk.UriPaymentProvider
import geoweather.shared.generated.resources.*
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource

private data class ExternalDonation(val labelKey: org.jetbrains.compose.resources.StringResource, val url: String)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DonateScreen(
    onBack: () -> Unit,
    onWalletAddressesClick: () -> Unit,
    onWebViewClick: (String, String) -> Unit
) {
    val paymentContext = rememberPaymentContext()
    val freetimePay = DependencyManager.getFreetimePay()
    val providers = remember { freetimePay.getAvailableProviders() }
    var showAmountDialog by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    val successMessage = stringResource(Res.string.payment_successful)
    val paymentFailed = stringResource(Res.string.payment_failed)

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
                PromotionView(onWebViewClick = onWebViewClick)
            }

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

            item {
                DonateButton(text = stringResource(Res.string.DonViaFMSDK)) {
                    showAmountDialog = true
                }
            }

            item {
                DonateButton(text = stringResource(Res.string.show_wallet_addresses)) {
                    onWalletAddressesClick()
                }
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

    if (showAmountDialog) {
        DonationAmountDialog(
            providers = providers,
            onDismiss = { showAmountDialog = false },
            onDonate = { provider, amount, currency ->
                showAmountDialog = false
                val request = PaymentRequest(
                    amount = amount,
                    currency = currency,
                    description = "GeoWeather Donation"
                )
                freetimePay.processPayment(paymentContext, provider.name, request) { result ->
                    scope.launch {
                        val message = when (result) {
                            is PaymentResult.Success -> successMessage
                            is PaymentResult.Error -> "$paymentFailed: ${result.message}"
                            PaymentResult.Cancelled -> null
                        }
                        message?.let { snackbarHostState.showSnackbar(it) }
                    }
                }
            }
        )
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WalletAddressesScreen(onBack: () -> Unit) {
    val freetimePay = DependencyManager.getFreetimePay()
    val providers = remember { freetimePay.getAvailableProviders() }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val copiedMessage = stringResource(Res.string.address_copied)

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text(stringResource(Res.string.wallet_addresses_title)) },
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
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(providers, key = { it.name }) { provider ->
                val address = (provider as? UriPaymentProvider)?.recipientAddress ?: return@items
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(provider.name, style = MaterialTheme.typography.titleMedium)
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = address,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.height(8.dp))
                        OutlinedButton(
                            onClick = {
                                copyToClipboard(address)
                                scope.launch { snackbarHostState.showSnackbar(copiedMessage) }
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(stringResource(Res.string.copy_address))
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DonationAmountDialog(
    providers: List<PaymentProvider>,
    onDismiss: () -> Unit,
    onDonate: (PaymentProvider, Double, String) -> Unit
) {
    val predefinedAmounts = listOf(2.0, 5.0, 10.0, 25.0, 50.0)
    val fiatCurrencies = listOf("USD", "EUR", "GBP", "JPY", "AUD", "CAD", "CHF", "CNY", "HKD", "NZD")
    val cryptoCurrencies = remember {
        providers.map { provider ->
            if (provider.name.contains("(") && provider.name.contains(")")) {
                provider.name.substringAfterLast("(").substringBefore(")")
            } else {
                provider.name
            }
        }.filter { it.length <= 5 && !it.contains("/") && !it.contains(" ") && it != "Mock" }.distinct()
    }
    val currencies = fiatCurrencies + cryptoCurrencies

    var selectedCurrency by remember { mutableStateOf("USD") }
    var selectedProviderName by remember(providers) {
        mutableStateOf(providers.firstOrNull()?.name ?: "")
    }

    // Auto-update currency when provider changes for Crypto
    LaunchedEffect(selectedProviderName) {
        val provider = providers.find { it.name == selectedProviderName }
        if (provider != null) {
            if (provider.name.contains("(") && provider.name.contains(")")) {
                selectedCurrency = provider.name.substringAfterLast("(").substringBefore(")")
            } else if (provider.name == "RevenueCat (Card/Subscription)" || provider.name == "One-Time 2 USD Donation") {
                // Keep USD for RevenueCat by default or let user choose
                if (selectedCurrency !in fiatCurrencies) selectedCurrency = "USD"
            }
        }
    }
    var customAmount by remember { mutableStateOf("") }
    var selectedPredefined by remember { mutableStateOf<Double?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    var expanded by remember { mutableStateOf(false) }
    var providerExpanded by remember { mutableStateOf(false) }

    val invalidAmountMsg = stringResource(Res.string.invalid_amount_msg)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(Res.string.select_donation_amount)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text(stringResource(Res.string.billing_options_title), style = MaterialTheme.typography.labelMedium)

                ExposedDropdownMenuBox(
                    expanded = providerExpanded,
                    onExpandedChange = { providerExpanded = !providerExpanded }
                ) {
                    OutlinedTextField(
                        value = selectedProviderName,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text(stringResource(Res.string.select_payment_method)) },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = providerExpanded) },
                        modifier = Modifier.menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable, true).fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = providerExpanded,
                        onDismissRequest = { providerExpanded = false }
                    ) {
                        providers.forEach { provider ->
                            DropdownMenuItem(
                                text = { Text(provider.name) },
                                onClick = {
                                    selectedProviderName = provider.name
                                    providerExpanded = false
                                }
                            )
                        }
                    }
                }

                val isCrypto = providers.find { it.name == selectedProviderName }?.name?.contains("(") ?: false

                ExposedDropdownMenuBox(
                    expanded = expanded && !isCrypto,
                    onExpandedChange = { if (!isCrypto) expanded = !expanded }
                ) {
                    OutlinedTextField(
                        value = selectedCurrency,
                        onValueChange = {},
                        readOnly = true,
                        enabled = !isCrypto,
                        label = { Text(stringResource(Res.string.currency_label)) },
                        trailingIcon = { if (!isCrypto) ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                        modifier = Modifier.menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable, true).fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        currencies.forEach { currency ->
                            DropdownMenuItem(
                                text = { Text(currency) },
                                onClick = {
                                    selectedCurrency = currency
                                    expanded = false
                                }
                            )
                        }
                    }
                }

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    val chunks = predefinedAmounts.chunked(3)
                    chunks.forEach { chunk ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            chunk.forEach { amount ->
                                FilterChip(
                                    selected = selectedPredefined == amount && customAmount.isEmpty(),
                                    onClick = {
                                        selectedPredefined = amount
                                        customAmount = ""
                                        error = null
                                    },
                                    label = {
                                        val symbol = when (selectedCurrency) {
                                            "USD" -> "$"
                                            "EUR" -> "€"
                                            "GBP" -> "£"
                                            "JPY" -> "¥"
                                            "CNY" -> "¥"
                                            else -> null
                                        }
                                        if (symbol != null) {
                                            Text("$symbol${amount.toInt()}")
                                        } else {
                                            Text("${amount.toInt()} $selectedCurrency")
                                        }
                                    },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                            repeat(3 - chunk.size) { Spacer(Modifier.weight(1f)) }
                        }
                    }
                }

                HorizontalDivider()

                OutlinedTextField(
                    value = customAmount,
                    onValueChange = {
                        customAmount = it
                        selectedPredefined = null
                        error = null
                    },
                    label = { Text(stringResource(Res.string.custom_amount_label)) },
                    placeholder = {
                        Text(stringResource(Res.string.amount_in_currency_hint, selectedCurrency))
                    },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    isError = error != null,
                    supportingText = { error?.let { Text(it) } }
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val finalAmount = if (customAmount.isNotEmpty()) {
                        customAmount.toDoubleOrNull()
                    } else {
                        selectedPredefined
                    }
                    val provider = providers.find { it.name == selectedProviderName }

                    if (provider != null && finalAmount != null && finalAmount > 0) {
                        onDonate(provider, finalAmount, selectedCurrency)
                    } else {
                        error = invalidAmountMsg
                    }
                }
            ) {
                Text(stringResource(Res.string.donate_btn))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(Res.string.cancel_btn))
            }
        }
    )
}

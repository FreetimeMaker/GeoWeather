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
import com.freetime.geoweather.data.DependencyManager
import com.freetime.geoweather.openUrl
import com.freetime.geoweather.rememberPaymentContext
import com.freetime.sdk.PaymentProvider
import com.freetime.sdk.PaymentRequest
import com.freetime.sdk.PaymentResult
import com.freetime.sdk.UriPaymentProvider
import geoweather.shared.generated.resources.*
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DonateScreen(onBack: () -> Unit) {
    val paymentContext = rememberPaymentContext()
    val freetimePay = DependencyManager.getFreetimePay()
    val providers = remember { freetimePay.getAvailableProviders() }
    var selectedProvider by remember { mutableStateOf<PaymentProvider?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    val successMessage = stringResource(Res.string.payment_successful)
    val paymentFailed = stringResource(Res.string.payment_failed)

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
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Text(
                    textAlign = TextAlign.Center,
                    text = stringResource(Res.string.support_development),
                    style = MaterialTheme.typography.headlineMedium
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    textAlign = TextAlign.Center,
                    text = stringResource(Res.string.donation_mission_text),
                    style = MaterialTheme.typography.bodyLarge
                )
            }

            item {
                Text(stringResource(Res.string.select_option_msg), style = MaterialTheme.typography.titleMedium)
            }

            items(providers) { provider ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = { selectedProvider = provider }
                ) {
                    ListItem(
                        headlineContent = { Text(provider.name) },
                        supportingContent = {
                            val address = (provider as? UriPaymentProvider)?.recipientAddress
                            if (address != null && address.length > 24) {
                                Text("${address.take(12)}...${address.takeLast(8)}")
                            } else if (address != null) {
                                Text(address)
                            }
                        }
                    )
                }
            }

            item {
                Button(
                    onClick = { openUrl("https://github.com/sponsors/FreetimeMaker") },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(stringResource(Res.string.gh_sponsors))
                }
            }
        }
    }

    selectedProvider?.let { provider ->
        DonationAmountDialog(
            providers = providers,
            onDismiss = { selectedProvider = null },
            onDonate = { amount, currency ->
                selectedProvider = null
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DonationAmountDialog(
    providers: List<PaymentProvider>,
    onDismiss: () -> Unit,
    onDonate: (Double, String) -> Unit
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
    var customAmount by remember { mutableStateOf("") }
    var selectedPredefined by remember { mutableStateOf<Double?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    var expanded by remember { mutableStateOf(false) }

    val invalidAmountMsg = stringResource(Res.string.invalid_amount_msg)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(Res.string.select_donation_amount)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text(stringResource(Res.string.billing_options_title), style = MaterialTheme.typography.labelMedium)

                ExposedDropdownMenuBox(
                    expanded = expanded,
                    onExpandedChange = { expanded = !expanded }
                ) {
                    OutlinedTextField(
                        value = selectedCurrency,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text(stringResource(Res.string.currency_label)) },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
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

                    if (finalAmount != null && finalAmount > 0) {
                        onDonate(finalAmount, selectedCurrency)
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
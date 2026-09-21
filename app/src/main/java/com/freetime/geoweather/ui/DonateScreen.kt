package com.freetime.geoweather.ui
import com.freetime.design.FreetimeIconButton
import com.freetime.design.FreetimeDesign
import com.freetime.design.freetimeGlass
import com.freetime.design.FreetimeGlassTopBar
import com.freetime.design.FreetimeGlassAction
import com.freetime.design.FreetimeGlassPanel

import androidx.annotation.StringRes
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.freetime.geoweather.R as Res
import com.freetime.geoweather.copyToClipboard
import com.freetime.design.freetimeGlass
import com.freetime.design.FreetimeGlassTopBar
import com.freetime.design.FreetimeGlassPanel
import com.freetime.design.FreetimeGlassAction
import com.freetime.donations.DonationTarget
import com.freetime.donations.FreetimeDonationScreen

private data class ExternalDonation(@StringRes val labelKey: Int, val url: String)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DonateScreen(
    onBack: () -> Unit,
    onWebViewClick: (String, String) -> Unit
) {
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

    val targets = buildList<DonationTarget> {
        add(DonationTarget.Link(stringResource(Res.string.DonViaGHSponsors), "https://github.com/sponsors/FreetimeMaker"))
        externalDonations.forEach { donation ->
            add(DonationTarget.Link(stringResource(donation.labelKey), donation.url))
        }
        walletAddresses.forEach { (label, address) ->
            add(DonationTarget.Wallet(label = label, currency = label.substringBefore(" ("), address = address))
        }
    }

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            FreetimeGlassTopBar(
                title = stringResource(Res.string.donate_title),
                navigation = { FreetimeIconButton(icon = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null, onClick = onBack) }
            )
        }
    ) { innerPadding ->
        FreetimeDonationScreen(
            targets = targets,
            title = stringResource(Res.string.support_development),
            onLinkClick = { target -> onWebViewClick(target.url, target.label) },
            onWalletClick = { target -> copyToClipboard(target.address) },
            onCopyWallet = { target -> copyToClipboard(target.address) },
            modifier = Modifier.fillMaxSize().padding(innerPadding)
        )
    }
}

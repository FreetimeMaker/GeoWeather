package com.freetime.geoweather

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.freetime.geoweather.ui.theme.GeoWeatherTheme

class SubscriptionActivity : ComponentActivity() {
    private lateinit var authManager: AuthManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        hideSystemBars()
        authManager = AuthManager.getInstance(this)

        setContent {
            GeoWeatherTheme {
                SubscriptionScreen(onBack = { finish() }, authManager = authManager)
            }
        }
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) {
            hideSystemBars()
        }
    }

    private fun hideSystemBars() {
        val windowInsetsController =
            WindowCompat.getInsetsController(window, window.decorView)
        windowInsetsController.systemBarsBehavior =
            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        windowInsetsController.hide(WindowInsetsCompat.Type.systemBars())
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SubscriptionScreen(onBack: () -> Unit, authManager: AuthManager) {
    val context = LocalContext.current
    val isAuthenticated = authManager.isAuthenticated

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.subscription_title)) },
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
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            if (!isAuthenticated) {
                Text(
                    text = stringResource(R.string.subscription_login_required),
                    style = MaterialTheme.typography.bodyLarge
                )

                Spacer(modifier = Modifier.height(24.dp))

                Button(
                    onClick = {
                        context.startActivity(Intent(context, AuthActivity::class.java))
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(stringResource(R.string.login_title))
                }
            } else {
                Text(
                    text = stringResource(R.string.subscription_explain),
                    style = MaterialTheme.typography.bodyLarge
                )

                SubscriptionTierCard(
                    title = stringResource(R.string.tier_free),
                    description = stringResource(R.string.tier_description_free),
                    buttonText = null,
                    onButtonClick = {}
                )

                SubscriptionTierCard(
                    title = stringResource(R.string.tier_freemium),
                    description = stringResource(R.string.tier_description_freemium),
                    buttonText = stringResource(R.string.support_freemium_button),
                    onButtonClick = {
                        val intent = Intent(Intent.ACTION_VIEW, android.net.Uri.parse("https://pay.oxapay.com/13038067"))
                        context.startActivity(intent)
                    }
                )

                SubscriptionTierCard(
                    title = stringResource(R.string.tier_premium),
                    description = stringResource(R.string.tier_description_premium),
                    buttonText = stringResource(R.string.support_premium_button),
                    onButtonClick = {
                        val intent = Intent(Intent.ACTION_VIEW, android.net.Uri.parse("https://pay.oxapay.com/13038068"))
                        context.startActivity(intent)
                    }
                )
            }
        }
    }
}

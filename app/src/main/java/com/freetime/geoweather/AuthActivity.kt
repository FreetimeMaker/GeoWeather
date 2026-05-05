package com.freetime.geoweather

import android.content.Intent
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.freetime.geoweather.ui.theme.GeoWeatherTheme

class AuthActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: android.os.Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val authMgr = AuthManager.getInstance(this)

        setContent {
            GeoWeatherTheme {
                val isAuthenticated = remember { mutableStateOf(authMgr.isAuthenticated) }
                
                if (isAuthenticated.value) {
                    UserInfoScreen(
                        authManager = authMgr,
                        onLogout = {
                            authMgr.logout()
                            isAuthenticated.value = false
                        },
                        onBack = { finish() }
                    )
                } else {
                    AuthScreenContent(
                        onLoginClick = {
                            val intent = Intent(Intent.ACTION_VIEW, android.net.Uri.parse("${ApiConstants.BASE_URL}/api/v1/auth/github"))
                            startActivity(intent)
                        }
                    )
                }
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

@Composable
private fun UserInfoScreen(
    authManager: com.freetime.geoweather.AuthManager,
    onLogout: () -> Unit,
    onBack: () -> Unit
) {
    val userInfo = authManager.userInfo

    Scaffold(
        topBar = {
            @OptIn(ExperimentalMaterial3Api::class)
            TopAppBar(
                title = { Text(stringResource(R.string.account_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back_nav_desc))
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                Icons.Default.AccountCircle,
                contentDescription = null,
                modifier = Modifier.size(100.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Text(text = stringResource(R.string.logged_in_as), style = MaterialTheme.typography.labelLarge)
            Text(text = userInfo?.name ?: stringResource(R.string.unknown_location), style = MaterialTheme.typography.headlineMedium)
            Text(text = userInfo?.email ?: "", style = MaterialTheme.typography.bodyLarge)
            
            val tierString = if (userInfo?.subscriptionTier == "premium") stringResource(R.string.tier_premium) else if (userInfo?.subscriptionTier == "freemium") stringResource(R.string.tier_freemium) else stringResource(R.string.tier_free)
            Text(text = stringResource(R.string.subscription_tier, tierString), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
            
            Spacer(modifier = Modifier.height(32.dp))
            
            Button(
                onClick = onLogout,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
            ) {
                Text(stringResource(R.string.logout_button))
            }
        }
    }
}

@Composable
private fun AuthScreenContent(
    onLoginClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = Icons.Default.AccountCircle,
            contentDescription = null,
            modifier = Modifier.size(100.dp),
            tint = MaterialTheme.colorScheme.primary
        )

        Spacer(modifier = Modifier.height(32.dp))

        Text(
            text = stringResource(R.string.login_title),
            style = MaterialTheme.typography.headlineMedium
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = stringResource(R.string.github_login_desc),
            style = MaterialTheme.typography.bodyLarge,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(48.dp))

        Button(
            onClick = onLoginClick,
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.medium
        ) {
            Icon(
                painter = painterResource(id = android.R.drawable.ic_menu_info_details), // Just a placeholder icon
                contentDescription = null
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(stringResource(R.string.login_with_github))
        }
    }
}

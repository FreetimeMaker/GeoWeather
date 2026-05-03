package com.freetime.geoweather

import android.content.Intent
import android.widget.Toast
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.freetime.geoweather.ui.theme.GeoWeatherTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class AuthActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: android.os.Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val authMgr = com.freetime.geoweather.AuthManager.getInstance(this)

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
                        authManager = authMgr,
                        onLoginSuccess = { startMainActivity() },
                        onRegisterSuccess = { startMainActivity() }
                    )
                }
            }
        }
    }

    private fun startMainActivity() {
        val intent = Intent(this, MainActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        startActivity(intent)
        finish()
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
            
            val tierString = if (userInfo?.subscriptionTier == "pro") stringResource(R.string.tier_pro) else stringResource(R.string.tier_free)
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
    authManager: com.freetime.geoweather.AuthManager,
    onLoginSuccess: () -> Unit,
    onRegisterSuccess: () -> Unit
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var name by remember { mutableStateOf("") }
    var isLogin by remember { mutableStateOf(true) }
    var isLoading by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = if (isLogin) stringResource(R.string.login_title) else stringResource(R.string.register_title),
            style = MaterialTheme.typography.headlineMedium
        )

        Spacer(modifier = Modifier.height(32.dp))

        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            label = { Text(stringResource(R.string.email_label)) },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))

        if (!isLogin) {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text(stringResource(R.string.name_label)) },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(16.dp))
        }

        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text(stringResource(R.string.password_label)) },
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(32.dp))

        Button(
            onClick = {
                if (email.isBlank() || password.isBlank() || (!isLogin && name.isBlank())) {
                    Toast.makeText(context, context.getString(R.string.fill_all_fields), Toast.LENGTH_SHORT).show()
                    return@Button
                }

                isLoading = true
                scope.launch(Dispatchers.Main) {
                    try {
                        val result = if (isLogin) {
                            authManager.login(email, password, com.freetime.geoweather.ApiConstants.BASE_URL)
                        } else {
                            authManager.register(email, password, name, com.freetime.geoweather.ApiConstants.BASE_URL)
                        }
                        isLoading = false
                        if (result.success) {
                            if (isLogin) onLoginSuccess() else onRegisterSuccess()
                        } else {
                            Toast.makeText(context, result.message, Toast.LENGTH_SHORT).show()
                        }
                    } catch (e: Exception) {
                        isLoading = false
                        Toast.makeText(context, "${context.getString(R.string.error_loading_weather)}: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                }
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = !isLoading
        ) {
            Text(if (isLogin) stringResource(R.string.login_button) else stringResource(R.string.register_button))
        }

        Spacer(modifier = Modifier.height(16.dp))

        TextButton(onClick = {
            isLogin = !isLogin
            email = ""
            password = ""
            name = ""
        }) {
            Text(if (isLogin) stringResource(R.string.no_account_msg) else stringResource(R.string.have_account_msg))
        }
    }
}

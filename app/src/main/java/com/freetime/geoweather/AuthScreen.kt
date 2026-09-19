package com.freetime.geoweather

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.activity.ComponentActivity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.freetime.geoweather.ui.glass.geoWeatherGlass
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AuthScreen(onAuthenticated: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var register by remember { mutableStateOf(false) }
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    Box(Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
        Card(
            modifier = Modifier.fillMaxWidth().widthIn(max = 520.dp)
                .geoWeatherGlass(RoundedCornerShape(32.dp), interactive = false),
            colors = CardDefaults.cardColors(containerColor = Color.Transparent),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
        ) {
            Column(
                Modifier.padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(stringResource(R.string.login_title), style = MaterialTheme.typography.headlineMedium)
                Text(stringResource(R.string.login_subtitle), color = MaterialTheme.colorScheme.onSurfaceVariant)

                if (register) {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text(stringResource(R.string.name_label)) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().geoWeatherGlass(RoundedCornerShape(20.dp))
                    )
                }
                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text(stringResource(R.string.email_label)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().geoWeatherGlass(RoundedCornerShape(20.dp))
                )
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text(stringResource(R.string.password_label)) },
                    visualTransformation = PasswordVisualTransformation(),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().geoWeatherGlass(RoundedCornerShape(20.dp))
                )

                error?.let { Text(it, color = MaterialTheme.colorScheme.error) }

                Button(
                    enabled = !loading && email.isNotBlank() && password.isNotBlank() && (!register || name.isNotBlank()),
                    onClick = {
                        loading = true
                        error = null
                        scope.launch {
                            runCatching {
                                if (register) AppwriteAuth.signUp(context, name, email, password)
                                else AppwriteAuth.signIn(context, email, password)
                            }.onSuccess {
                                onAuthenticated()
                            }.onFailure {
                                error = it.message ?: context.getString(R.string.auth_failed)
                            }
                            loading = false
                        }
                    },
                    modifier = Modifier.fillMaxWidth().geoWeatherGlass(RoundedCornerShape(24.dp)),
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent)
                ) {
                    if (loading) CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
                    else Text(stringResource(if (register) R.string.create_account else R.string.sign_in))
                }

                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedButton(
                        onClick = { scope.launch { runCatching { AppwriteAuth.signInWithGitHub(context as ComponentActivity) }.onFailure { error = it.message } } },
                        modifier = Modifier.weight(1f).geoWeatherGlass(RoundedCornerShape(20.dp)),
                        colors = ButtonDefaults.outlinedButtonColors(containerColor = Color.Transparent),
                        border = null
                    ) { Text(stringResource(R.string.sign_in_github)) }
                    OutlinedButton(
                        onClick = { scope.launch { runCatching { AppwriteAuth.signInWithGitLab(context as ComponentActivity) }.onFailure { error = it.message } } },
                        modifier = Modifier.weight(1f).geoWeatherGlass(RoundedCornerShape(20.dp)),
                        colors = ButtonDefaults.outlinedButtonColors(containerColor = Color.Transparent),
                        border = null
                    ) { Text(stringResource(R.string.sign_in_gitlab)) }
                }

                TextButton(
                    onClick = { register = !register; error = null },
                    modifier = Modifier.fillMaxWidth().geoWeatherGlass(RoundedCornerShape(20.dp))
                ) {
                    Text(stringResource(if (register) R.string.have_account else R.string.need_account))
                }
            }
        }
    }
}

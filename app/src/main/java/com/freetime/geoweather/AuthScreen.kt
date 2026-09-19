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
import androidx.activity.compose.BackHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.freetime.geoweather.ui.glass.geoWeatherGlass
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AuthScreen(onAuthenticated: () -> Unit, onBack: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var error by remember { mutableStateOf<String?>(null) }

    BackHandler(onBack = onBack)
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
                Row(verticalAlignment = Alignment.CenterVertically) {
                    TextButton(onClick = onBack) { Text(stringResource(R.string.back_nav_desc)) }
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.login_title), style = MaterialTheme.typography.headlineMedium)
                }
                Text(stringResource(R.string.login_subtitle), color = MaterialTheme.colorScheme.onSurfaceVariant)

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

            }
        }
    }
}

package com.freetime.geoweather.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.freetime.geoweather.data.DependencyManager
import com.freetime.geoweather.isDesktop
import com.freetime.geoweather.openUrl
import com.freetime.sdk.Promotion
import com.freetime.sdk.PromotionManager
import coil3.compose.AsyncImage
import kotlinx.coroutines.launch

@Composable
fun PromotionView(
    modifier: Modifier = Modifier,
    onWebViewClick: (String, String) -> Unit = { _, _ -> }
) {
    val freetimePay = DependencyManager.getFreetimePay()
    val manager = remember { PromotionManager(freetimePay.config) }
    var promotion by remember { mutableStateOf<Promotion?>(null) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        scope.launch {
            try {
                promotion = manager.fetchPromotions().firstOrNull()
            } catch (e: Exception) {
                // Ignore errors in promotions to not break the UI
            }
        }
    }

    promotion?.let { promo ->
        Card(
            modifier = modifier
                .fillMaxWidth()
                .clickable {
                    if (isDesktop) {
                        openUrl(promo.targetUrl)
                    } else {
                        onWebViewClick(promo.targetUrl, promo.title)
                    }
                },
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.7f)
            )
        ) {
            Row(
                modifier = Modifier.padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                AsyncImage(
                    model = promo.iconUrl,
                    contentDescription = null,
                    modifier = Modifier.size(48.dp)
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = promo.title,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                    Text(
                        text = promo.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.8f)
                    )
                }
            }
        }
    }
}

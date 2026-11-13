package com.freetime.geoweather.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.freetime.geoweather.GeoWeatherApp
import com.freetime.geoweather.R as Res
import com.freetime.notifications.FreetimeNotifications
import com.freetime.notifications.NotificationCategory
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

@Composable
fun FreetimeNotificationSettings(onError: (String) -> Unit) {
    val context = LocalContext.current.applicationContext
    val scope = rememberCoroutineScope()
    var categories by remember(context) { mutableStateOf<Map<NotificationCategory, Boolean>?>(null) }
    var loadFailed by remember(context) { mutableStateOf(false) }
    var loadAttempt by remember(context) { mutableIntStateOf(0) }
    var savingCategory by remember(context) { mutableStateOf<NotificationCategory?>(null) }
    val saveError = stringResource(Res.string.freetime_notifications_save_error)
    val currentOnError by rememberUpdatedState(onError)

    LaunchedEffect(context, loadAttempt) {
        loadFailed = false
        try {
            (context as GeoWeatherApp).awaitNotificationsInitialized()
            categories = listOf(
                NotificationCategory.UPDATES,
                NotificationCategory.CHANGELOGS,
                NotificationCategory.DEVELOPER_MESSAGES
            ).associateWith { category ->
                FreetimeNotifications.isCategoryEnabled(context, category)
            }
        } catch (error: CancellationException) {
            throw error
        } catch (_: Exception) {
            loadFailed = true
        }
    }

    Text(
        stringResource(Res.string.freetime_notifications_polling_info),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )

    val loadedCategories = categories
    when {
        loadFailed -> {
            Text(
                stringResource(Res.string.freetime_notifications_load_error),
                color = MaterialTheme.colorScheme.error
            )
            TextButton(onClick = { loadAttempt++ }) {
                Text(stringResource(Res.string.freetime_notifications_retry))
            }
        }
        loadedCategories == null -> {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                CircularProgressIndicator(Modifier.size(24.dp))
                Text(stringResource(Res.string.freetime_notifications_loading))
            }
        }
        else -> {
            val labels = listOf(
                NotificationCategory.UPDATES to Res.string.freetime_notifications_updates,
                NotificationCategory.CHANGELOGS to Res.string.freetime_notifications_changelogs,
                NotificationCategory.DEVELOPER_MESSAGES to Res.string.freetime_notifications_developer_messages
            )
            labels.forEach { (category, label) ->
                val checked = loadedCategories.getValue(category)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .toggleable(
                            value = checked,
                            enabled = savingCategory == null,
                            role = Role.Switch,
                            onValueChange = { enabled ->
                                // Claim the save before launching so rapid taps cannot overlap writes.
                                if (savingCategory == null) {
                                    savingCategory = category
                                    scope.launch {
                                        try {
                                            FreetimeNotifications.setCategoryEnabled(context, category, enabled)
                                            categories = loadedCategories + (category to enabled)
                                        } catch (error: CancellationException) {
                                            throw error
                                        } catch (_: Exception) {
                                            currentOnError(saveError)
                                        } finally {
                                            savingCategory = null
                                        }
                                    }
                                }
                            }
                        )
                        .padding(vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        stringResource(label),
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Switch(
                        checked = checked,
                        enabled = savingCategory == null,
                        onCheckedChange = null
                    )
                }
            }
            if (savingCategory != null) {
                Text(
                    stringResource(Res.string.freetime_notifications_saving),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

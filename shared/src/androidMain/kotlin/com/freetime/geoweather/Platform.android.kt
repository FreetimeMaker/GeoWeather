package com.freetime.geoweather

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

private var androidContext: Context? = null

fun setAndroidContext(context: Context) {
    androidContext = context
}

actual fun openUrl(url: String) {
    androidContext?.let { context ->
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }
}

@Composable
actual fun rememberPaymentContext(): Any? {
    return LocalContext.current as? Activity ?: androidContext
}

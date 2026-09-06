package com.freetime.geoweather

import android.content.Context
import android.content.ClipData
import android.content.Intent
import android.net.Uri
import android.os.Build

class AndroidPlatform(private val context: Context) : Platform {
    override val name: String = "Android ${Build.VERSION.SDK_INT}"
    
    override fun openUrl(url: String) {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }

    override fun copyToClipboard(text: String) {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
        clipboard.setPrimaryClip(ClipData.newPlainText("GeoWeather backup", text))
    }
}

// We need a way to get the context, normally via a provider or initialization
private var appContext: Context? = null

fun initPlatform(context: Context) {
    appContext = context.applicationContext
}

actual fun getPlatform(): Platform = AndroidPlatform(appContext!!)

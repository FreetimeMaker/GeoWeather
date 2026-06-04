package com.freetime.geoweather

import android.content.Context
import android.util.AttributeSet
import android.webkit.WebView

class SimpleWebView(context: Context, attrs: AttributeSet? = null) :
    WebView(context, attrs) {

    init {
        setupBasicSettings()
    }

    private fun setupBasicSettings() {
        settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            allowFileAccess = true
            allowContentAccess = true
        }
    }
}

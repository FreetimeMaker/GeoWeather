package com.freetime.geoweather

import android.appwidget.AppWidgetManager
import android.content.Intent
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.ListView
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.lifecycle.lifecycleScope
import com.freetime.geoweather.data.DependencyManager
import kotlinx.coroutines.launch

class WeatherWidgetConfigureActivity : ComponentActivity() {
    private var appWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setResult(RESULT_CANCELED)
        appWidgetId = intent?.extras?.getInt(
            AppWidgetManager.EXTRA_APPWIDGET_ID,
            AppWidgetManager.INVALID_APPWIDGET_ID
        ) ?: AppWidgetManager.INVALID_APPWIDGET_ID
        if (appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            finish()
            return
        }

        lifecycleScope.launch {
            val locations = DependencyManager.getRepository().getAllLocationsSync()
            if (locations.isEmpty()) {
                val message = TextView(this@WeatherWidgetConfigureActivity).apply {
                    text = getString(R.string.widget_config_empty)
                    textSize = 18f
                    setPadding(48, 48, 48, 48)
                }
                setContentView(message)
                return@launch
            }

            title = getString(R.string.widget_config_title)
            val list = ListView(this@WeatherWidgetConfigureActivity)
            list.adapter = ArrayAdapter(
                this@WeatherWidgetConfigureActivity,
                android.R.layout.simple_list_item_1,
                locations.map { it.name }
            )
            list.setOnItemClickListener { _, _, position, _ ->
                WidgetLocationPreferences.saveLocationId(
                    this@WeatherWidgetConfigureActivity,
                    appWidgetId,
                    locations[position].id
                )
                val result = Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
                setResult(RESULT_OK, result)
                finish()
            }
            setContentView(list)
        }
    }
}

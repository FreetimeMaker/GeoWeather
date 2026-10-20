package com.freetime.geoweather.ui

import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.text.StaticLayout
import android.text.TextPaint
import android.text.Layout
import android.widget.Toast
import androidx.core.content.FileProvider
import com.freetime.geoweather.R
import java.io.File
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

fun createWeatherShareCard(
    title: String,
    temperature: String,
    description: String,
    subtitle: String,
    details: List<String> = emptyList(),
    source: String = "Open-Meteo · GeoWeather"
): Bitmap {
    val width = 1080
    val textWidth = 840
    fun layout(text: String, size: Float, bold: Boolean = false): StaticLayout {
        val paint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = size
            typeface = if (bold) Typeface.DEFAULT_BOLD else Typeface.DEFAULT
            // Fit complete text, including units, without truncation or ellipses.
            val widest = text.lines().maxOfOrNull { measureText(it) } ?: 0f
            if (widest > textWidth) textSize *= textWidth / widest
        }
        return StaticLayout.Builder.obtain(text, 0, text.length, paint, textWidth)
            .setAlignment(Layout.Alignment.ALIGN_CENTER)
            .setIncludePad(true)
            .build()
    }
    val blocks = buildList {
        add(layout(title, 62f, true))
        add(layout(temperature, 190f, true))
        add(layout(description, 58f))
        if (subtitle.isNotBlank()) add(layout(subtitle, 38f))
        details.forEach { add(layout(it, 30f)) }
        add(layout(source, 34f, true))
    }
    val gap = 28
    val height = maxOf(1080, 240 + blocks.sumOf { it.height } + gap * (blocks.size - 1))
    val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    try {
        val canvas = Canvas(bitmap)
        canvas.drawColor(Color.rgb(20, 38, 62))
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.argb(48, 255, 255, 255)
        }
        canvas.drawRoundRect(70f, 70f, 1010f, height - 70f, 72f, 72f, paint)
        var y = (height - blocks.sumOf { it.height } - gap * (blocks.size - 1)) / 2f
        blocks.forEach { block ->
            canvas.save()
            canvas.translate((width - textWidth) / 2f, y)
            block.draw(canvas)
            canvas.restore()
            y += block.height + gap
        }
        return bitmap
    } catch (error: Throwable) {
        bitmap.recycle()
        throw error
    }
}

internal suspend fun sendWeatherShareCard(context: Context, bitmap: Bitmap, shareText: String) {
    val uri = withContext(Dispatchers.IO) {
        val directory = File(context.cacheDir, "shared_weather")
        check(directory.isDirectory || directory.mkdirs()) { "Cannot create share cache" }
        val cutoff = System.currentTimeMillis() - 7L * 24 * 60 * 60 * 1000
        // Only this feature's PNGs expire; recent shares must remain readable by recipients.
        directory.listFiles()?.forEach { file ->
            if (file.isFile && file.name.startsWith("geoweather-") &&
                file.extension == "png" && file.lastModified() < cutoff
            ) file.delete()
        }
        val file = File.createTempFile("geoweather-", ".png", directory)
        try {
            file.outputStream().use { output ->
                check(bitmap.compress(Bitmap.CompressFormat.PNG, 100, output)) { "PNG encoding failed" }
            }
            FileProvider.getUriForFile(context, context.packageName + ".fileprovider", file)
        } catch (error: Throwable) {
            file.delete()
            throw error
        }
    }
    withContext(Dispatchers.Main) {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "image/png"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_TEXT, shareText)
            clipData = ClipData.newRawUri("GeoWeather", uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        val chooser = Intent.createChooser(intent, context.getString(R.string.weather_share_dialog_title)).apply {
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            clipData = intent.clipData
            if (context !is android.app.Activity) addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(chooser)
    }
}

// Compatibility for existing callers; the dialog uses the suspend path to display errors inline.
fun shareWeatherCard(context: Context, bitmap: Bitmap, locationName: String) {
    CoroutineScope(Dispatchers.Main).launch {
        try {
            sendWeatherShareCard(context, bitmap, "$locationName · Open-Meteo · GeoWeather")
        } catch (error: CancellationException) {
            throw error
        } catch (error: Exception) {
            Toast.makeText(context, R.string.weather_share_error, Toast.LENGTH_LONG).show()
        }
    }
}

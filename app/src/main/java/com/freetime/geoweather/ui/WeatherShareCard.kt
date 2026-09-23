package com.freetime.geoweather.ui

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import androidx.core.content.FileProvider
import java.io.File

fun createWeatherShareCard(
    title: String,
    temperature: String,
    description: String,
    subtitle: String,
    details: List<String> = emptyList()
): Bitmap {
    val width = 1080
    val height = 1080
    val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    val paint = Paint(Paint.ANTI_ALIAS_FLAG)

    paint.color = android.graphics.Color.rgb(20, 38, 62)
    canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), paint)

    paint.color = android.graphics.Color.argb(48, 255, 255, 255)
    canvas.drawRoundRect(70f, 70f, 1010f, 1010f, 72f, 72f, paint)

    paint.color = android.graphics.Color.WHITE
    paint.textAlign = Paint.Align.CENTER
    paint.typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD)
    paint.textSize = 62f
    canvas.drawText(title.take(28), width / 2f, 260f, paint)

    paint.textSize = 190f
    canvas.drawText(temperature, width / 2f, 535f, paint)

    paint.textSize = 58f
    canvas.drawText(description.take(34), width / 2f, 675f, paint)

    paint.typeface = android.graphics.Typeface.DEFAULT
    paint.textSize = 38f
    paint.color = android.graphics.Color.argb(220, 255, 255, 255)
    if (subtitle.isNotBlank()) canvas.drawText(subtitle.take(42), width / 2f, 760f, paint)

    paint.textSize = 30f
    details.take(3).forEachIndexed { index, detail ->
        canvas.drawText(detail.take(52), width / 2f, 820f + index * 38f, paint)
    }

    paint.textSize = 34f
    paint.typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD)
    canvas.drawText("GeoWeather", width / 2f, 960f, paint)
    return bitmap
}

fun shareWeatherCard(context: Context, bitmap: Bitmap, locationName: String) {
    val directory = File(context.cacheDir, "shared_weather").apply { mkdirs() }
    val file = File(directory, "geoweather-card.png")
    file.outputStream().use { output ->
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, output)
    }
    val uri = FileProvider.getUriForFile(context, context.packageName + ".fileprovider", file)
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "image/png"
        putExtra(Intent.EXTRA_STREAM, uri)
        putExtra(Intent.EXTRA_TEXT, locationName + " · GeoWeather")
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    context.startActivity(Intent.createChooser(intent, context.getString(com.freetime.geoweather.R.string.share_weather)))
}

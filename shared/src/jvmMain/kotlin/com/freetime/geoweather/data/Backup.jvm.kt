package com.freetime.geoweather.data

import java.awt.FileDialog
import java.awt.Frame
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

actual suspend fun saveTextFile(fileName: String, mimeType: String, content: String): Boolean =
    withContext(Dispatchers.IO) {
        try {
            val dialog = FileDialog(null as Frame?, "GeoWeather", FileDialog.SAVE)
            dialog.file = fileName
            dialog.isVisible = true
            val directory = dialog.directory
            val file = dialog.file
            dialog.dispose()
            if (file == null) return@withContext false
            val target = if (directory != null) File(directory, file) else File(file)
            target.writeText(content)
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

actual suspend fun loadTextFile(mimeTypes: Array<String>): String? =
    withContext(Dispatchers.IO) {
        try {
            val dialog = FileDialog(null as Frame?, "GeoWeather", FileDialog.LOAD)
            dialog.isVisible = true
            val directory = dialog.directory
            val file = dialog.file
            dialog.dispose()
            if (file == null) return@withContext null
            val target = if (directory != null) File(directory, file) else File(file)
            target.readText()
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

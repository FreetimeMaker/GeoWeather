package com.freetime.geoweather.data

import android.net.Uri
import androidx.activity.result.ActivityResultLauncher
import com.freetime.geoweather.getAndroidAppContext
import kotlin.coroutines.Continuation
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

private var createLauncher: ActivityResultLauncher<String>? = null
private var openLauncher: ActivityResultLauncher<Array<String>>? = null
private var createContinuation: Continuation<Boolean>? = null
private var createContent: String? = null
private var openContinuation: Continuation<String?>? = null

/**
 * Called once from MainActivity.onCreate. Launchers must be registered
 * before the Activity is started, so they cannot be created on demand.
 */
fun registerFilePickers(
    createDocument: ActivityResultLauncher<String>,
    openDocument: ActivityResultLauncher<Array<String>>
) {
    createLauncher = createDocument
    openLauncher = openDocument
}

fun onCreateDocumentResult(uri: Uri?) {
    val cont = createContinuation
    createContinuation = null
    val content = createContent
    createContent = null
    if (cont == null) return
    if (uri == null || content == null) {
        cont.resume(false)
        return
    }
    try {
        getAndroidAppContext()?.contentResolver?.openOutputStream(uri)?.use {
            it.write(content.toByteArray())
        }
        cont.resume(true)
    } catch (e: Exception) {
        e.printStackTrace()
        cont.resume(false)
    }
}

fun onOpenDocumentResult(uri: Uri?) {
    val cont = openContinuation
    openContinuation = null
    if (cont == null) return
    if (uri == null) {
        cont.resume(null)
        return
    }
    try {
        val text = getAndroidAppContext()?.contentResolver
            ?.openInputStream(uri)?.bufferedReader()?.readText()
        cont.resume(text)
    } catch (e: Exception) {
        e.printStackTrace()
        cont.resume(null)
    }
}

actual suspend fun saveTextFile(fileName: String, mimeType: String, content: String): Boolean =
    suspendCoroutine { cont ->
        val launcher = createLauncher
        if (launcher == null) {
            cont.resume(false)
            return@suspendCoroutine
        }
        createContinuation = cont
        createContent = content
        launcher.launch(fileName)
    }

actual suspend fun loadTextFile(mimeTypes: Array<String>): String? =
    suspendCoroutine { cont ->
        val launcher = openLauncher
        if (launcher == null) {
            cont.resume(null)
            return@suspendCoroutine
        }
        openContinuation = cont
        launcher.launch(mimeTypes)
    }

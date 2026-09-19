package com.freetime.geoweather

import android.content.Context
import android.os.Build
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import me.free_time.updater.AppVersion
import me.free_time.updater.FreetimeUpdater
import me.free_time.updater.RegisteredUpdateSource
import me.free_time.updater.UpdateInfo
import me.free_time.updater.UpdateSource
import me.free_time.updater.UpdateSourceSelection
import me.free_time.updater.UpdateSourceType

object GeoWeatherUpdateChecker {
    private const val GITHUB_RELEASES = "https://api.github.com/repos/FreetimeMaker/GeoWeather/releases/latest"
    private const val FDROID_PACKAGES = "https://f-droid.org/api/v1/packages/com.freetime.geoweather"
    private const val LUMA_APP = "https://api.free-time.me/v2/store/apps/com.freetime.geoweather"

    private val json = Json { ignoreUnknownKeys = true; isLenient = true }
    private val client = HttpClient()

    suspend fun check(context: Context, source: String): Result<UpdateInfo?> {
        val type = when (source) {
            "luma_store" -> UpdateSourceType.LUMA_STORE
            "github" -> UpdateSourceType.GITHUB
            "f_droid" -> UpdateSourceType.F_DROID
            else -> return Result.success(null)
        }
        val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
        val currentCode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) packageInfo.longVersionCode
        else @Suppress("DEPRECATION") packageInfo.versionCode.toLong()
        val current = AppVersion(packageInfo.versionName ?: "unknown", currentCode)

        val updater = FreetimeUpdater(
            listOf(
                RegisteredUpdateSource(UpdateSourceType.GITHUB, githubSource),
                RegisteredUpdateSource(UpdateSourceType.F_DROID, fdroidSource),
                RegisteredUpdateSource(UpdateSourceType.LUMA_STORE, lumaSource),
            )
        )
        return updater.check(context.packageName, current, UpdateSourceSelection.only(type))
    }

    private val githubSource = UpdateSource {
        val root = json.parseToJsonElement(client.get(GITHUB_RELEASES).bodyAsText()).jsonObject
        val tag = root["tag_name"]?.jsonPrimitive?.contentOrNull?.removePrefix("v") ?: return@UpdateSource null
        val assets = root["assets"]?.jsonArray
        val apk = assets?.firstOrNull { it.jsonObject["name"]?.jsonPrimitive?.contentOrNull?.endsWith(".apk") == true }?.jsonObject
        AppVersion(tag, versionCodeFromName(tag), apk?.get("browser_download_url")?.jsonPrimitive?.contentOrNull, root["body"]?.jsonPrimitive?.contentOrNull)
    }

    private val fdroidSource = UpdateSource {
        val root = json.parseToJsonElement(client.get(FDROID_PACKAGES).bodyAsText()).jsonObject
        val versionName = root["suggestedVersionName"]?.jsonPrimitive?.contentOrNull ?: return@UpdateSource null
        val versionCode = root["suggestedVersionCode"]?.jsonPrimitive?.contentOrNull?.toLongOrNull()
            ?: versionCodeFromName(versionName)
        AppVersion(versionName, versionCode, "https://f-droid.org/packages/com.freetime.geoweather/")
    }

    private val lumaSource = UpdateSource {
        val root = json.parseToJsonElement(client.get(LUMA_APP).bodyAsText()).jsonObject
        val app = root["app"]?.jsonObject ?: root
        val versionName = app["version_name"]?.jsonPrimitive?.contentOrNull
            ?: app["versionName"]?.jsonPrimitive?.contentOrNull
            ?: return@UpdateSource null
        val versionCode = app["version_code"]?.jsonPrimitive?.contentOrNull?.toLongOrNull()
            ?: app["versionCode"]?.jsonPrimitive?.contentOrNull?.toLongOrNull()
            ?: versionCodeFromName(versionName)
        val download = app["download_url"]?.jsonPrimitive?.contentOrNull
            ?: app["downloadUrl"]?.jsonPrimitive?.contentOrNull
        AppVersion(versionName, versionCode, download)
    }

    private fun versionCodeFromName(version: String): Long {
        val parts = version.removePrefix("v").split(".").map { it.filter(Char::isDigit).toLongOrNull() ?: 0L }
        return (parts.getOrElse(0) { 0 } * 1_000_000L) +
            (parts.getOrElse(1) { 0 } * 1_000L) +
            parts.getOrElse(2) { 0 }
    }
}

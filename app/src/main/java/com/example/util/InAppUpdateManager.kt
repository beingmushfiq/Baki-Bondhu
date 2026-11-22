package com.example.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.util.Log
import androidx.core.content.FileProvider
import com.example.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.TimeUnit

data class AppUpdateInfo(
    val latestVersion: String,
    val currentVersion: String,
    val isUpdateAvailable: Boolean,
    val downloadUrl: String?,
    val releaseTitle: String?,
    val releaseNotes: String?
)

class InAppUpdateManager(private val context: Context) {
    private val tag = "InAppUpdateManager"
    private val repoOwner = "beingmushfiq"
    private val repoName = "Baki-Bondhu"

    /**
     * Queries GitHub Releases API for the latest published release.
     */
    suspend fun checkForUpdates(): Result<AppUpdateInfo> = withContext(Dispatchers.IO) {
        try {
            val apiUrl = "https://api.github.com/repos/$repoOwner/$repoName/releases/latest"
            val url = URL(apiUrl)
            val connection = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                setRequestProperty("Accept", "application/vnd.github.v3+json")
                setRequestProperty("User-Agent", "BakiBondhu-App")
                connectTimeout = 10000
                readTimeout = 10000
            }

            if (connection.responseCode != 200) {
                return@withContext Result.failure(
                    IllegalStateException("GitHub API responded with code: ${connection.responseCode}")
                )
            }

            val responseText = connection.inputStream.bufferedReader().use { it.readText() }
            val json = JSONObject(responseText)

            val rawTagName = json.optString("tag_name", "")
            val latestVersion = rawTagName.removePrefix("v").trim()
            val currentVersion = BuildConfig.VERSION_NAME.removePrefix("v").trim()
            val releaseTitle = json.optString("name", "Version $latestVersion")
            val releaseNotes = json.optString("body", "")

            // Find APK asset (prioritize version-specific asset over generic latest)
            var downloadUrl: String? = null
            val assets = json.optJSONArray("assets")
            if (assets != null) {
                val preferredName = "bakibondhu-v$latestVersion.apk"
                for (i in 0 until assets.length()) {
                    val asset = assets.getJSONObject(i)
                    val name = asset.optString("name", "")
                    if (name.equals(preferredName, ignoreCase = true)) {
                        val url = asset.optString("browser_download_url", "")
                        if (url.isNotEmpty()) {
                            downloadUrl = url
                            break
                        }
                    }
                }
                if (downloadUrl == null) {
                    for (i in 0 until assets.length()) {
                        val asset = assets.getJSONObject(i)
                        val name = asset.optString("name", "")
                        if (name.endsWith(".apk", ignoreCase = true)) {
                            val url = asset.optString("browser_download_url", "")
                            if (url.isNotEmpty()) {
                                downloadUrl = url
                                break
                            }
                        }
                    }
                }
            }

            // Fallback to release HTML URL if APK not found in assets
            if (downloadUrl == null) {
                downloadUrl = json.optString("html_url", "https://github.com/$repoOwner/$repoName/releases")
            }

            val isNewer = isVersionNewer(latestVersion, currentVersion)

            Result.success(
                AppUpdateInfo(
                    latestVersion = latestVersion,
                    currentVersion = currentVersion,
                    isUpdateAvailable = isNewer,
                    downloadUrl = downloadUrl,
                    releaseTitle = releaseTitle,
                    releaseNotes = releaseNotes
                )
            )
        } catch (e: Exception) {
            Log.e(tag, "Failed to check for updates: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Downloads the APK file to app cache and triggers Android package installer.
     * Uses OkHttpClient with followRedirects=true to reliably download from GitHub/AWS S3.
     */
    suspend fun downloadAndInstallApk(
        apkUrl: String,
        onProgress: (Int) -> Unit = {}
    ): Result<File> = withContext(Dispatchers.IO) {
        try {
            // If the URL is not a direct APK asset (e.g. fallback release page), open browser directly
            if (!apkUrl.endsWith(".apk", ignoreCase = true)) {
                withContext(Dispatchers.Main) {
                    val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(apkUrl)).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    }
                    context.startActivity(browserIntent)
                }
                return@withContext Result.failure(IllegalStateException("No direct APK asset; opening release page."))
            }

            val client = OkHttpClient.Builder()
                .followRedirects(true)
                .followSslRedirects(true)
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(90, TimeUnit.SECONDS)
                .build()

            val request = Request.Builder()
                .url(apkUrl)
                .header("User-Agent", "BakiBondhu-App")
                .header("Accept", "application/octet-stream")
                .build()

            val response = client.newCall(request).execute()
            if (!response.isSuccessful) {
                return@withContext Result.failure(IllegalStateException("Server responded with ${response.code}"))
            }

            val responseBody = response.body ?: return@withContext Result.failure(IllegalStateException("Empty response body"))
            val fileLength = responseBody.contentLength()
            val cacheDir = File(context.getExternalFilesDir(null) ?: context.cacheDir, "updates")
            if (!cacheDir.exists()) cacheDir.mkdirs()

            val apkFile = File(cacheDir, "BakiBondhu_update.apk")
            if (apkFile.exists()) apkFile.delete()

            responseBody.byteStream().use { input ->
                FileOutputStream(apkFile).use { output ->
                    val data = ByteArray(8192)
                    var total: Long = 0
                    var count: Int
                    while (input.read(data).also { count = it } != -1) {
                        total += count
                        if (fileLength > 0) {
                            val percent = ((total * 100) / fileLength).toInt()
                            onProgress(percent.coerceIn(0, 100))
                        }
                        output.write(data, 0, count)
                    }
                    output.flush()
                }
            }

            // Verify downloaded APK size is valid (> 500 KB)
            if (apkFile.length() < 500_000L) {
                apkFile.delete()
                return@withContext Result.failure(IllegalStateException("Downloaded update package is incomplete (${apkFile.length()} bytes)."))
            }

            // Launch package installer
            withContext(Dispatchers.Main) {
                installApk(apkFile)
            }

            Result.success(apkFile)
        } catch (e: Exception) {
            Log.e(tag, "Download failed: ${e.message}", e)
            Result.failure(e)
        }
    }

    fun installApk(file: File) {
        try {
            val uri: Uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )

            val installIntent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "application/vnd.android.package-archive")
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION
            }
            context.startActivity(installIntent)
        } catch (e: Exception) {
            Log.e(tag, "Failed to launch installer: ${e.message}", e)
            // Fallback: open release page in browser
            try {
                val browserIntent = Intent(
                    Intent.ACTION_VIEW,
                    Uri.parse("https://github.com/$repoOwner/$repoName/releases/latest")
                ).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                context.startActivity(browserIntent)
            } catch (_: Exception) {}
        }
    }

    private fun isVersionNewer(latest: String, current: String): Boolean {
        try {
            val latestParts = latest.split(".").map { it.filter { c -> c.isDigit() }.toIntOrNull() ?: 0 }
            val currentParts = current.split(".").map { it.filter { c -> c.isDigit() }.toIntOrNull() ?: 0 }

            val maxLen = maxOf(latestParts.size, currentParts.size)
            for (i in 0 until maxLen) {
                val l = latestParts.getOrElse(i) { 0 }
                val c = currentParts.getOrElse(i) { 0 }
                if (l > c) return true
                if (l < c) return false
            }
            return false
        } catch (_: Exception) {
            return latest != current && latest.isNotBlank()
        }
    }
}

package com.soumit.technovision.data.repository

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.pm.PackageInfoCompat
import com.soumit.technovision.data.model.AppUpdateInfo
import com.soumit.technovision.data.model.DownloadStatus
import com.soumit.technovision.data.model.GitHubReleaseDto
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL
import javax.inject.Inject
import javax.inject.Singleton

interface AppUpdateRepository {
    val latestUpdateInfo: StateFlow<AppUpdateInfo?>
    val downloadStatus: StateFlow<DownloadStatus>
    suspend fun checkForUpdate(): Result<AppUpdateInfo?>
    fun downloadApk(downloadUrl: String, apkName: String): Flow<DownloadStatus>
}

@Singleton
class GitHubAppUpdateRepository @Inject constructor(
    @ApplicationContext private val context: Context
) : AppUpdateRepository {

    private val _latestUpdateInfo = MutableStateFlow<AppUpdateInfo?>(null)
    override val latestUpdateInfo: StateFlow<AppUpdateInfo?> = _latestUpdateInfo.asStateFlow()

    private val _downloadStatus = MutableStateFlow<DownloadStatus>(DownloadStatus.Idle)
    override val downloadStatus: StateFlow<DownloadStatus> = _downloadStatus.asStateFlow()

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    private val githubReleasesUrl = "https://api.github.com/repos/IamSoumitDas/TechnoVision/releases/latest"

    override suspend fun checkForUpdate(): Result<AppUpdateInfo?> = withContext(Dispatchers.IO) {
        runCatching {
            val url = URL(githubReleasesUrl)
            val connection = (url.openConnection() as HttpURLConnection).apply {
                connectTimeout = 10_000
                readTimeout = 15_000
                setRequestProperty("Accept", "application/vnd.github.v3+json")
                setRequestProperty("User-Agent", "TechnoVision-App")
            }

            try {
                val responseCode = connection.responseCode
                if (responseCode == HttpURLConnection.HTTP_NOT_FOUND) {
                    // No releases published yet
                    return@runCatching null
                }
                if (responseCode !in 200..299) {
                    throw IllegalStateException("GitHub API returned HTTP $responseCode")
                }

                val responseBody = connection.inputStream.bufferedReader().use { it.readText() }
                val release = json.decodeFromString<GitHubReleaseDto>(responseBody)

                val packageInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    context.packageManager.getPackageInfo(context.packageName, PackageManager.PackageInfoFlags.of(0))
                } else {
                    @Suppress("DEPRECATION")
                    context.packageManager.getPackageInfo(context.packageName, 0)
                }

                val currentVersionName = packageInfo.versionName ?: "1.0.0"
                val currentVersionCode = PackageInfoCompat.getLongVersionCode(packageInfo)

                val isNewer = isNewerVersion(
                    remoteTag = release.tagName,
                    currentVersionName = currentVersionName,
                    currentVersionCode = currentVersionCode
                )

                if (!isNewer) return@runCatching null

                val apkAsset = release.assets.firstOrNull { it.name.endsWith(".apk", ignoreCase = true) }
                    ?: return@runCatching null

                val updateInfo = AppUpdateInfo(
                    versionName = release.tagName.removePrefix("v").removePrefix("V"),
                    changelog = release.body.orEmpty().ifBlank { "Performance improvements and bug fixes." },
                    downloadUrl = apkAsset.downloadUrl,
                    apkName = apkAsset.name,
                    sizeBytes = apkAsset.size
                )
                _latestUpdateInfo.value = updateInfo
                updateInfo
            } finally {
                connection.disconnect()
            }
        }
    }

    override fun downloadApk(downloadUrl: String, apkName: String): Flow<DownloadStatus> = flow {
        val initialStatus = DownloadStatus.Progress(downloadedBytes = 0L, totalBytes = -1L, progress = 0f)
        _downloadStatus.value = initialStatus
        emit(initialStatus)

        val updatesDir = File(context.cacheDir, "updates")
        if (!updatesDir.exists()) {
            updatesDir.mkdirs()
        }
        updatesDir.listFiles()?.forEach { file ->
            if (file.isFile && file.name.endsWith(".apk")) {
                file.delete()
            }
        }

        val outputFile = File(updatesDir, apkName)
        val tempFile = File(updatesDir, "$apkName.tmp")
        if (tempFile.exists()) tempFile.delete()

        var currentUrl = downloadUrl
        var connection: HttpURLConnection? = null
        var redirects = 0
        val maxRedirects = 5

        try {
            while (redirects < maxRedirects) {
                val url = URL(currentUrl)
                val conn = (url.openConnection() as HttpURLConnection).apply {
                    connectTimeout = 15_000
                    readTimeout = 30_000
                    instanceFollowRedirects = true
                    setRequestProperty("User-Agent", "TechnoVision-App")
                }
                val responseCode = conn.responseCode
                if (responseCode == HttpURLConnection.HTTP_MOVED_PERM ||
                    responseCode == HttpURLConnection.HTTP_MOVED_TEMP ||
                    responseCode == 307 || responseCode == 308
                ) {
                    val newUrl = conn.getHeaderField("Location")
                    conn.disconnect()
                    if (newUrl != null) {
                        currentUrl = newUrl
                        redirects++
                        continue
                    }
                }
                connection = conn
                break
            }

            val conn = connection ?: throw IllegalStateException("Failed to establish connection")
            if (conn.responseCode !in 200..299) {
                throw IllegalStateException("HTTP ${conn.responseCode}: ${conn.responseMessage}")
            }

            val totalBytes = conn.contentLengthLong
            var downloadedBytes = 0L

            conn.inputStream.use { input ->
                FileOutputStream(tempFile).use { output ->
                    val buffer = ByteArray(8192)
                    var bytesRead: Int
                    var lastEmittedTime = 0L

                    while (input.read(buffer).also { bytesRead = it } != -1) {
                        output.write(buffer, 0, bytesRead)
                        downloadedBytes += bytesRead

                        val now = System.currentTimeMillis()
                        if (now - lastEmittedTime > 100 || downloadedBytes == totalBytes) {
                            val progress = if (totalBytes > 0) downloadedBytes.toFloat() / totalBytes else 0f
                            val status = DownloadStatus.Progress(downloadedBytes, totalBytes, progress)
                            _downloadStatus.value = status
                            emit(status)
                            lastEmittedTime = now
                        }
                    }
                    output.flush()
                }
            }

            if (outputFile.exists()) outputFile.delete()
            if (!tempFile.renameTo(outputFile)) {
                tempFile.copyTo(outputFile, overwrite = true)
                tempFile.delete()
            }

            val completed = DownloadStatus.Completed(outputFile)
            _downloadStatus.value = completed
            emit(completed)
        } catch (e: Exception) {
            if (tempFile.exists()) tempFile.delete()
            val failed = DownloadStatus.Failed(e.message ?: "Failed to download update")
            _downloadStatus.value = failed
            emit(failed)
        } finally {
            connection?.disconnect()
        }
    }.flowOn(Dispatchers.IO)

    internal fun isNewerVersion(
        remoteTag: String,
        currentVersionName: String,
        currentVersionCode: Long
    ): Boolean {
        val cleanRemote = remoteTag.trim().removePrefix("v").removePrefix("V")
        val cleanCurrent = currentVersionName.trim().removePrefix("v").removePrefix("V")

        if (cleanRemote.equals(cleanCurrent, ignoreCase = true)) return false

        val remoteNumeric = cleanRemote.toLongOrNull()
        if (remoteNumeric != null && currentVersionCode > 0) {
            return remoteNumeric > currentVersionCode
        }

        val remoteParts = cleanRemote.split(".", "-", "_").mapNotNull { it.toIntOrNull() }
        val currentParts = cleanCurrent.split(".", "-", "_").mapNotNull { it.toIntOrNull() }

        if (remoteParts.isNotEmpty() && currentParts.isNotEmpty()) {
            val maxLen = maxOf(remoteParts.size, currentParts.size)
            for (i in 0 until maxLen) {
                val r = remoteParts.getOrElse(i) { 0 }
                val c = currentParts.getOrElse(i) { 0 }
                if (r > c) return true
                if (r < c) return false
            }
            return false
        }

        return cleanRemote.compareTo(cleanCurrent) > 0
    }
}

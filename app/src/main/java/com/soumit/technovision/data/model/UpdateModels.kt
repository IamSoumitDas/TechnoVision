package com.soumit.technovision.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.io.File

@Serializable
data class GitHubReleaseDto(
    @SerialName("tag_name") val tagName: String,
    val name: String? = null,
    val body: String? = null,
    val assets: List<GitHubAssetDto> = emptyList()
)

@Serializable
data class GitHubAssetDto(
    val name: String,
    @SerialName("browser_download_url") val downloadUrl: String,
    val size: Long = 0L,
    @SerialName("content_type") val contentType: String? = null
)

data class AppUpdateInfo(
    val versionName: String,
    val changelog: String,
    val downloadUrl: String,
    val apkName: String,
    val sizeBytes: Long
)

sealed interface DownloadStatus {
    data object Idle : DownloadStatus
    data class Progress(val downloadedBytes: Long, val totalBytes: Long, val progress: Float) : DownloadStatus
    data class Completed(val apkFile: File) : DownloadStatus
    data class Failed(val error: String) : DownloadStatus
}

package com.hairstyle.generator.aimakerover.utils

import android.content.Context
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.ConcurrentHashMap

object VideoCacheManager {
    private const val CACHE_DIR_NAME = "video_cache"
    private val downloadLocks = ConcurrentHashMap<String, Any>()

    fun resolveCacheExtension(url: String): String {
        val path = url.substringBefore('?').substringAfterLast('/').lowercase()
        val ext = path.substringAfterLast('.', missingDelimiterValue = "")
        return if (ext.isNotBlank() && ext.length in 2..5 && '/' !in ext) {
            ".$ext"
        } else {
            ".mp4"
        }
    }

    /**
     * Gets the local cached media file if it exists and is not empty.
     */
    fun getCachedMediaFile(
        context: Context,
        videoTitleKey: String,
        sourceUrl: String? = null
    ): File? {
        val cacheDir = FileUtils.getCacheDir(context, CACHE_DIR_NAME)
        if (!sourceUrl.isNullOrBlank()) {
            val preferred = File(cacheDir, "$videoTitleKey${resolveCacheExtension(sourceUrl)}")
            return preferred.takeIf { it.exists() && it.length() > 0 }
        }
        val extensions = listOf(".mp4", ".webp", ".gif", ".webm", ".mov", ".m4v")
        return extensions.firstNotNullOfOrNull { ext ->
            File(cacheDir, "$videoTitleKey$ext").takeIf { it.exists() && it.length() > 0 }
        }
    }

    /**
     * Gets the local cached video file if it exists and is not empty.
     */
    fun getCachedVideoFile(context: Context, videoTitleKey: String): File? {
        return getCachedMediaFile(context, videoTitleKey)
    }

    /**
     * Downloads and caches the media to a local file.
     * Skips if already on disk. One download per title at a time.
     */
    fun cacheVideo(
        context: Context,
        okHttpClient: OkHttpClient,
        videoTitleKey: String,
        url: String
    ): File? {
        if (videoTitleKey.isBlank() || url.isBlank()) return null
        val lock = downloadLocks.getOrPut(videoTitleKey) { Any() }
        synchronized(lock) {
            return cacheVideoLocked(context, okHttpClient, videoTitleKey, url)
        }
    }

    private fun cacheVideoLocked(
        context: Context,
        okHttpClient: OkHttpClient,
        videoTitleKey: String,
        url: String
    ): File? {
        val cacheDir = FileUtils.getCacheDir(context, CACHE_DIR_NAME)
        val extension = resolveCacheExtension(url)
        val tempFile = File(cacheDir, "${videoTitleKey}.tmp")
        val destFile = File(cacheDir, "$videoTitleKey$extension")

        if (destFile.exists() && destFile.length() > 0) {
            return destFile
        }

        try {
            val request = Request.Builder().url(url).build()
            val response = okHttpClient.newCall(request).execute()
            if (!response.isSuccessful) {
                return null
            }

            response.body?.byteStream()?.use { inputStream ->
                FileOutputStream(tempFile).use { outputStream ->
                    inputStream.copyTo(outputStream)
                }
            }

            if (tempFile.exists() && tempFile.length() > 0) {
                if (tempFile.renameTo(destFile)) {
                    return destFile
                }
            }
        } catch (_: Exception) {
            if (tempFile.exists()) tempFile.delete()
        }
        return null
    }
}

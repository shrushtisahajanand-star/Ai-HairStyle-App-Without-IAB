package com.hairstyle.generator.aimakerover.utils

import android.content.Context
import android.util.Log
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream

object VideoCacheManager {
    private const val TAG = "VideoCacheManager"
    private const val CACHE_DIR_NAME = "video_cache"

    fun resolveCacheExtension(url: String): String {
        return if (SplashAnimationHelper.isWebpSource(url)) ".webp" else ".mp4"
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
        val extensions = buildList {
            sourceUrl?.let { add(resolveCacheExtension(it)) }
            add(".webp")
            add(".mp4")
        }.distinct()
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
     */
    fun cacheVideo(
        context: Context,
        okHttpClient: OkHttpClient,
        videoTitleKey: String,
        url: String
    ): File? {
        if (videoTitleKey.isBlank() || url.isBlank()) return null
        val cacheDir = FileUtils.getCacheDir(context, CACHE_DIR_NAME)
        val extension = resolveCacheExtension(url)
        val tempFile = File(cacheDir, "${videoTitleKey}.tmp")
        val destFile = File(cacheDir, "$videoTitleKey$extension")

        if (destFile.exists() && destFile.length() > 0) {
            // Log.d(TAG, "Media $videoTitleKey is already cached.")
            return destFile
        }

        try {
            // Log.d(TAG, "Starting cache download for $videoTitleKey: $url")
            val request = Request.Builder().url(url).build()
            val response = okHttpClient.newCall(request).execute()
            if (!response.isSuccessful) {
                // Log.e(TAG, "Failed to download media $videoTitleKey, response code: ${response.code}")
                return null
            }

            response.body?.byteStream()?.use { inputStream ->
                FileOutputStream(tempFile).use { outputStream ->
                    inputStream.copyTo(outputStream)
                }
            }

            if (tempFile.exists() && tempFile.length() > 0) {
                if (tempFile.renameTo(destFile)) {
                    // Log.d(TAG, "Successfully cached media $videoTitleKey")
                    return destFile
                } else {
                    // Log.e(TAG, "Failed to rename temp file for $videoTitleKey")
                }
            }
        } catch (e: Exception) {
            // Log.e(TAG, "Error caching media $videoTitleKey", e)
            if (tempFile.exists()) tempFile.delete()
        }
        return null
    }
}

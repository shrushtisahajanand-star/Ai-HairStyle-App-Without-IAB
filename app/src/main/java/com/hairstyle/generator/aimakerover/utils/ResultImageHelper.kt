package com.hairstyle.generator.aimakerover.utils

import android.app.Activity
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.core.content.FileProvider
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Downloads, caches, saves to gallery, and shares the AI result image.
 */
@Singleton
class ResultImageHelper @Inject constructor(
    @ApplicationContext private val context: Context,
    private val okHttpClient: OkHttpClient
) {

    private val cacheDir: File
        get() = File(context.cacheDir, "results").apply { if (!exists()) mkdirs() }

    /**
     * Returns a local file for the result image.
     * Uses cached path if valid, otherwise downloads from [imageUrl].
     */
    suspend fun ensureLocalFile(imageUrl: String?, existingPath: String?): File =
        withContext(Dispatchers.IO) {
            val cached = existingPath?.let { File(it) }
            if (cached != null && cached.exists() && cached.length() > 0L) {
                return@withContext cached
            }

            val url = imageUrl?.takeIf { it.isNotBlank() }
                ?: throw IOException("No result image available")

            downloadToCache(url)
        }

    suspend fun downloadToCache(imageUrl: String): File = withContext(Dispatchers.IO) {
        val request = Request.Builder()
            .url(imageUrl)
            .header("User-Agent", "AIHairstyleStudio/1.0")
            .build()

        val response = okHttpClient.newCall(request).execute()
        if (!response.isSuccessful) {
            throw IOException("Failed to download image (${response.code})")
        }

        val bytes = response.body?.bytes()
            ?: throw IOException("Empty image response")

        if (bytes.isEmpty()) {
            throw IOException("Downloaded image is empty")
        }

        val extension = imageUrl.substringAfterLast('.')
            .substringBefore('?')
            .lowercase()
            .takeIf { it in SUPPORTED_IMAGE_EXTENSIONS }
            ?: "png"

        val file = File(cacheDir, "hairstyle_result_${System.currentTimeMillis()}.$extension")
        file.writeBytes(bytes)

        decodeResultBitmap(file, bytes)
            ?: throw IOException("Cannot decode downloaded image")

        file
    }

    /**
     * Decodes [file]. Applies EXIF only when still present (e.g. raw camera files).
     * Compressed JPEGs from [ImageCompressor] are already upright with EXIF stripped.
     */
    fun decodeResultBitmap(file: File, fallbackBytes: ByteArray? = null): Bitmap? {
        val decoded = BitmapFactory.decodeFile(file.absolutePath)
            ?: fallbackBytes?.takeIf { it.isNotEmpty() }?.let {
                BitmapFactory.decodeByteArray(it, 0, it.size)
            }
            ?: return null
        return applyExifOrientation(decoded, file)
    }

    private fun applyExifOrientation(bitmap: Bitmap, file: File): Bitmap {
        val orientation = runCatching {
            ExifInterface(file.absolutePath).getAttributeInt(
                ExifInterface.TAG_ORIENTATION,
                ExifInterface.ORIENTATION_NORMAL
            )
        }.getOrDefault(ExifInterface.ORIENTATION_NORMAL)

        val matrix = Matrix()
        when (orientation) {
            ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
            ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
            ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
            ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> matrix.preScale(-1f, 1f)
            ExifInterface.ORIENTATION_FLIP_VERTICAL -> matrix.preScale(1f, -1f)
            ExifInterface.ORIENTATION_TRANSPOSE -> {
                matrix.postRotate(90f)
                matrix.preScale(-1f, 1f)
            }
            ExifInterface.ORIENTATION_TRANSVERSE -> {
                matrix.postRotate(270f)
                matrix.preScale(-1f, 1f)
            }
            else -> return bitmap
        }

        val oriented = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
        if (oriented !== bitmap) {
            bitmap.recycle()
        }
        return oriented
    }

    /**
     * Saves image to Pictures/AI Hairstyle via MediaStore.
     * Free users get [ResultWatermarkHelper] baked into the JPEG; premium saves clean.
     */
    suspend fun saveToGallery(sourceFile: File, highQuality: Boolean = false): Uri =
        withContext(Dispatchers.IO) {
            if (!sourceFile.exists() || sourceFile.length() == 0L) {
                throw IOException("Image file not found")
            }

            val decoded = decodeResultBitmap(sourceFile)
                ?: throw IOException("Cannot read image file")

            val bitmap = ResultWatermarkHelper.applyIfNeeded(context, decoded)
            val quality = if (highQuality) 100 else 92
            val displayName = "AI_Hairstyle_${System.currentTimeMillis()}.jpg"

            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    saveViaMediaStore(bitmap, displayName, quality)
                } else {
                    saveViaLegacyStorage(bitmap, displayName, quality)
                }
            } finally {
                if (bitmap !== decoded) {
                    bitmap.recycle()
                }
                decoded.recycle()
            }
        }

    /**
     * Shares the result image. Free users share a watermarked temp copy.
     */
    suspend fun shareImage(sourceFile: File, launchContext: Context = context) =
        withContext(Dispatchers.IO) {
            if (!sourceFile.exists() || sourceFile.length() == 0L) {
                throw IOException("Image file not found")
            }

            val fileToShare = if (ResultWatermarkHelper.shouldShowWatermark()) {
                val decoded = decodeResultBitmap(sourceFile)
                    ?: throw IOException("Cannot read image file")
                val watermarked = ResultWatermarkHelper.applyIfNeeded(context, decoded)
                try {
                    val out = File(cacheDir, "share_watermarked_${System.currentTimeMillis()}.jpg")
                    FileOutputStream(out).use { stream ->
                        if (!watermarked.compress(Bitmap.CompressFormat.JPEG, 92, stream)) {
                            throw IOException("Failed to compress share image")
                        }
                    }
                    out
                } finally {
                    if (watermarked !== decoded) watermarked.recycle()
                    decoded.recycle()
                }
            } else {
                sourceFile
            }

            withContext(Dispatchers.Main) {
                launchShareIntent(fileToShare, launchContext)
            }
        }

    private fun launchShareIntent(sourceFile: File, launchContext: Context) {
        val uri = FileProvider.getUriForFile(
            launchContext,
            "${launchContext.packageName}.fileprovider",
            sourceFile
        )

        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "image/*"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        val chooser = Intent.createChooser(shareIntent, "Share hairstyle via")
        if (launchContext !is Activity) {
            chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        launchContext.startActivity(chooser)
    }

    private fun saveViaMediaStore(bitmap: Bitmap, displayName: String, quality: Int): Uri {
        val contentValues = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, displayName)
            put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
            put(
                MediaStore.Images.Media.RELATIVE_PATH,
                "${Environment.DIRECTORY_PICTURES}/AI Hairstyle"
            )
            put(MediaStore.Images.Media.IS_PENDING, 1)
        }

        val resolver = context.contentResolver
        val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
            ?: throw IOException("Could not create gallery entry")

        resolver.openOutputStream(uri)?.use { out ->
            if (!bitmap.compress(Bitmap.CompressFormat.JPEG, quality, out)) {
                throw IOException("Failed to compress image")
            }
        } ?: throw IOException("Could not open output stream")

        contentValues.clear()
        contentValues.put(MediaStore.Images.Media.IS_PENDING, 0)
        resolver.update(uri, contentValues, null, null)
        return uri
    }

    @Suppress("DEPRECATION")
    private fun saveViaLegacyStorage(bitmap: Bitmap, displayName: String, quality: Int): Uri {
        val picturesDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
        val appDir = File(picturesDir, "AI Hairstyle")
        if (!appDir.exists()) appDir.mkdirs()

        val file = File(appDir, displayName)
        FileOutputStream(file).use { out ->
            if (!bitmap.compress(Bitmap.CompressFormat.JPEG, quality, out)) {
                throw IOException("Failed to compress image")
            }
        }
        return Uri.fromFile(file)
    }

    companion object {
        private val SUPPORTED_IMAGE_EXTENSIONS = setOf("png", "jpg", "jpeg", "webp")
    }
}

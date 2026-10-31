package com.hairstyle.changer.aitryon.utils

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ImageSaveHelper @Inject constructor(
    @ApplicationContext private val context: Context
) {

    suspend fun saveToGallery(
        sourcePath: String,
        fileName: String,
        highQuality: Boolean = false
    ): Uri = withContext(Dispatchers.IO) {
        val bitmap = BitmapFactory.decodeFile(sourcePath)
            ?: throw IllegalArgumentException("Cannot decode image")

        val quality = if (highQuality) 100 else 92
        val displayName = "${fileName}_${System.currentTimeMillis()}.jpg"

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val contentValues = ContentValues().apply {
                put(MediaStore.Images.Media.DISPLAY_NAME, displayName)
                put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
                put(
                    MediaStore.Images.Media.RELATIVE_PATH,
                    "${Environment.DIRECTORY_PICTURES}/HairGlow"
                )
                put(MediaStore.Images.Media.IS_PENDING, 1)
            }

            val resolver = context.contentResolver
            val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
                ?: throw IllegalStateException("Failed to create media entry")

            resolver.openOutputStream(uri)?.use { out ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, quality, out)
            }

            contentValues.clear()
            contentValues.put(MediaStore.Images.Media.IS_PENDING, 0)
            resolver.update(uri, contentValues, null, null)
            bitmap.recycle()
            uri
        } else {
            @Suppress("DEPRECATION")
            val picturesDir = Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_PICTURES
            )
            val appDir = File(picturesDir, "HairGlow")
            if (!appDir.exists()) appDir.mkdirs()

            val file = File(appDir, displayName)
            FileOutputStream(file).use { out ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, quality, out)
            }
            bitmap.recycle()

            Uri.fromFile(file)
        }
    }

    suspend fun cacheImageFromUrl(
        cacheDir: File,
        imageBytes: ByteArray
    ): File = withContext(Dispatchers.IO) {
        if (!cacheDir.exists()) cacheDir.mkdirs()
        val file = File(cacheDir, "result_${System.currentTimeMillis()}.jpg")
        file.writeBytes(imageBytes)
        file
    }

    suspend fun cacheImageFromPath(
        cacheDir: File,
        sourcePath: String
    ): File = withContext(Dispatchers.IO) {
        val source = File(sourcePath)
        if (!cacheDir.exists()) cacheDir.mkdirs()
        val dest = File(cacheDir, "result_${System.currentTimeMillis()}.jpg")
        source.copyTo(dest, overwrite = true)
        dest
    }
}

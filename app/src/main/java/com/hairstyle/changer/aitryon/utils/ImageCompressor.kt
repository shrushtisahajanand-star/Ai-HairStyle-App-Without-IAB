package com.hairstyle.changer.aitryon.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream

/**
 * Compresses images for generate API + Before/After.
 * Always bakes EXIF orientation into pixels so the saved JPEG is upright
 * (no sideways camera photos in Result slider / API upload).
 */
object ImageCompressor {

    private const val MAX_DIMENSION = 1024
    private const val JPEG_QUALITY = 85

    suspend fun compress(
        context: Context,
        sourceUri: Uri,
        outputDir: File
    ): File = withContext(Dispatchers.IO) {
        val original = context.contentResolver.openInputStream(sourceUri)?.use { stream ->
            BitmapFactory.decodeStream(stream)
        } ?: throw IllegalArgumentException("Cannot read image")

        val oriented = applyExifOrientation(original, readOrientationFromUri(context, sourceUri))
        if (oriented !== original) {
            original.recycle()
        }

        val scaled = scaleDown(oriented)
        if (scaled !== oriented) {
            oriented.recycle()
        }

        writeJpeg(scaled, outputDir).also { scaled.recycle() }
    }

    suspend fun compressFile(sourceFile: File, outputDir: File): File = withContext(Dispatchers.IO) {
        val original = BitmapFactory.decodeFile(sourceFile.absolutePath)
            ?: throw IllegalArgumentException("Invalid image file")

        val oriented = applyExifOrientation(original, readOrientationFromFile(sourceFile))
        if (oriented !== original) {
            original.recycle()
        }

        val scaled = scaleDown(oriented)
        if (scaled !== oriented) {
            oriented.recycle()
        }

        writeJpeg(scaled, outputDir).also { scaled.recycle() }
    }

    private fun writeJpeg(bitmap: Bitmap, outputDir: File): File {
        if (!outputDir.exists()) outputDir.mkdirs()
        val outputFile = File(outputDir, "compressed_${System.currentTimeMillis()}.jpg")
        FileOutputStream(outputFile).use { out ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, out)
        }
        return outputFile
    }

    private fun scaleDown(bitmap: Bitmap): Bitmap {
        val width = bitmap.width
        val height = bitmap.height
        val maxSide = maxOf(width, height)

        if (maxSide <= MAX_DIMENSION) return bitmap

        val ratio = MAX_DIMENSION.toFloat() / maxSide
        val newWidth = (width * ratio).toInt()
        val newHeight = (height * ratio).toInt()

        return Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true)
    }

    private fun readOrientationFromUri(context: Context, uri: Uri): Int {
        return runCatching {
            context.contentResolver.openInputStream(uri)?.use { stream ->
                readOrientation(stream)
            } ?: ExifInterface.ORIENTATION_NORMAL
        }.getOrDefault(ExifInterface.ORIENTATION_NORMAL)
    }

    private fun readOrientationFromFile(file: File): Int {
        return runCatching {
            ExifInterface(file.absolutePath).getAttributeInt(
                ExifInterface.TAG_ORIENTATION,
                ExifInterface.ORIENTATION_NORMAL
            )
        }.getOrDefault(ExifInterface.ORIENTATION_NORMAL)
    }

    private fun readOrientation(stream: InputStream): Int {
        return ExifInterface(stream).getAttributeInt(
            ExifInterface.TAG_ORIENTATION,
            ExifInterface.ORIENTATION_NORMAL
        )
    }

    private fun applyExifOrientation(bitmap: Bitmap, orientation: Int): Bitmap {
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
        return if (oriented !== bitmap) oriented else bitmap
    }
}

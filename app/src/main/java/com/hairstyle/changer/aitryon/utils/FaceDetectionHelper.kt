package com.hairstyle.changer.aitryon.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.graphics.Rect
import android.media.ExifInterface
import android.net.Uri
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.Face
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetector
import com.google.mlkit.vision.face.FaceDetectorOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlin.coroutines.resume
import kotlin.math.max

object FaceDetectionHelper {

    /** Face clearly visible na hoy (vadhu nano) tyare j reject. */
    private const val MIN_FACE_SIZE_RATIO = 0.08f

    /** Khali clearly blur hoy tyare j reject. */
    private const val MIN_LAPLACIAN_VARIANCE = 22.0
    private const val FACE_ANALYSIS_SIZE = 128
    /**
     * ML Kit runs on this bitmap — never feed the full camera JPEG via fromFilePath
     * (that was the main reason detection felt slow).
     */
    private const val MAX_DECODE_SIZE = 960

    private val detector: FaceDetector by lazy {
        FaceDetection.getClient(
            FaceDetectorOptions.Builder()
                .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_FAST)
                .setMinFaceSize(MIN_FACE_SIZE_RATIO)
                .build()
        )
    }

    suspend fun hasDetectableFace(context: Context, uri: Uri): Boolean =
        withContext(Dispatchers.Default) {
            val appContext = context.applicationContext
            // One downsampled, upright bitmap → ML Kit + blur (no full-res fromFilePath).
            val bitmap = decodeUprightBitmap(appContext, uri) ?: return@withContext false
            try {
                val inputImage = InputImage.fromBitmap(bitmap, 0)
                detectValidFace(inputImage, bitmap)
            } finally {
                bitmap.recycle()
            }
        }

    private suspend fun detectValidFace(inputImage: InputImage, bitmap: Bitmap): Boolean =
        suspendCancellableCoroutine { continuation ->
            detector.process(inputImage)
                .addOnSuccessListener { faces ->
                    val valid = faces
                        .maxByOrNull { it.boundingBox.width() * it.boundingBox.height() }
                        ?.let { face -> isValidFace(bitmap, face) }
                        ?: false
                    if (continuation.isActive) continuation.resume(valid)
                }
                .addOnFailureListener {
                    if (continuation.isActive) continuation.resume(false)
                }
        }

    private fun isValidFace(bitmap: Bitmap, face: Face): Boolean {
        if (!isFaceVisibleEnough(bitmap, face.boundingBox)) return false
        if (isVeryBlurry(bitmap, face.boundingBox)) return false
        return true
    }

    private fun isFaceVisibleEnough(bitmap: Bitmap, bounds: Rect): Boolean {
        if (bitmap.width <= 0 || bitmap.height <= 0) return false

        val widthRatio = bounds.width().toFloat() / bitmap.width.toFloat()
        val heightRatio = bounds.height().toFloat() / bitmap.height.toFloat()
        return max(widthRatio, heightRatio) >= MIN_FACE_SIZE_RATIO
    }

    private fun isVeryBlurry(bitmap: Bitmap, bounds: Rect): Boolean {
        val faceBitmap = cropFaceRegion(bitmap, bounds) ?: return false
        return try {
            val analysisBitmap = Bitmap.createScaledBitmap(
                faceBitmap,
                FACE_ANALYSIS_SIZE,
                FACE_ANALYSIS_SIZE,
                true
            )
            try {
                calculateLaplacianVariance(analysisBitmap) < MIN_LAPLACIAN_VARIANCE
            } finally {
                if (analysisBitmap !== faceBitmap) {
                    analysisBitmap.recycle()
                }
            }
        } finally {
            faceBitmap.recycle()
        }
    }

    private fun cropFaceRegion(bitmap: Bitmap, bounds: Rect): Bitmap? {
        val paddingX = (bounds.width() * 0.1f).toInt()
        val paddingY = (bounds.height() * 0.1f).toInt()
        val left = (bounds.left - paddingX).coerceAtLeast(0)
        val top = (bounds.top - paddingY).coerceAtLeast(0)
        val right = (bounds.right + paddingX).coerceAtMost(bitmap.width)
        val bottom = (bounds.bottom + paddingY).coerceAtMost(bitmap.height)
        val width = right - left
        val height = bottom - top
        if (width <= 0 || height <= 0) return null
        return Bitmap.createBitmap(bitmap, left, top, width, height)
    }

    private fun calculateLaplacianVariance(bitmap: Bitmap): Double {
        val width = bitmap.width
        val height = bitmap.height
        val pixels = IntArray(width * height)
        bitmap.getPixels(pixels, 0, width, 0, 0, width, height)

        val gray = IntArray(width * height)
        for (index in pixels.indices) {
            val pixel = pixels[index]
            val red = (pixel shr 16) and 0xFF
            val green = (pixel shr 8) and 0xFF
            val blue = pixel and 0xFF
            gray[index] = (0.299 * red + 0.587 * green + 0.114 * blue).toInt()
        }

        var sum = 0.0
        var sumSquares = 0.0
        var count = 0

        for (y in 1 until height - 1) {
            for (x in 1 until width - 1) {
                val center = y * width + x
                val laplacian = (-4 * gray[center]) +
                    gray[center - 1] +
                    gray[center + 1] +
                    gray[center - width] +
                    gray[center + width]
                sum += laplacian
                sumSquares += laplacian * laplacian
                count++
            }
        }

        if (count == 0) return 0.0
        val mean = sum / count
        return (sumSquares / count) - (mean * mean)
    }

    private fun decodeUprightBitmap(context: Context, uri: Uri): Bitmap? {
        val rotation = readExifRotation(context, uri)
        val bitmap = decodeBitmap(context, uri) ?: return null
        if (rotation == 0) return bitmap

        val matrix = Matrix().apply { postRotate(rotation.toFloat()) }
        val rotated = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
        if (rotated !== bitmap) {
            bitmap.recycle()
        }
        return rotated
    }

    private fun readExifRotation(context: Context, uri: Uri): Int {
        return try {
            context.contentResolver.openInputStream(uri)?.use { stream ->
                when (
                    ExifInterface(stream).getAttributeInt(
                        ExifInterface.TAG_ORIENTATION,
                        ExifInterface.ORIENTATION_NORMAL
                    )
                ) {
                    ExifInterface.ORIENTATION_ROTATE_90 -> 90
                    ExifInterface.ORIENTATION_ROTATE_180 -> 180
                    ExifInterface.ORIENTATION_ROTATE_270 -> 270
                    else -> 0
                }
            } ?: 0
        } catch (_: Exception) {
            0
        }
    }

    private fun decodeBitmap(context: Context, uri: Uri): Bitmap? {
        val boundsOptions = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        context.contentResolver.openInputStream(uri)?.use { stream ->
            BitmapFactory.decodeStream(stream, null, boundsOptions)
        }

        val decodeOptions = BitmapFactory.Options().apply {
            inSampleSize = calculateInSampleSize(boundsOptions, MAX_DECODE_SIZE, MAX_DECODE_SIZE)
            inPreferredConfig = Bitmap.Config.ARGB_8888
        }

        return context.contentResolver.openInputStream(uri)?.use { stream ->
            BitmapFactory.decodeStream(stream, null, decodeOptions)
        }
    }

    private fun calculateInSampleSize(
        options: BitmapFactory.Options,
        maxWidth: Int,
        maxHeight: Int
    ): Int {
        val height = options.outHeight
        val width = options.outWidth
        var inSampleSize = 1

        if (height > maxHeight || width > maxWidth) {
            val halfHeight = height / 2
            val halfWidth = width / 2
            while ((halfHeight / inSampleSize) >= maxHeight &&
                (halfWidth / inSampleSize) >= maxWidth
            ) {
                inSampleSize *= 2
            }
        }

        return max(1, inSampleSize)
    }
}

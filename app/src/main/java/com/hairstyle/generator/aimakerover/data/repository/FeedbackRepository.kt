package com.hairstyle.generator.aimakerover.data.repository

import android.content.Context
import android.net.Uri
import android.util.Log
import android.webkit.MimeTypeMap
import com.hairstyle.generator.aimakeover.R
import com.hairstyle.generator.aimakerover.data.remoteconfig.RemoteConfigManager
import com.hairstyle.generator.aimakerover.utils.DeviceIdProvider
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FeedbackRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val okHttpClient: OkHttpClient,
    private val remoteConfigManager: RemoteConfigManager
) {

    /**
     * POST multipart: image?, deviceId, appName, packageName, title, description, mail_id?
     * URL: Firebase `feedback_url` + `/api/apps/upload` (plain URL, no encrypt).
     */
    suspend fun submitFeedback(
        title: String,
        description: String,
        mailId: String? = null,
        imageUri: Uri?
    ): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val apiUrl = remoteConfigManager.buildFeedbackUploadUrl()
            val deviceId = DeviceIdProvider.getDeviceId(context)
            val appName = context.getString(R.string.app_name)
            val packageName = context.packageName
            val trimmedMailId = mailId?.trim().orEmpty()

            // Log.d(TAG, "â”€â”€â”€â”€â”€â”€â”€â”€ FEEDBACK REQUEST â”€â”€â”€â”€â”€â”€â”€â”€")
            // Log.d(TAG, "apiUrl      = $apiUrl")
            // Log.d(TAG, "deviceId    = $deviceId")
            // Log.d(TAG, "appName     = $appName")
            // Log.d(TAG, "packageName = $packageName")
            // Log.d(TAG, "title       = $title")
            // Log.d(TAG, "description = $description")
            // Log.d(TAG, "mail_id     = ${trimmedMailId.ifBlank { "(not sent)" }}")

            val multipart = MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("deviceId", deviceId)
                .addFormDataPart("appName", appName)
                .addFormDataPart("packageName", packageName)
                .addFormDataPart("title", title)
                .addFormDataPart("description", description)

            if (trimmedMailId.isNotBlank()) {
                multipart.addFormDataPart("mail_id", trimmedMailId)
            }

            var tempImage: File? = null
            if (imageUri != null) {
                tempImage = copyUriToCache(imageUri)
                val mime = guessMimeType(imageUri, tempImage) ?: "image/jpeg"
                val fileName = tempImage.name.ifBlank { "feedback.jpg" }
                // Log.d(TAG, "imageName   = $fileName")
                // Log.d(TAG, "imageMime   = $mime")
                // Log.d(TAG, "imageSize   = ${tempImage.length() / 1024}KB")
                multipart.addFormDataPart(
                    "image",
                    fileName,
                    tempImage.asRequestBody(mime.toMediaTypeOrNull())
                )
            } else {
                // Log.d(TAG, "image       = (not sent)")
            }

            try {
                val request = Request.Builder()
                    .url(apiUrl)
                    .post(multipart.build())
                    .build()

                okHttpClient.newCall(request).execute().use { response ->
                    val raw = response.body?.string().orEmpty()
                    // Log.d(TAG, "â”€â”€â”€â”€â”€â”€â”€â”€ FEEDBACK RESPONSE â”€â”€â”€â”€â”€â”€â”€â”€")
                    // Log.d(TAG, "httpCode    = ${response.code}")
                    // Log.d(TAG, "rawBody     = $raw")

                    if (!response.isSuccessful) {
                        throw IOException(
                            raw.takeIf { it.isNotBlank() }
                                ?: "Feedback upload failed (${response.code})"
                        )
                    }
                }
            } finally {
                tempImage?.delete()
            }
        }
    }

    private fun copyUriToCache(uri: Uri): File {
        val extension = guessExtension(uri) ?: "jpg"
        val dest = File(context.cacheDir, "feedback_${System.currentTimeMillis()}.$extension")
        context.contentResolver.openInputStream(uri)?.use { input ->
            dest.outputStream().use { output -> input.copyTo(output) }
        } ?: throw IOException("Cannot read selected image")
        if (!dest.exists() || dest.length() == 0L) {
            throw IOException("Selected image is empty")
        }
        return dest
    }

    private fun guessMimeType(uri: Uri, file: File): String? {
        context.contentResolver.getType(uri)?.let { return it }
        val ext = file.extension.lowercase()
        return MimeTypeMap.getSingleton().getMimeTypeFromExtension(ext)
    }

    private fun guessExtension(uri: Uri): String? {
        val type = context.contentResolver.getType(uri)
        if (!type.isNullOrBlank()) {
            MimeTypeMap.getSingleton().getExtensionFromMimeType(type)?.let { return it }
        }
        return uri.lastPathSegment
            ?.substringAfterLast('.', missingDelimiterValue = "")
            ?.takeIf { it.isNotBlank() && it.length <= 5 }
    }

    companion object {
        private const val TAG = "FeedbackRepository"
    }
}

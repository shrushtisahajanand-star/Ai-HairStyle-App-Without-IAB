package com.hairstyle.changer.aitryon.repository

import android.content.Context
import android.util.Log
import com.google.gson.Gson
import com.hairstyle.changer.aitryon.model.LocalHairstyleResponse
import com.hairstyle.changer.aitryon.remote.GenerateApiConfig
import com.hairstyle.changer.aitryon.remote.GenerateApiFlags
import com.hairstyle.changer.aitryon.utils.DeviceIdProvider
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LocalHairstyleRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val okHttpClient: OkHttpClient,
    private val gson: Gson
) {

    /**
     * POST form-data: image (JPEG), hair_style, color
     * URL: [GenerateApiConfig.baseUrl()] + [GenerateApiConfig.HAIRSTYLE_ENDPOINT]
     */
    suspend fun generate(
        imageFile: File?,
        hairStyle: String,
        color: String
    ): LocalHairstyleResponse = withContext(Dispatchers.IO) {
        val apiUrl = GenerateApiConfig.buildUrl(GenerateApiConfig.HAIRSTYLE_ENDPOINT)
//        Log.d(TAG, "â”€â”€â”€â”€â”€â”€â”€â”€ REQUEST â”€â”€â”€â”€â”€â”€â”€â”€")
//        Log.d(TAG, "apiUrl      = $apiUrl")
//        Log.d(TAG, "hair_style  = $hairStyle")
//        Log.d(TAG, "color       = $color")
//        Log.d(TAG, "sendImage   = ${GenerateApiFlags.SEND_IMAGE_FILE}")

        val multipart = MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart("hair_style", hairStyle)
            .addFormDataPart("color", color)
            .also { appendDeviceField(it) }

        if (GenerateApiFlags.SEND_IMAGE_FILE) {
            require(imageFile != null && imageFile.exists()) {
                "Image file is required for generate API"
            }
            val sizeKb = imageFile.length() / 1024
//            Log.d(TAG, "imagePath   = ${imageFile.absolutePath}")
//            Log.d(TAG, "imageName   = photo.jpg")
//            Log.d(TAG, "imageSize   = ${sizeKb}KB")
//            Log.d(TAG, "imageMime   = image/jpeg")
//
            val body = imageFile.asRequestBody("image/jpeg".toMediaTypeOrNull())
            multipart.addFormDataPart("image", "photo.jpg", body)
        } else {
//            Log.d(TAG, "imagePath   = (not sent)")
        }

        val request = Request.Builder()
            .url(apiUrl)
            .post(multipart.build())
            .build()

        okHttpClient.newCall(request).execute().use { response ->
            val raw = response.body?.string().orEmpty()
            //Log.d(TAG, "â”€â”€â”€â”€â”€â”€â”€â”€ RESPONSE â”€â”€â”€â”€â”€â”€â”€â”€")
//            Log.d(TAG, "httpCode    = ${response.code}")
//            Log.d(TAG, "rawBody     = $raw")

            if (!response.isSuccessful) {
//                 Log.e(TAG, "Generate failed: HTTP ${response.code}")
                error("Generate failed: HTTP ${response.code} $raw")
            }
            val parsed = gson.fromJson(raw, LocalHairstyleResponse::class.java)
                ?: error("Invalid generate response")

//            Log.d(TAG, "success     = ${parsed.success}")
//            Log.d(TAG, "resultUrl   = ${parsed.image}")
            if (!parsed.success) {
//                Log.e(TAG, "serverMsg   = ${parsed.failureReason()} (HTTP ${response.code})")
            }
            parsed
        }
    }

    /**
     * POST form-data: image (JPEG), beard_style
     * URL: [GenerateApiConfig.baseUrl()] + [GenerateApiConfig.HAIRSTYLE_ENDPOINT]
     */
    suspend fun generateBeard(
        imageFile: File?,
        beardStyle: String
    ): LocalHairstyleResponse = withContext(Dispatchers.IO) {
        val apiUrl = GenerateApiConfig.buildUrl(GenerateApiConfig.HAIRSTYLE_ENDPOINT)
//        Log.d(TAG, "â”€â”€â”€â”€â”€â”€â”€â”€ BEARD REQUEST â”€â”€â”€â”€â”€â”€â”€â”€")
//        Log.d(TAG, "apiUrl       = $apiUrl")
//        Log.d(TAG, "beard_style  = $beardStyle")
//        Log.d(TAG, "sendImage    = ${GenerateApiFlags.SEND_IMAGE_FILE}")

        val multipart = MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart("beard_style", beardStyle)
            .also { appendDeviceField(it) }

        if (GenerateApiFlags.SEND_IMAGE_FILE) {
            require(imageFile != null && imageFile.exists()) {
                "Image file is required for generate API"
            }
            val sizeKb = imageFile.length() / 1024
//            Log.d(TAG, "imagePath    = ${imageFile.absolutePath}")
//            Log.d(TAG, "imageName    = photo.jpg")
//            Log.d(TAG, "imageSize    = ${sizeKb}KB")
//            Log.d(TAG, "imageMime    = image/jpeg")

            val body = imageFile.asRequestBody("image/jpeg".toMediaTypeOrNull())
            multipart.addFormDataPart("image", "photo.jpg", body)
        } else {
//            Log.d(TAG, "imagePath    = (not sent)")
        }

        val request = Request.Builder()
            .url(apiUrl)
            .post(multipart.build())
            .build()

        okHttpClient.newCall(request).execute().use { response ->
            val raw = response.body?.string().orEmpty()
//            Log.d(TAG, "â”€â”€â”€â”€â”€â”€â”€â”€ BEARD RESPONSE â”€â”€â”€â”€â”€â”€â”€â”€")
//            Log.d(TAG, "httpCode     = ${response.code}")
//            Log.d(TAG, "rawBody      = $raw")

            if (!response.isSuccessful) {
//                Log.e(TAG, "Beard generate failed: HTTP ${response.code}")
                error("Generate failed: HTTP ${response.code} $raw")
            }
            val parsed = gson.fromJson(raw, LocalHairstyleResponse::class.java)
                ?: error("Invalid generate response")

//            Log.d(TAG, "success      = ${parsed.success}")
//            Log.d(TAG, "resultUrl    = ${parsed.image}")
            parsed
        }
    }

    private fun appendDeviceField(builder: MultipartBody.Builder) {
        val deviceId = DeviceIdProvider.getDeviceId(context)
        Log.d(TAG, "device_id    = $deviceId")
        builder.addFormDataPart("device_id", deviceId)
    }

    /** API response image URL only â€” no fallback to selected style image. */
    fun resolveResultImageUrl(apiImageUrl: String?): String {
        require(!apiImageUrl.isNullOrBlank()) { "No result image URL in API response" }
//        Log.d(TAG, "resultSource = API response image")
//        Log.d(TAG, "finalUrl     = $apiImageUrl")
        return apiImageUrl
    }

    companion object {
        const val TAG = "HairstyleGenerate"
    }
}

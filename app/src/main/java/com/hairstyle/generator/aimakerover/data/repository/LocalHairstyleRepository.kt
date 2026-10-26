package com.hairstyle.generator.aimakerover.data.repository

import android.content.Context
import android.util.Log
import com.google.gson.Gson
import com.hairstyle.generator.aimakerover.billing.BillingManager
import com.hairstyle.generator.aimakerover.data.model.LocalHairstyleResponse
import com.hairstyle.generator.aimakerover.data.remote.GenerateApiConfig
import com.hairstyle.generator.aimakerover.data.remote.GenerateApiFlags
import com.hairstyle.generator.aimakerover.data.remoteconfig.RemoteConfigManager
import com.hairstyle.generator.aimakerover.utils.DeviceIdProvider
import com.hairstyle.generator.aimakerover.utils.PrefManager
import com.hairstyle.generator.aimakerover.utils.PremiumPlanManager
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LocalHairstyleRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val okHttpClient: OkHttpClient,
    private val gson: Gson,
    private val remoteConfigManager: RemoteConfigManager
) {

    /**
     * POST form-data: image (JPEG), hair_style, color
     * URL: Firebase generate base + [GenerateApiConfig.HAIRSTYLE_ENDPOINT]
     */
    suspend fun generate(
        imageFile: File?,
        hairStyle: String,
        color: String
    ): LocalHairstyleResponse = withContext(Dispatchers.IO) {
        val apiUrl = remoteConfigManager.buildGenerateApiUrl(GenerateApiConfig.HAIRSTYLE_ENDPOINT)
//        Log.d(TAG, "â”€â”€â”€â”€â”€â”€â”€â”€ REQUEST â”€â”€â”€â”€â”€â”€â”€â”€")
//        Log.d(TAG, "apiUrl      = $apiUrl")
//        Log.d(TAG, "hair_style  = $hairStyle")
//        Log.d(TAG, "color       = $color")
//        Log.d(TAG, "sendImage   = ${GenerateApiFlags.SEND_IMAGE_FILE}")

        val multipart = MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart("hair_style", hairStyle)
            .addFormDataPart("color", color)
            .also { appendDeviceAndPremiumFields(it) }

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
     * URL: Firebase generate base + [GenerateApiConfig.HAIRSTYLE_ENDPOINT]
     */
    suspend fun generateBeard(
        imageFile: File?,
        beardStyle: String
    ): LocalHairstyleResponse = withContext(Dispatchers.IO) {
        val apiUrl = remoteConfigManager.buildGenerateApiUrl(GenerateApiConfig.HAIRSTYLE_ENDPOINT)
//        Log.d(TAG, "â”€â”€â”€â”€â”€â”€â”€â”€ BEARD REQUEST â”€â”€â”€â”€â”€â”€â”€â”€")
//        Log.d(TAG, "apiUrl       = $apiUrl")
//        Log.d(TAG, "beard_style  = $beardStyle")
//        Log.d(TAG, "sendImage    = ${GenerateApiFlags.SEND_IMAGE_FILE}")

        val multipart = MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart("beard_style", beardStyle)
            .also { appendDeviceAndPremiumFields(it) }

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

    private fun appendDeviceAndPremiumFields(builder: MultipartBody.Builder) {
        val deviceId = DeviceIdProvider.getDeviceId(context)
        val isPremium = PremiumPlanManager.isPremiumActive()
        Log.d(TAG, "device_id    = $deviceId")
//        Log.d(TAG, "is_premium   = $isPremium")
        builder
            .addFormDataPart("device_id", deviceId)
            .addFormDataPart("is_premium", isPremium.toString())

        // Premium-only fields: purchase_id, plan_type, purchase_date, expiry_date
        if (!isPremium) return

        val active = PremiumPlanManager.getActivePlans().firstOrNull()
        val planType = active?.planKey.orEmpty()
        val purchaseId = resolvePurchaseId()
        val purchaseDate = formatIsoUtc(active?.startedAt ?: 0L)
        val expiryDate = formatIsoUtc(active?.expiresAt ?: 0L)

        if (purchaseId.isNotBlank()) {
            builder.addFormDataPart("purchase_id", purchaseId)
        }
        if (planType.isNotBlank()) {
            builder.addFormDataPart("plan_type", planType)
        }
        if (purchaseDate.isNotBlank()) {
            builder.addFormDataPart("purchase_date", purchaseDate)
        }
        if (expiryDate.isNotBlank()) {
            builder.addFormDataPart("expiry_date", expiryDate)
        }

        //Log.d(TAG, "──────── GENERATE API premium fields ────────")
//        Log.d(TAG, "purchase_id   = $purchaseId")
//        Log.d(TAG, "plan_type     = $planType")
//        Log.d(TAG, "purchase_date = $purchaseDate")
//        Log.d(TAG, "expiry_date   = $expiryDate")
    }

    /** RevenueCat originalAppUserId (unique). Falls back to live appUserID if prefs empty. */
    private fun resolvePurchaseId(): String {
        PrefManager.getRcOriginalAppUserId().takeIf { it.isNotBlank() }?.let { return it }
        return runCatching {
            com.revenuecat.purchases.Purchases.sharedInstance.appUserID.trim()
        }.getOrDefault("").also { id ->
            if (id.isNotBlank()) PrefManager.setRcOriginalAppUserId(id)
        }
    }

    private fun formatIsoUtc(epochMs: Long): String {
        if (epochMs <= 0L) return ""
        val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US)
        sdf.timeZone = TimeZone.getTimeZone("UTC")
        return sdf.format(Date(epochMs))
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

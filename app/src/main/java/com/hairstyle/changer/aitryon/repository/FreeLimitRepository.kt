package com.hairstyle.changer.aitryon.repository

import android.content.Context
import com.google.gson.Gson
import com.hairstyle.changer.aitryon.model.CheckFreeLimitResponse
import com.hairstyle.changer.aitryon.remote.GenerateApiConfig
import com.hairstyle.changer.aitryon.utils.DeviceIdProvider
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Result of POST /api/check-free-limit before photo / generate.
 *
 * - `image_generation == true` and (`remaining` missing or `remaining > 0`) → [Allowed]
 * - `image_generation == false` or `remaining <= 0` → [DailyLimitReached]
 */
sealed class FreeLimitCheckResult {
    data object Allowed : FreeLimitCheckResult()
    data object DailyLimitReached : FreeLimitCheckResult()
}

@Singleton
class FreeLimitRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val okHttpClient: OkHttpClient,
    private val gson: Gson
) {

    /**
     * Checks the server daily free limit before face detection or generate.
     * On network/parse failure → [FreeLimitCheckResult.Allowed] (fail-open).
     */
    suspend fun checkBeforeFaceDetection(): FreeLimitCheckResult = withContext(Dispatchers.IO) {
        runCatching {
            val deviceId = DeviceIdProvider.getDeviceId(context)
            val apiUrl = GenerateApiConfig.buildUrl(GenerateApiConfig.CHECK_FREE_LIMIT_ENDPOINT)
            val jsonBody = JSONObject().put("device_id", deviceId).toString()

            val request = Request.Builder()
                .url(apiUrl)
                .post(jsonBody.toRequestBody(JSON_MEDIA_TYPE))
                .header("Content-Type", "application/json")
                .build()

            okHttpClient.newCall(request).execute().use { response ->
                val raw = response.body?.string().orEmpty()

                if (!response.isSuccessful) {
                    return@withContext FreeLimitCheckResult.Allowed
                }

                val parsed = gson.fromJson(raw, CheckFreeLimitResponse::class.java)
                    ?: return@withContext FreeLimitCheckResult.Allowed

                resolveResult(parsed)
            }
        }.getOrElse {
            FreeLimitCheckResult.Allowed
        }
    }

    private fun resolveResult(parsed: CheckFreeLimitResponse): FreeLimitCheckResult {
        val remaining = parsed.data?.remaining
        if (remaining != null && remaining <= 0) {
            return FreeLimitCheckResult.DailyLimitReached
        }
        // null / missing → fail-open
        if (parsed.effectiveImageGeneration() == false) {
            return FreeLimitCheckResult.DailyLimitReached
        }
        return FreeLimitCheckResult.Allowed
    }

    companion object {
        private val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()
    }
}

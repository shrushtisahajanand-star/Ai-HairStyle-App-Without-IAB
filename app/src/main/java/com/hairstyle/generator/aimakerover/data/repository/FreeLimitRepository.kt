package com.hairstyle.generator.aimakerover.data.repository

import android.content.Context
import android.util.Log
import com.google.gson.Gson
import com.hairstyle.generator.aimakerover.data.model.CheckFreeLimitResponse
import com.hairstyle.generator.aimakerover.data.remote.GenerateApiConfig
import com.hairstyle.generator.aimakerover.data.remoteconfig.RemoteConfigManager
import com.hairstyle.generator.aimakerover.utils.DeviceIdProvider
import com.hairstyle.generator.aimakerover.utils.PremiumPlanManager
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

sealed class FreeLimitCheckResult {
    data object Allowed : FreeLimitCheckResult()
    data object LimitReached : FreeLimitCheckResult()
}

@Singleton
class FreeLimitRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val okHttpClient: OkHttpClient,
    private val gson: Gson,
    private val remoteConfigManager: RemoteConfigManager
) {

    /**
     * Checks server free-limit before face detection.
     * Premium users skip the API and are always [FreeLimitCheckResult.Allowed].
     * On network/parse failure → [FreeLimitCheckResult.Allowed] (fail-open).
     *
     * TEMP: free-limit API not ready — always [FreeLimitCheckResult.Allowed].
     * Uncomment the body below when backend is live.
     */
    suspend fun checkBeforeFaceDetection(): FreeLimitCheckResult = withContext(Dispatchers.IO) {
        // TEMP: free-limit API not ready yet — allow everyone.
        return@withContext FreeLimitCheckResult.Allowed
        /*
        if (PremiumPlanManager.isPremiumActive()) {
            // Log.d(TAG, "Premium active — skip free-limit check")
            return@withContext FreeLimitCheckResult.Allowed
        }

        runCatching {
            val deviceId = DeviceIdProvider.getDeviceId(context)
            val apiUrl = remoteConfigManager.buildGenerateApiUrl(
                GenerateApiConfig.CHECK_FREE_LIMIT_ENDPOINT
            )
            val jsonBody = JSONObject().put("device_id", deviceId).toString()
            // Log.d(TAG, "──────── CHECK FREE LIMIT ────────")
            // Log.d(TAG, "url       = $apiUrl")
            // Log.d(TAG, "device_id = $deviceId")

            val request = Request.Builder()
                .url(apiUrl)
                .post(jsonBody.toRequestBody(JSON_MEDIA_TYPE))
                .header("Content-Type", "application/json")
                .build()

            okHttpClient.newCall(request).execute().use { response ->
                val raw = response.body?.string().orEmpty()
                // Log.d(TAG, "httpCode  = ${response.code}")
                // Log.d(TAG, "rawBody   = $raw")

                if (!response.isSuccessful) {
                    // Log.w(TAG, "Free-limit check HTTP ${response.code} — allow continue")
                    return@withContext FreeLimitCheckResult.Allowed
                }

                val parsed = gson.fromJson(raw, CheckFreeLimitResponse::class.java)
                    ?: return@withContext FreeLimitCheckResult.Allowed

                val reached = parsed.isLimitReached()
                // Log.d(
                    // TAG,
                    // "expired=${parsed.expired} free_limit=${parsed.freeLimit} used=${parsed.used} reached=$reached"
                // )
                if (reached) FreeLimitCheckResult.LimitReached else FreeLimitCheckResult.Allowed
            }
        }.getOrElse { e ->
            // Log.e(TAG, "Free-limit check failed — allow continue", e)
            FreeLimitCheckResult.Allowed
        }
        */
    }

    companion object {
        private const val TAG = "FreeLimitCheck"
        private val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()
    }
}

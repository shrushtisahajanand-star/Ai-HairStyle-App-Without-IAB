package com.hairstyle.generator.aimakerover.data.repository

import android.content.Context
import android.util.Log
import com.google.gson.Gson
import com.hairstyle.generator.aimakerover.data.model.CheckFreeLimitResponse
import com.hairstyle.generator.aimakerover.data.model.FreeLimitSubscription
import com.hairstyle.generator.aimakerover.data.remote.GenerateApiConfig
import com.hairstyle.generator.aimakerover.data.remoteconfig.RemoteConfigManager
import com.hairstyle.generator.aimakerover.utils.DeviceIdProvider
import com.hairstyle.generator.aimakerover.utils.PrefManager
import com.hairstyle.generator.aimakerover.utils.PremiumPlanManager
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Result of POST /api/check-free-limit before photo / generate.
 *
 * Allow only when generation is permitted by server flags **and** daily quota:
 * - `image_generation == true` and (no today cap or `today_remaining > 0`) → [Allowed]
 * - `today_remaining <= 0` (premium, not expired) → [DailyLimitReached]
 * - `image_generation == false` + free user → [FreeLimitReached]
 * - `image_generation == false` + premium + subscription_expired → [PremiumExpired]
 * - `image_generation == false` + premium + not expired → [DailyLimitReached]
 */
sealed class FreeLimitCheckResult {
    data object Allowed : FreeLimitCheckResult()
    data object FreeLimitReached : FreeLimitCheckResult()
    data object PremiumExpired : FreeLimitCheckResult()
    data object DailyLimitReached : FreeLimitCheckResult()
}

@Singleton
class FreeLimitRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val okHttpClient: OkHttpClient,
    private val gson: Gson,
    private val remoteConfigManager: RemoteConfigManager
) {

    /**
     * Checks server free-limit / subscription quota before face detection or generate.
     * On network/parse failure → [FreeLimitCheckResult.Allowed] (fail-open).
     */
    suspend fun checkBeforeFaceDetection(): FreeLimitCheckResult = withContext(Dispatchers.IO) {
        runCatching {
            val deviceId = DeviceIdProvider.getDeviceId(context)
            val isPremium = PremiumPlanManager.isPremiumActive()
            val apiUrl = remoteConfigManager.buildGenerateApiUrl(
                GenerateApiConfig.CHECK_FREE_LIMIT_ENDPOINT
            )
            val jsonBody = buildRequestBody(deviceId, isPremium)
            //Log.d(TAG, "──────── CHECK FREE LIMIT ────────")
            //Log.d(TAG, "url       = $apiUrl")
            //Log.d(TAG, "body      = $jsonBody")

            val request = Request.Builder()
                .url(apiUrl)
                .post(jsonBody.toRequestBody(JSON_MEDIA_TYPE))
                .header("Content-Type", "application/json")
                .build()

            okHttpClient.newCall(request).execute().use { response ->
                val raw = response.body?.string().orEmpty()
                //Log.d(TAG, "httpCode  = ${response.code}")
                //Log.d(TAG, "rawBody   = $raw")

                if (!response.isSuccessful) {
                    //Log.w(TAG, "Free-limit check HTTP ${response.code} — allow continue")
                    return@withContext FreeLimitCheckResult.Allowed
                }

                val parsed = gson.fromJson(raw, CheckFreeLimitResponse::class.java)
                    ?: return@withContext FreeLimitCheckResult.Allowed

                applySubscriptionDates(parsed.subscription())

                val result = resolveResult(parsed, isPremium)
                //Log.d(
//                    TAG,
//                    "image_generation=${parsed.effectiveImageGeneration()} " +
//                        "subscription_expired=${parsed.effectiveSubscriptionExpired()} " +
//                        "today_remaining=${parsed.subscription()?.todayRemaining} " +
//                        "result=$result"
//                )
                result
            }
        }.getOrElse { e ->
            //Log.e(TAG, "Free-limit check failed — allow continue", e)
            FreeLimitCheckResult.Allowed
        }
    }

    private fun resolveResult(
        parsed: CheckFreeLimitResponse,
        localPremium: Boolean
    ): FreeLimitCheckResult {
        val sub = parsed.subscription()
        val subscriptionExpired = parsed.effectiveSubscriptionExpired()

        // Server may still send image_generation=true while today's quota is 0 — block that.
        val todayRemaining = sub?.todayRemaining
        if (localPremium &&
            !subscriptionExpired &&
            todayRemaining != null &&
            todayRemaining <= 0
        ) {
            return FreeLimitCheckResult.DailyLimitReached
        }

        // Also treat used-up daily allocation as exhausted when remaining field is missing.
        val todayAllocated = sub?.todayAllocated
        val todayUsed = sub?.todayUsed
        if (localPremium &&
            !subscriptionExpired &&
            todayRemaining == null &&
            todayAllocated != null &&
            todayUsed != null &&
            todayUsed >= todayAllocated
        ) {
            return FreeLimitCheckResult.DailyLimitReached
        }

        val canGenerate = parsed.effectiveImageGeneration()
        if (canGenerate == true) return FreeLimitCheckResult.Allowed
        // null / missing → fail-open
        if (canGenerate == null) return FreeLimitCheckResult.Allowed

        // image_generation == false
        if (!localPremium) {
            return FreeLimitCheckResult.FreeLimitReached
        }

        if (subscriptionExpired) {
            PremiumPlanManager.clearActivePlan()
            return FreeLimitCheckResult.PremiumExpired
        }

        return FreeLimitCheckResult.DailyLimitReached
    }

    private fun buildRequestBody(deviceId: String, isPremium: Boolean): String {
        val root = JSONObject()
            .put("device_id", deviceId)
            .put("is_premium", isPremium)

        if (!isPremium) return root.toString()

        val purchaseId = resolvePurchaseId()
        if (purchaseId.isNotBlank()) {
            root.put("purchase_id", purchaseId)
        }

        val plans = JSONArray()
        PremiumPlanManager.getActivePlans().forEach { record ->
            val purchaseDate = formatIsoUtc(record.startedAt)
            val expiryDate = formatIsoUtc(record.expiresAt)
            if (record.planKey.isBlank()) return@forEach
            plans.put(
                JSONObject()
                    .put("plan_type", record.planKey)
                    .put("purchase_date", purchaseDate)
                    .put("expiry_date", expiryDate)
            )
        }
        if (plans.length() > 0) {
            root.put("plans", plans)
            logPlansPayload(api = "check-free-limit", plans = plans)
        } else {
            //Log.w(TAG, "check-free-limit: is_premium=true but plans[] is empty")
        }
        return root.toString()
    }

    private fun logPlansPayload(api: String, plans: JSONArray) {
        //Log.d(TAG, "──────── API plans payload ($api) ────────")
        for (i in 0 until plans.length()) {
            val plan = plans.optJSONObject(i) ?: continue
            //Log.d(
//                TAG,
//                "plans[$i] plan_type=${plan.optString("plan_type")} " +
//                    "purchase_date=${plan.optString("purchase_date")} " +
//                    "expiry_date=${plan.optString("expiry_date")}"
//            )
        }
    }

    /** Keep Active Subscription UI in sync with server purchase/expiry. */
    private fun applySubscriptionDates(subscription: FreeLimitSubscription?) {
        if (subscription == null) return
        val planType = subscription.planType?.trim().orEmpty()
        if (planType.isBlank()) return
        val purchaseMs = parseIsoUtc(subscription.purchaseDate)
        val expiryMs = parseIsoUtc(subscription.expiryDate)
        if (purchaseMs == null && expiryMs == null) return
        PremiumPlanManager.syncPlanDatesFromServer(
            serverPlanKey = planType,
            purchaseMs = purchaseMs,
            expiryMs = expiryMs
        )
    }

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
        return ISO_FORMAT.get()!!.format(Date(epochMs))
    }

    private fun parseIsoUtc(raw: String?): Long? {
        if (raw.isNullOrBlank()) return null
        return runCatching { ISO_FORMAT.get()!!.parse(raw)?.time }.getOrNull()
            ?.takeIf { it > 0L }
    }

    companion object {
        private const val TAG = "FreeLimitCheck"
        private val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()

        private val ISO_FORMAT = object : ThreadLocal<SimpleDateFormat>() {
            override fun initialValue(): SimpleDateFormat {
                return SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US).apply {
                    timeZone = TimeZone.getTimeZone("UTC")
                }
            }
        }
    }
}

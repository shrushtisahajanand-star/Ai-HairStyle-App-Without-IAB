package com.hairstyle.changer.aitryon.remoteconfig

import android.util.Log
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.google.firebase.remoteconfig.FirebaseRemoteConfigSettings
import com.google.gson.Gson
import com.google.gson.JsonParser
import com.google.gson.annotations.SerializedName
import com.hairstyle.changer.aitryon.Ads.ADSMainClass
import com.hairstyle.changer.aitryon.remote.FeedbackApiConfig
import com.hairstyle.changer.aitryon.remote.GenerateApiConfig
import com.hairstyle.changer.aitryon.utils.InstallReferrerHelper
import com.hairstyle.changer.aitryon.utils.PrefManager
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RemoteConfigManager @Inject constructor(
    private val remoteConfig: FirebaseRemoteConfig,
    private val gson: Gson
) {

    companion object {
        private const val TAG = "UserSegment"
    }

    suspend fun fetchAndSaveAll() {
        val settings = FirebaseRemoteConfigSettings.Builder()
            .setMinimumFetchIntervalInSeconds(0)
            .build()
        remoteConfig.setConfigSettingsAsync(settings).await()
        remoteConfig.fetchAndActivate().await()

        val fileName = RemoteConfigKeys.remoteConfigFileName()
        val rawJson = remoteConfig.getString(fileName)
        val json = resolveUserSegmentJson(rawJson)

        PrefManager.setRemoteConfigJson(json)

        if (json.isNotBlank()) {
            runCatching {
                val root = gson.fromJson(json, RemoteConfigApiRoot::class.java)
                PrefManager.setPrivacyPolicyUrl(root.api?.privacyPolicyUrl.orEmpty())
                PrefManager.setBaseUrl(root.api?.baseUrl.orEmpty())
                PrefManager.setFeedbackUrl(root.api?.feedbackUrl.orEmpty())
                PrefManager.setImagesPageLimit(
                    root.api?.pageLimit ?: RemoteConfigKeys.DEFAULT_PAGE_LIMIT
                )
            }
            saveUpdateAppFromJson(json)
            AdsConfigApplier.applyFromRemoteJson(json, gson)
        }

        PrefManager.setRemoteConfigFetched(true)
    }

    /**
     * Firebase file can be `{ ads, api, ... }` or
     * `{ "normal_user": {...}, "paid_user": {...} }` (optionally wrapped in an array).
     */
    private fun resolveUserSegmentJson(raw: String): String {
        val trimmed = raw.trim()
        if (trimmed.isBlank()) return raw
        val rootElement = runCatching { JsonParser.parseString(trimmed) }.getOrNull() ?: return raw
        val root = when {
            rootElement.isJsonArray && rootElement.asJsonArray.size() > 0 -> {
                val first = rootElement.asJsonArray[0]
                if (first.isJsonObject) first.asJsonObject else null
            }
            rootElement.isJsonObject -> rootElement.asJsonObject
            else -> null
        } ?: return raw

        if (!root.has(ADSMainClass.NORMAL_USER) && !root.has(ADSMainClass.PAID_USER)) {
            return raw
        }

        val preferredKey = if (InstallReferrerHelper.isPaidMarketingReferrer()) {
            Log.d(TAG, "resolveUserSegmentJson: using paid_user")
            ADSMainClass.PAID_USER
        } else {
            Log.d(TAG, "resolveUserSegmentJson: using normal_user")
            ADSMainClass.NORMAL_USER
        }

        val fallbackKey = if (preferredKey == ADSMainClass.PAID_USER) {
            ADSMainClass.NORMAL_USER
        } else {
            ADSMainClass.PAID_USER
        }
        val block = root.get(preferredKey)?.takeIf { it.isJsonObject }
            ?: root.get(fallbackKey)?.takeIf { it.isJsonObject }
        if (block == null) {
            Log.w(TAG, "resolveUserSegmentJson: missing paid_user and normal_user blocks")
        } else if (block !== root.get(preferredKey)) {
            Log.w(TAG, "resolveUserSegmentJson: fallback to $fallbackKey ($preferredKey missing)")
        }
        return block?.asJsonObject?.toString() ?: raw
    }

    private fun saveUpdateAppFromJson(json: String) {
        runCatching {
            val root = gson.fromJson(json, RemoteConfigApiRoot::class.java)
            val config = root.updateApp ?: InAppUpdateConfig.DISABLED
            PrefManager.setUpdateAppJson(gson.toJson(config))
        }.onFailure {
            PrefManager.setUpdateAppJson(gson.toJson(InAppUpdateConfig.DISABLED))
        }
    }

    fun getInAppUpdateConfig(): InAppUpdateConfig {
        val json = PrefManager.getUpdateAppJson()
        if (json.isBlank()) return InAppUpdateConfig.DISABLED
        return runCatching {
            gson.fromJson(json, InAppUpdateConfig::class.java)
        }.getOrDefault(InAppUpdateConfig.DISABLED)
    }

    fun getPageLimit(): Int {
        return PrefManager.getImagesPageLimit(RemoteConfigKeys.DEFAULT_PAGE_LIMIT)
    }

    fun getPrivacyPolicyUrl(): String {
        val stored = PrefManager.getPrivacyPolicyUrl().trim()
        if (stored.isBlank()) return ""
        return if (stored.startsWith("http", ignoreCase = true)) stored else ""
    }

    fun getFeedbackUrl(): String {
        val url = PrefManager.getFeedbackUrl().trim().trimEnd('/')
        require(url.isNotBlank()) {
            "feedback_url not found in prefs. Remote Config must load on Splash."
        }
        require(url.startsWith("http", ignoreCase = true)) {
            "Invalid feedback_url: $url"
        }
        return url
    }

    fun buildFeedbackUploadUrl(): String {
        return GenerateApiConfig.buildUrl(getFeedbackUrl(), FeedbackApiConfig.UPLOAD_ENDPOINT)
    }

    data class RemoteConfigApiRoot(
        @SerializedName("APi") val api: ApiSection? = null,
        @SerializedName("update_app") val updateApp: InAppUpdateConfig? = null
    )

    data class ApiSection(
        @SerializedName("base_url") val baseUrl: String? = null,
        @SerializedName("privacy_policy_url") val privacyPolicyUrl: String? = null,
        @SerializedName("feedback_url") val feedbackUrl: String? = null,
        @SerializedName("page_limit") val pageLimit: Int? = null
    )
}

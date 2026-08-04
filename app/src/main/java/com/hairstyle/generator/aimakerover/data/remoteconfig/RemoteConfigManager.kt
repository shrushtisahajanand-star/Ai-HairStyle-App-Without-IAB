package com.hairstyle.generator.aimakerover.data.remoteconfig

import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.google.firebase.remoteconfig.FirebaseRemoteConfigSettings
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import com.hairstyle.generator.aimakeover.BuildConfig
import com.hairstyle.generator.aimakerover.data.model.PremiumPlanDetailsRoot
import com.hairstyle.generator.aimakerover.data.remote.FeedbackApiConfig
import com.hairstyle.generator.aimakerover.data.remote.GenerateApiConfig
import com.hairstyle.generator.aimakerover.utils.AESHelper
import com.hairstyle.generator.aimakerover.utils.PrefManager
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RemoteConfigManager @Inject constructor(
    private val remoteConfig: FirebaseRemoteConfig,
    private val gson: Gson
) {

    suspend fun fetchAndSaveAll() {
        val settings = FirebaseRemoteConfigSettings.Builder()
            .setMinimumFetchIntervalInSeconds(if (BuildConfig.DEBUG) 0 else 3600)
            .build()
        remoteConfig.setConfigSettingsAsync(settings).await()
        remoteConfig.fetchAndActivate().await()

        val fileName = RemoteConfigKeys.remoteConfigFileName()
        val json = remoteConfig.getString(fileName)

        PrefManager.setRemoteConfigJson(json)

        if (json.isNotBlank()) {
            runCatching {
                val root = gson.fromJson(json, RemoteConfigApiRoot::class.java)
                PrefManager.setImagesBaseUrl(root.api?.baseUrl.orEmpty())
                PrefManager.setVideosApiUrl(root.api?.videosUrl.orEmpty())
                PrefManager.setGenerateBaseUrl(root.api?.generateBaseUrl.orEmpty())
                PrefManager.setPrivacyPolicyUrl(root.api?.privacyPolicyUrl.orEmpty())
                PrefManager.setFeedbackUrl(root.api?.feedbackUrl.orEmpty())
                PrefManager.setImagesPageLimit(
                    root.api?.pageLimit ?: RemoteConfigKeys.DEFAULT_PAGE_LIMIT
                )
            }
            savePremiumPlanDetailsFromJson(json)
            saveUpdateAppFromJson(json)
            // Splash: ads → ADSMainClass SharedPreferences (+ PrefManager ads_config_json)
            AdsConfigApplier.applyFromRemoteJson(json, gson)
        }

        PrefManager.setRemoteConfigFetched(true)
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

    private fun savePremiumPlanDetailsFromJson(json: String) {
        // Store the Firebase object as-is. A parse → gson.toJson round trip is lossy for
        // custom-adapter fields and would silently fall back to the built-in defaults.
        runCatching {
            val jsonObject = com.google.gson.JsonParser.parseString(json).asJsonObject
            val key = PREMIUM_JSON_KEYS.firstOrNull { jsonObject.has(it) }
            if (key != null) {
                PrefManager.setPremiumPlanDetailsJson(jsonObject.get(key).toString())
                return
            }
        }
        runCatching {
            val root = gson.fromJson(json, RemoteConfigApiRoot::class.java)
            root.premiumPlanDetails?.let { details ->
                PrefManager.setPremiumPlanDetailsJson(gson.toJson(details))
            }
        }
    }

    companion object {
        private val PREMIUM_JSON_KEYS = listOf(
            RemoteConfigKeys.PREMIUM_PLAN_DETAILS,
            "premium_plan_details"
        )
    }

    fun getPremiumPlanDetails(): PremiumPlanDetailsRoot {
        val json = PrefManager.getPremiumPlanDetailsJson()
        if (json.isBlank()) return PremiumPlanDetailsRoot()
        return runCatching {
            gson.fromJson(json, PremiumPlanDetailsRoot::class.java)
        }.getOrDefault(PremiumPlanDetailsRoot())
    }

    fun getImagesBaseUrl(): String {
        val encrypted = PrefManager.getImagesBaseUrl()
        require(encrypted.isNotBlank()) {
            "base_url not found in prefs. Remote Config must load on Splash."
        }
        return AESHelper.decrypt(encrypted)
    }

    fun getPageLimit(): Int {
        return PrefManager.getImagesPageLimit(RemoteConfigKeys.DEFAULT_PAGE_LIMIT)
    }

    fun getVideosApiUrl(): String {
        val encrypted = PrefManager.getVideosApiUrl()
        require(encrypted.isNotBlank()) {
            "videos_url not found in prefs. Remote Config must load on Splash."
        }
        return AESHelper.decrypt(encrypted)
    }


    fun getGenerateBaseUrl(): String {
        val stored = PrefManager.getGenerateBaseUrl().trim()
        val resolved = when {
            stored.isBlank() -> GenerateApiConfig.DEFAULT_BASE_URL
            stored.startsWith("http", ignoreCase = true) -> stored
            else -> runCatching { AESHelper.decrypt(stored) }
                .getOrDefault(GenerateApiConfig.DEFAULT_BASE_URL)
        }
        return normalizeGenerateBaseUrl(resolved)
    }

    private fun normalizeGenerateBaseUrl(url: String): String {
        var base = url.trim().trimEnd('/')
        if (base.endsWith("/api/hairstyle", ignoreCase = true)) {
            base = base.dropLast("/api/hairstyle".length).trimEnd('/')
        }
        return base.ifBlank { GenerateApiConfig.DEFAULT_BASE_URL }
    }

    fun buildGenerateApiUrl(endpoint: String): String {
        return GenerateApiConfig.buildUrl(getGenerateBaseUrl(), endpoint)
    }

    fun getPrivacyPolicyUrl(): String {
        val stored = PrefManager.getPrivacyPolicyUrl().trim()
        if (stored.isBlank()) return ""
        if (stored.startsWith("http", ignoreCase = true)) return stored
        return runCatching { AESHelper.decrypt(stored).trim() }.getOrDefault("").trim()
    }

    /**
     * Plain feedback API base URL from Firebase `APi.feedback_url` (no encrypt/decrypt).
     * Example: http://139.59.79.157:3002
     */
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
        @SerializedName(RemoteConfigKeys.PREMIUM_PLAN_DETAILS)
        val premiumPlanDetails: PremiumPlanDetailsRoot? = null,
        @SerializedName("update_app") val updateApp: InAppUpdateConfig? = null
    )

    data class ApiSection(
        @SerializedName("base_url") val baseUrl: String? = null,
        @SerializedName("videos_url") val videosUrl: String? = null,
        /** Encrypted or plain root for generate APIs (from Firebase). */
        @SerializedName("generate_base_url") val generateBaseUrl: String? = null,
        /** Encrypted privacy policy page URL */
        @SerializedName("privacy_policy_url") val privacyPolicyUrl: String? = null,
        /** Plain feedback API base URL (no encrypt), e.g. http://139.59.79.157:3002 */
        @SerializedName("feedback_url") val feedbackUrl: String? = null,
        @SerializedName("page_limit") val pageLimit: Int? = null
    )
}

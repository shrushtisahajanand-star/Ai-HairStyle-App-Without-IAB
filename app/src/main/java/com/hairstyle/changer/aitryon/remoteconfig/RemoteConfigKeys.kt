package com.hairstyle.changer.aitryon.remoteconfig

import com.hairstyle.changer.aitryon.BuildConfig

object RemoteConfigKeys {

    private const val FILE_PREFIX = "ai_hairstyle_version_"

//    const val JSON_API_OBJECT = "APi"
//    const val BASE_URL = "base_url"
//    const val VIDEOS_URL = "videos_url"
//    const val GENERATE_BASE_URL = "generate_base_url"
//    const val PRIVACY_POLICY_URL = "privacy_policy_url"
//    const val PAGE_LIMIT = "page_limit"

    const val DEFAULT_PAGE_LIMIT = 20

    fun remoteConfigFileName(): String {
        val versionSuffix = BuildConfig.VERSION_NAME.replace('.', '_')
        return FILE_PREFIX + versionSuffix
    }
}

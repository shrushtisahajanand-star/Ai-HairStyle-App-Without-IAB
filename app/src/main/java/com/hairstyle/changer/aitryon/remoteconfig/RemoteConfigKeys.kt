package com.hairstyle.changer.aitryon.remoteconfig

import com.hairstyle.changer.aitryon.BuildConfig
import com.hairstyle.changer.aitryon.utils.AESHelper

object RemoteConfigKeys {

    private const val ENCRYPTED_FILE_PREFIX =
        "U2FsdGVkX18ab5RiNe+Tbw/27l9ilrN2BFtFqeMFczY+kBAZmySminRQLvzvDTL8"

//    const val JSON_API_OBJECT = "APi"
//    const val BASE_URL = "base_url"
//    const val VIDEOS_URL = "videos_url"
//    const val GENERATE_BASE_URL = "generate_base_url"
//    const val PRIVACY_POLICY_URL = "privacy_policy_url"
//    const val PAGE_LIMIT = "page_limit"
    const val PREMIUM_PLAN_DETAILS = "premium_plan_detils"

    const val DEFAULT_PAGE_LIMIT = 20

    fun remoteConfigFileName(): String {
        val prefix = AESHelper.decrypt(ENCRYPTED_FILE_PREFIX)
        val versionSuffix = BuildConfig.VERSION_NAME.replace('.', '_')
        return prefix + versionSuffix
    }
}

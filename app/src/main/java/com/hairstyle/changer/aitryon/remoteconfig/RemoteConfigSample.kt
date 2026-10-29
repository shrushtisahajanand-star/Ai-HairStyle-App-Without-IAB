//package com.hairstyle.changer.aitryon.data.remoteconfig
//
///**
// * Firebase Remote Config — version 1.0
// *
// * Parameter key: `ai_hairstyle_version_1_0`
// *
// * ```json
// * {
// *   "APi": {
// *     "base_url": "U2FsdGVkX18sbTYBkGuK1+1cfN7fhMmaVqX2B1XOlyhG6Z5iACvo9ZuqrUODVEJzrGyKzH38EVRlxFmXj3t3Kw==",
// *     "videos_url": "U2FsdGVkX1+qQdu585Krj+F35FJQgxPgCWtILi4QMv7xKeCFWQ2f3dsZC6OSzMT3v9rFNvKPe8N76wdKOgKMuA==",
// *     "generate_base_url": "U2FsdGVkX1/rZxxq0BOBBln8UThl/Waue62Hxgwq1i158KvhpU1CoY6TAryCW0Q9TEXjflrX6hDGdHS1ndWsmQ==",
// *     "privacy_policy_url": "<AES encrypt: https://hair.champslevl.com/privacy-policy>",
// *     "page_limit": 20
// *   },
// *   "premium_plan_detils": {
// *     "year_price": "$49.99",
// *     "month_price": "$9.99",
// *     "week_price": "$4.99",
// *     "free_user_price": "$0"
// *   },
// *   "ads": { ... },
// *   "consent_form_show_screen": "splash",
// *   "Ads_Loading_Types": "PreLoad"
// * }
// * ```
// *
// * Plain values:
// * - base_url            → https://hair.champslevl.com/api/images
// * - videos_url          → https://hair.champslevl.com/api/videos
// * - generate_base_url   → (AES encrypted in Firebase; see ENCRYPTED_GENERATE_BASE_URL)
// *   (final call)        → {generate_base}/api/hairstyle
// * - privacy_policy_url  → https://hair.champslevl.com/privacy-policy
// *
// * generate_base_url can be plain https or AES-encrypted.
// * Re-encrypt anytime: `AESHelper.encrypt("your_url")`
// */
//object RemoteConfigSample {
//    const val PLAIN_BASE_URL = "https://hair.champslevl.com/api/images"
//    const val PLAIN_VIDEOS_URL = "https://hair.champslevl.com/api/videos"
//    // Plain URLs only for local encrypt tooling — do not ship in release comments as active config.
//    // const val PLAIN_GENERATE_BASE_URL = "..."
//    // const val PLAIN_HAIRSTYLE_API_URL = "..."
//    const val PLAIN_PRIVACY_POLICY_URL = "https://hair.champslevl.com/privacy-policy"
//    const val ENCRYPTED_BASE_URL =
//        "U2FsdGVkX18sbTYBkGuK1+1cfN7fhMmaVqX2B1XOlyhG6Z5iACvo9ZuqrUODVEJzrGyKzH38EVRlxFmXj3t3Kw=="
//    const val ENCRYPTED_VIDEOS_URL =
//        "U2FsdGVkX1+qQdu585Krj+F35FJQgxPgCWtILi4QMv7xKeCFWQ2f3dsZC6OSzMT3v9rFNvKPe8N76wdKOgKMuA=="
//    const val ENCRYPTED_GENERATE_BASE_URL =
//        "U2FsdGVkX1/rZxxq0BOBBln8UThl/Waue62Hxgwq1i158KvhpU1CoY6TAryCW0Q9TEXjflrX6hDGdHS1ndWsmQ=="
//    const val DEFAULT_PAGE_LIMIT = 20
//}

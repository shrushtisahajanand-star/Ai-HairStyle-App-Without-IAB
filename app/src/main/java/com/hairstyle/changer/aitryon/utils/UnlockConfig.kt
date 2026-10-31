package com.hairstyle.changer.aitryon.utils

import org.json.JSONArray
import org.json.JSONObject

/**
 * Locked-style settings. App defaults below; Firebase `ads.unlock_items` overrides them:
 *
 * ```json
 * "unlock_items": {
 *   "unlock_ads_show": true,
 *   "free_home": 5, "free_fun": 3, "free_beard": 3, "free_hair_color": 3,
 *   "reward_ad_id": "...", "inter_ad_id": "...",
 *   "ads_load_type": "PreLoad",
 *   "ads_sequence": [["reward", 2], ["inter", 1]]
 * }
 * ```
 *
 * Items at position < free count are free; the rest need [ads_sequence] on every click.
 */
object UnlockConfig {

    enum class Group(val key: String, val defaultFree: Int) {
        HOME("free_home", 5),
        FUN("free_fun", 3),
        BEARD("free_beard", 3),
        HAIR_COLOR("free_hair_color", 3)
    }

    enum class AdKind { REWARD, INTER }

    private const val KEY_UNLOCK_ADS_SHOW = "unlock_ads_show"
    private const val KEY_ADS_LOAD_TYPE = "ads_load_type"
    private const val KEY_ADS_SEQUENCE = "ads_sequence"
    private const val MAX_STEP_COUNT = 10

    private val DEFAULT_SEQUENCE = listOf(AdKind.REWARD)

    private var cachedRaw: String? = null
    private var cachedJson: JSONObject? = null

    /**
     * Master switch (`unlock_ads_show`). `true` → locks + unlock ads; `false` / missing →
     * the app works as before (no locks, old click / generate ads).
     */
    fun unlockAdsShow(): Boolean = json()?.optBoolean(KEY_UNLOCK_ADS_SHOW, false) ?: false

    fun freeCount(group: Group): Int {
        val value = json()?.optInt(group.key, -1) ?: -1
        return if (value >= 0) value else group.defaultFree
    }

    /** Whole sequence, expanded: `[["reward",2],["inter",1]]` → REWARD, REWARD, INTER. */
    fun adSequence(): List<AdKind> {
        val array = json()?.optJSONArray(KEY_ADS_SEQUENCE) ?: return DEFAULT_SEQUENCE
        val result = ArrayList<AdKind>()
        for (i in 0 until array.length()) {
            appendStep(array.opt(i), result)
        }
        return result.ifEmpty { DEFAULT_SEQUENCE }
    }

    /**
     * Unlock ads' own load type (`ads_load_type`): `PreLoad` = cache the next ad in advance,
     * anything else / missing = `Load` (load when the user taps Watch Ad).
     * Independent of `Ads_Loading_Types` and `inter_and_reward`.
     */
    fun isPreload(): Boolean {
        val type = json()?.optString(KEY_ADS_LOAD_TYPE, "").orEmpty().trim()
        return type.equals("PreLoad", ignoreCase = true)
    }

    private fun appendStep(step: Any?, out: MutableList<AdKind>) {
        val type: String
        val count: Int
        when (step) {
            is JSONArray -> {
                type = step.optString(0)
                count = step.optInt(1, 1)
            }
            is String -> {
                type = step
                count = 1
            }
            else -> return
        }
        val kind = when (type.trim().lowercase()) {
            "reward", "rewarded" -> AdKind.REWARD
            "inter", "interstitial" -> AdKind.INTER
            else -> return
        }
        repeat(count.coerceIn(1, MAX_STEP_COUNT)) { out.add(kind) }
    }

    private fun json(): JSONObject? {
        val raw = PrefManager.getUnlockItemsJson()
        if (raw == cachedRaw) return cachedJson
        cachedRaw = raw
        cachedJson = if (raw.isBlank()) null else runCatching { JSONObject(raw) }.getOrNull()
        return cachedJson
    }
}

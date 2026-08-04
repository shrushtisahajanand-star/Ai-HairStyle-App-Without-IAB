package com.hairstyle.generator.aimakerover.data.model

import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonDeserializer
import com.google.gson.JsonElement
import com.google.gson.JsonNull
import com.google.gson.JsonPrimitive
import com.google.gson.JsonSerializationContext
import com.google.gson.JsonSerializer
import com.google.gson.annotations.JsonAdapter
import com.google.gson.annotations.SerializedName
import java.lang.reflect.Type
import java.util.Locale

/**
 * Firebase `premium_plan_detils` — **current** shape (prices only):
 * ```
 * {
 *   "year_price": "$49.99",
 *   "month_price": "$9.99",
 *   "week_price": "$4.99",
 *   "free_user_price": "$0"
 * }
 * ```
 *
 * Nested `year` / `month` / `week` / `free_user` objects (with `image_access`) are kept
 * only so older cached JSON still parses; client no longer enforces image_access.
 */
data class PremiumPlanDetailsRoot(
    @SerializedName("year_price")
    @JsonAdapter(FlexibleStringAdapter::class)
    val yearPrice: String? = null,
    @SerializedName("month_price")
    @JsonAdapter(FlexibleStringAdapter::class)
    val monthPrice: String? = null,
    @SerializedName("week_price")
    @JsonAdapter(FlexibleStringAdapter::class)
    val weekPrice: String? = null,
    @SerializedName("free_user_price")
    @JsonAdapter(FlexibleStringAdapter::class)
    val freeUserPrice: String? = null,

    // region OLD nested shape (image_access / monthly_add) — kept for legacy prefs only
    @SerializedName("year") val year: PremiumPlanItem? = null,
    @SerializedName("month") val month: PremiumPlanItem? = null,
    @SerializedName("week") val week: PremiumPlanItem? = null,
    @SerializedName("free_user") val freeUser: PremiumPlanItem? = null
    // endregion
)

data class PremiumPlanItem(
    @SerializedName("price")
    @JsonAdapter(FlexibleStringAdapter::class)
    val price: String? = null,
    @SerializedName("discount")
    @JsonAdapter(FlexibleStringAdapter::class)
    val discount: String? = null,
    @SerializedName("days") val days: Int = 0, // ignored for premium — use PremiumPlanManager.planDays
    // OLD: client image quota (disabled — backend free-limit API will own this)
    @SerializedName("image_access") val imageAccess: Int = 0,
    @SerializedName("monthly_add") val monthlyAdd: Int = 0,
    @SerializedName("ads_show") val adsShow: Boolean = true, // ignored for premium — premium never shows ads
    @SerializedName("product_id") val productId: String? = null
) {
    fun priceValue(): Double = parseNumeric(price)

    fun discountValue(): Double = parseNumeric(discount)

    fun displayPrice(): String {
        val raw = price?.trim().orEmpty()
        if (raw.isBlank()) return ""
        if (raw.startsWith("$")) return raw
        return formatUsd(priceValue())
    }

    private fun formatUsd(value: Double): String {
        return if (value % 1.0 == 0.0) {
            "$${value.toInt()}"
        } else {
            String.format(Locale.US, "$%.2f", value)
        }
    }

    companion object {
        fun parseNumeric(raw: String?): Double {
            if (raw.isNullOrBlank()) return 0.0
            val cleaned = raw
                .replace("$", "")
                .replace("%", "")
                .replace("OFF", "", ignoreCase = true)
                .trim()
            // Prefer the first number token (e.g. "27% OFF" → 27)
            val match = Regex("""-?\d+(\.\d+)?""").find(cleaned)?.value
            return (match ?: cleaned).toDoubleOrNull() ?: 0.0
        }
    }
}

/**
 * Reads `price` / `discount` whether Firebase sends a string or a number.
 *
 * Serialization must be implemented too: with only a [JsonDeserializer], Gson writes the
 * String reflectively and turns `"$4.99"` into `{"count":10}`, which then fails to read back.
 */
class FlexibleStringAdapter : JsonDeserializer<String>, JsonSerializer<String> {
    override fun deserialize(
        json: JsonElement,
        typeOfT: Type,
        context: JsonDeserializationContext
    ): String {
        if (!json.isJsonPrimitive) return ""
        val primitive = json.asJsonPrimitive
        return if (primitive.isString) primitive.asString else primitive.toString()
    }

    override fun serialize(
        src: String?,
        typeOfSrc: Type,
        context: JsonSerializationContext
    ): JsonElement {
        return if (src == null) JsonNull.INSTANCE else JsonPrimitive(src)
    }
}

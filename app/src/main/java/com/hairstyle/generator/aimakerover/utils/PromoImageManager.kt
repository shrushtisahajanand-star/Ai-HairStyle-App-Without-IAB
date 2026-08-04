package com.hairstyle.generator.aimakerover.utils

import com.google.gson.Gson
import com.hairstyle.generator.aimakeover.R
import com.hairstyle.generator.aimakerover.domain.model.PromoImageItem

data class PromoSheetContent(
    val imageUrl: String,
    val badgeRes: Int,
    val title: String,
    val subtitle: String
)

object PromoImageManager {

    private val gson = Gson()

    fun getRandomPromoContent(): PromoSheetContent? {
        val isMale = UserGenderManager.isMale()
        val items = loadPromoImages().filter { item ->
            if (isMale) {
                item.title.lowercase().startsWith(VideoTitleKeys.MALE_IMAGE_PREFIX)
            } else {
                item.title.lowercase().startsWith(VideoTitleKeys.FEMALE_IMAGE_PREFIX)
            }
        }
        if (items.isEmpty()) return null

        val selected = items.random()
        return PromoSheetContent(
            imageUrl = selected.imageUrl,
            badgeRes = if (isMale) {
                R.string.promo_sheet_badge_male
            } else {
                R.string.promo_sheet_badge_female
            },
            title = selected.header.ifBlank {
                if (isMale) "Modern Fade Cut" else "Sleek High Ponytail"
            },
            subtitle = selected.description.ifBlank {
                "Explore modern haircut styles before your next look"
            }
        )
    }

    private fun loadPromoImages(): List<PromoImageItem> {
        val json = PrefManager.getPromoImagesJson()
        if (json.isBlank()) return emptyList()
        return runCatching {
            gson.fromJson(json, Array<PromoImageItem>::class.java)?.toList().orEmpty()
        }.getOrDefault(emptyList())
    }
}

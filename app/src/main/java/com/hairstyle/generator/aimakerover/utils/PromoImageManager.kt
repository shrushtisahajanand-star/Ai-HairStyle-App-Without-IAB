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
        val allItems = loadPromoImages()
        if (allItems.isEmpty()) return null

        val items = if (UserGenderManager.isMale()) {
            allItems.filter { item ->
                item.title.lowercase().startsWith(VideoTitleKeys.MALE_IMAGE_PREFIX)
            }.ifEmpty { allItems }
        } else {
            allItems
        }

        val selected = items.random()
        val selectedIsMale = selected.title.lowercase()
            .startsWith(VideoTitleKeys.MALE_IMAGE_PREFIX)
        return PromoSheetContent(
            imageUrl = selected.imageUrl,
            badgeRes = if (selectedIsMale) {
                R.string.promo_sheet_badge_male
            } else {
                R.string.promo_sheet_badge_female
            },
            title = selected.header.ifBlank {
                if (selectedIsMale) "Modern Fade Cut" else "Sleek High Ponytail"
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

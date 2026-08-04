package com.hairstyle.generator.aimakerover.data.model

enum class ImageApiCategory(
    val category: String,
    val subcategory: String? = null
) {
    HOME_MEN(category = "home", subcategory = "men_hairstyles"),
    HOME_WOMEN(category = "home", subcategory = "women_hairstyles"),
    HOME_CHILDREN(category = "home", subcategory = "children_hairstyles"),
    HOME_HAIR_COLOR(category = "home", subcategory = "haircolor"),
    FUN_MEN(category = "fun", subcategory = "men"),
    FUN_WOMEN(category = "fun", subcategory = "women"),
    FUN_BABY(category = "fun", subcategory = "baby"),
    BEARD(category = "beard")
}

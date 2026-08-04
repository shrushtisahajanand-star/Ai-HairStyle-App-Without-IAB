package com.hairstyle.generator.aimakerover.data.model

import com.google.gson.annotations.SerializedName
import com.hairstyle.generator.aimakerover.domain.model.BeardStyle
import com.hairstyle.generator.aimakerover.domain.model.CategoryHairstyle
import com.hairstyle.generator.aimakerover.domain.model.FunStyle

data class CategoryImagesResponse(
    @SerializedName("success") val success: Boolean = false,
    @SerializedName("category") val category: String? = null,
    @SerializedName("page") val page: Int = 1,
    @SerializedName("limit") val limit: Int = 0,
    @SerializedName("totalCount") val totalCount: Int = 0,
    @SerializedName("totalPages") val totalPages: Int = 0,
    @SerializedName("count") val count: Int = 0,
    @SerializedName("images") val images: List<CategoryImageDto> = emptyList()
)

data class CategoryImageDto(
    @SerializedName("id") val id: Int = 0,
    @SerializedName("title") val title: String? = null,
    @SerializedName("url") val url: String? = null,
    @SerializedName("image") val image: String? = null,
    @SerializedName("value") val value: String? = null,
    @SerializedName("hairstyle") val hairstyle: String? = null,
    @SerializedName("hairColor") val hairColor: String? = null,
    @SerializedName("description") val description: String? = null,
    @SerializedName("beforeImage") val beforeImage: String? = null,
    @SerializedName("afterImage") val afterImage: String? = null
)

data class PagedCategoryImages(
    val items: List<CategoryHairstyle>,
    val page: Int,
    val totalPages: Int,
    val hasMore: Boolean
)

data class PagedBeardStyles(
    val items: List<BeardStyle>,
    val page: Int,
    val totalPages: Int,
    val hasMore: Boolean
)

data class PagedFunStyles(
    val items: List<FunStyle>,
    val page: Int,
    val totalPages: Int,
    val hasMore: Boolean
)

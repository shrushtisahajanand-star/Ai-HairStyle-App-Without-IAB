package com.hairstyle.generator.aimakerover.data.repository

import androidx.core.net.toUri
import com.google.gson.Gson
import com.hairstyle.generator.aimakerover.data.model.CategoryImageDto
import com.hairstyle.generator.aimakerover.data.model.CategoryImagesResponse
import com.hairstyle.generator.aimakerover.data.model.ImageApiCategory
import com.hairstyle.generator.aimakerover.data.model.PagedBeardStyles
import com.hairstyle.generator.aimakerover.data.model.PagedCategoryImages
import com.hairstyle.generator.aimakerover.data.model.PagedFunStyles
import com.hairstyle.generator.aimakerover.data.remoteconfig.RemoteConfigManager
import com.hairstyle.generator.aimakerover.domain.model.BeardStyle
import com.hairstyle.generator.aimakerover.domain.model.CategoryHairstyle
import com.hairstyle.generator.aimakerover.domain.model.FunStyle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CategoryImagesRepository @Inject constructor(
    private val remoteConfigManager: RemoteConfigManager,
    private val okHttpClient: OkHttpClient,
    private val gson: Gson
) {

    suspend fun loadMenHairstyles(
        page: Int = 1,
        limit: Int = remoteConfigManager.getPageLimit()
    ): PagedCategoryImages = withContext(Dispatchers.IO) {
        loadImages(ImageApiCategory.HOME_MEN, page, limit)
    }

    suspend fun loadWomenHairstyles(
        page: Int = 1,
        limit: Int = remoteConfigManager.getPageLimit()
    ): PagedCategoryImages = withContext(Dispatchers.IO) {
        loadImages(ImageApiCategory.HOME_WOMEN, page, limit)
    }

    suspend fun loadChildrenHairstyles(
        page: Int = 1,
        limit: Int = remoteConfigManager.getPageLimit()
    ): PagedCategoryImages = withContext(Dispatchers.IO) {
        loadImages(ImageApiCategory.HOME_CHILDREN, page, limit)
    }

    suspend fun loadHairColors(
        page: Int = 1,
        limit: Int = remoteConfigManager.getPageLimit()
    ): PagedCategoryImages = withContext(Dispatchers.IO) {
        loadImages(ImageApiCategory.HOME_HAIR_COLOR, page, limit)
    }

    suspend fun loadBeardStyles(
        page: Int = 1,
        limit: Int = remoteConfigManager.getPageLimit()
    ): PagedBeardStyles = withContext(Dispatchers.IO) {
        loadBeardStylesPage(ImageApiCategory.BEARD, page, limit)
    }

    suspend fun loadFunMenStyles(
        page: Int = 1,
        limit: Int = remoteConfigManager.getPageLimit()
    ): PagedFunStyles = withContext(Dispatchers.IO) {
        loadFunStylesPage(ImageApiCategory.FUN_MEN, page, limit)
    }

    suspend fun loadFunWomenStyles(
        page: Int = 1,
        limit: Int = remoteConfigManager.getPageLimit()
    ): PagedFunStyles = withContext(Dispatchers.IO) {
        loadFunStylesPage(ImageApiCategory.FUN_WOMEN, page, limit)
    }

    suspend fun loadFunBabyStyles(
        page: Int = 1,
        limit: Int = remoteConfigManager.getPageLimit()
    ): PagedFunStyles = withContext(Dispatchers.IO) {
        loadFunStylesPage(ImageApiCategory.FUN_BABY, page, limit)
    }

    private fun loadFunStylesPage(
        imageCategory: ImageApiCategory,
        page: Int,
        limit: Int
    ): PagedFunStyles {
        val apiUrl = buildImagesApiUrl(imageCategory, page, limit)
        val request = Request.Builder().url(apiUrl).get().build()
        okHttpClient.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                error("Failed to load fun styles: HTTP ${response.code}")
            }
            val body = response.body?.string().orEmpty()
            val parsed = gson.fromJson(body, CategoryImagesResponse::class.java)
            if (!parsed.success) {
                error("Failed to load fun styles: API returned success=false")
            }

            val originBaseUrl = apiUrl.toOriginBaseUrl()
            val items = parsed.images.mapNotNull { dto ->
                mapDtoToFunStyle(dto, originBaseUrl)
            }

            val totalPages = parsed.totalPages.takeIf { it > 0 }
                ?: if (parsed.totalCount > 0 && limit > 0) {
                    (parsed.totalCount + limit - 1) / limit
                } else {
                    1
                }
            val currentPage = parsed.page.takeIf { it > 0 } ?: page

            return PagedFunStyles(
                items = items,
                page = currentPage,
                totalPages = totalPages,
                hasMore = currentPage < totalPages
            )
        }
    }

    private fun mapDtoToFunStyle(
        dto: CategoryImageDto,
        originBaseUrl: String
    ): FunStyle? {
        val beforeRelative = dto.beforeImage ?: return null
        val afterRelative = dto.afterImage ?: return null
        val hairstyleApiValue = (dto.value ?: dto.hairstyle)?.trim().orEmpty()
            .takeIf { it.isNotBlank() } ?: return null
        val hairColorApiValue = dto.hairColor?.trim().orEmpty()
            .takeIf { it.isNotBlank() } ?: return null
        val title = dto.title?.trim().orEmpty().ifBlank { hairstyleApiValue }

        return FunStyle(
            id = dto.id,
            title = title,
            description = dto.description?.trim().orEmpty(),
            hairstyleApiValue = hairstyleApiValue,
            hairColorApiValue = hairColorApiValue,
            beforeImageUrl = resolveImageUrl(originBaseUrl, beforeRelative),
            afterImageUrl = resolveImageUrl(originBaseUrl, afterRelative)
        )
    }

    private fun loadBeardStylesPage(
        imageCategory: ImageApiCategory,
        page: Int,
        limit: Int
    ): PagedBeardStyles {
        val apiUrl = buildImagesApiUrl(imageCategory, page, limit)
        val request = Request.Builder().url(apiUrl).get().build()
        okHttpClient.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                error("Failed to load beard styles: HTTP ${response.code}")
            }
            val body = response.body?.string().orEmpty()
            val parsed = gson.fromJson(body, CategoryImagesResponse::class.java)
            if (!parsed.success) {
                error("Failed to load beard styles: API returned success=false")
            }

            val originBaseUrl = apiUrl.toOriginBaseUrl()
            val items = parsed.images.mapNotNull { dto ->
                mapDtoToBeardStyle(dto, originBaseUrl)
            }

            val totalPages = parsed.totalPages.takeIf { it > 0 }
                ?: if (parsed.totalCount > 0 && limit > 0) {
                    (parsed.totalCount + limit - 1) / limit
                } else {
                    1
                }
            val currentPage = parsed.page.takeIf { it > 0 } ?: page

            return PagedBeardStyles(
                items = items,
                page = currentPage,
                totalPages = totalPages,
                hasMore = currentPage < totalPages
            )
        }
    }

    private fun mapDtoToBeardStyle(
        dto: CategoryImageDto,
        originBaseUrl: String
    ): BeardStyle? {
        val beforeRelative = dto.beforeImage ?: return null
        val afterRelative = dto.afterImage ?: return null
        val apiValue = dto.value?.trim().orEmpty()
            .ifBlank { dto.title?.trim().orEmpty() }
            .takeIf { it.isNotBlank() } ?: return null
        val title = dto.title?.trim().orEmpty().ifBlank { apiValue }

        return BeardStyle(
            id = dto.id,
            title = title,
            description = dto.description?.trim().orEmpty(),
            apiValue = apiValue,
            beforeImageUrl = resolveImageUrl(originBaseUrl, beforeRelative),
            afterImageUrl = resolveImageUrl(originBaseUrl, afterRelative)
        )
    }

    private fun loadImages(
        imageCategory: ImageApiCategory,
        page: Int,
        limit: Int
    ): PagedCategoryImages {
        val apiUrl = buildImagesApiUrl(imageCategory, page, limit)
        val request = Request.Builder().url(apiUrl).get().build()
        okHttpClient.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                error("Failed to load images: HTTP ${response.code}")
            }
            val body = response.body?.string().orEmpty()
            val parsed = gson.fromJson(body, CategoryImagesResponse::class.java)
            if (!parsed.success) {
                error("Failed to load images: API returned success=false")
            }

            val originBaseUrl = apiUrl.toOriginBaseUrl()
            val items = parsed.images.mapNotNull { dto ->
                mapDtoToCategoryHairstyle(dto, imageCategory, originBaseUrl)
            }

            val totalPages = parsed.totalPages.takeIf { it > 0 }
                ?: if (parsed.totalCount > 0 && limit > 0) {
                    (parsed.totalCount + limit - 1) / limit
                } else {
                    1
                }
            val currentPage = parsed.page.takeIf { it > 0 } ?: page

            return PagedCategoryImages(
                items = items,
                page = currentPage,
                totalPages = totalPages,
                hasMore = currentPage < totalPages
            )
        }
    }

    private fun mapDtoToCategoryHairstyle(
        dto: CategoryImageDto,
        imageCategory: ImageApiCategory,
        originBaseUrl: String
    ): CategoryHairstyle? {
        val relative = dto.image ?: dto.url ?: return null
        val apiValue = resolveApiValue(dto, imageCategory) ?: return null
        val title = dto.title?.trim().orEmpty().ifBlank { apiValue }
        val fullUrl = resolveImageUrl(originBaseUrl, relative)

        return CategoryHairstyle(
            id = dto.id,
            title = title,
            apiValue = apiValue,
            imageUrl = fullUrl
        )
    }

    private fun resolveApiValue(
        dto: CategoryImageDto,
        imageCategory: ImageApiCategory
    ): String? {
        val value = when (imageCategory) {
            ImageApiCategory.HOME_HAIR_COLOR -> dto.hairColor ?: dto.value
            else -> dto.value ?: dto.hairstyle
        }?.trim().orEmpty()
        return value.takeIf { it.isNotBlank() }
    }

    private fun buildImagesApiUrl(
        imageCategory: ImageApiCategory,
        page: Int,
        limit: Int
    ): String {
        val baseUrl = remoteConfigManager.getImagesBaseUrl()
        val httpUrl = baseUrl.toHttpUrlOrNull()
            ?: error("Invalid base_url from Remote Config: $baseUrl")

        val builder = httpUrl.newBuilder()
            .addQueryParameter("category", imageCategory.category)
            .addQueryParameter("page", page.toString())
            .addQueryParameter("limit", limit.toString())

        imageCategory.subcategory?.let { subcategory ->
            builder.addQueryParameter("subcategory", subcategory)
        }

        return builder.build().toString()
    }

    private fun resolveImageUrl(originBaseUrl: String, relativePath: String): String {
        if (relativePath.startsWith("http")) return relativePath
        return originBaseUrl.trimEnd('/') + "/" + relativePath.trimStart('/')
    }

    private fun String.toOriginBaseUrl(): String {
        val uri = toUri()
        val port = if (uri.port != -1) ":${uri.port}" else ""
        return "${uri.scheme}://${uri.host}$port"
    }
}

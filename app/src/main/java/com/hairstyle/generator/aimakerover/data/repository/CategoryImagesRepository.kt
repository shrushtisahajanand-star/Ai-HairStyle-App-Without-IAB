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
        loadImages(LOG_MEN, ImageApiCategory.HOME_MEN, page, limit)
    }

    suspend fun loadWomenHairstyles(
        page: Int = 1,
        limit: Int = remoteConfigManager.getPageLimit()
    ): PagedCategoryImages = withContext(Dispatchers.IO) {
        loadImages(LOG_WOMEN, ImageApiCategory.HOME_WOMEN, page, limit)
    }

    suspend fun loadChildrenHairstyles(
        page: Int = 1,
        limit: Int = remoteConfigManager.getPageLimit()
    ): PagedCategoryImages = withContext(Dispatchers.IO) {
        loadImages(LOG_CHILDREN, ImageApiCategory.HOME_CHILDREN, page, limit)
    }

    suspend fun loadHairColors(
        page: Int = 1,
        limit: Int = remoteConfigManager.getPageLimit()
    ): PagedCategoryImages = withContext(Dispatchers.IO) {
        loadImages(LOG_HAIR_COLOR, ImageApiCategory.HOME_HAIR_COLOR, page, limit)
    }

    suspend fun loadBeardStyles(
        page: Int = 1,
        limit: Int = remoteConfigManager.getPageLimit()
    ): PagedBeardStyles = withContext(Dispatchers.IO) {
        loadBeardStylesPage(LOG_BEARD, ImageApiCategory.BEARD, page, limit)
    }

    suspend fun loadFunMenStyles(
        page: Int = 1,
        limit: Int = remoteConfigManager.getPageLimit()
    ): PagedFunStyles = withContext(Dispatchers.IO) {
        loadFunStylesPage(LOG_FUN_MEN, ImageApiCategory.FUN_MEN, page, limit)
    }

    suspend fun loadFunWomenStyles(
        page: Int = 1,
        limit: Int = remoteConfigManager.getPageLimit()
    ): PagedFunStyles = withContext(Dispatchers.IO) {
        loadFunStylesPage(LOG_FUN_WOMEN, ImageApiCategory.FUN_WOMEN, page, limit)
    }

    suspend fun loadFunBabyStyles(
        page: Int = 1,
        limit: Int = remoteConfigManager.getPageLimit()
    ): PagedFunStyles = withContext(Dispatchers.IO) {
        loadFunStylesPage(LOG_FUN_BABY, ImageApiCategory.FUN_BABY, page, limit)
    }

    private data class FetchedImages(
        val parsed: CategoryImagesResponse,
        val originBaseUrl: String
    )

    private fun fetchImages(label: String, apiUrl: String, baseUrl: String): FetchedImages {
        CategoryImagesLogger.request(label, apiUrl, baseUrl)
        val request = Request.Builder()
            .url(apiUrl)
            .header("User-Agent", "AIHairstyleStudio/1.0")
            .get()
            .build()
        okHttpClient.newCall(request).execute().use { response ->
            val body = response.body?.string().orEmpty()
            if (!response.isSuccessful) {
                CategoryImagesLogger.failure(
                    label,
                    "HTTP ${response.code} | bodyLength=${body.length} | bodyPreview=${body.take(200)}"
                )
                error("Failed to load images: HTTP ${response.code}")
            }
            val parsed = runCatching {
                gson.fromJson(body, CategoryImagesResponse::class.java)
            }.getOrElse { parseError ->
                CategoryImagesLogger.failure(label, "JSON parse failed", parseError)
                throw parseError
            }
            CategoryImagesLogger.response(label, response.code, body, parsed)
            if (!parsed.success) {
                CategoryImagesLogger.failure(label, "API returned success=false")
                error("Failed to load images: API returned success=false")
            }
            return FetchedImages(parsed, apiUrl.toOriginBaseUrl())
        }
    }

    private fun loadFunStylesPage(
        label: String,
        imageCategory: ImageApiCategory,
        page: Int,
        limit: Int
    ): PagedFunStyles {
        val baseUrl = remoteConfigManager.getImagesBaseUrl()
        val apiUrl = buildImagesApiUrl(baseUrl, imageCategory, page, limit)
        val fetched = fetchImages(label, apiUrl, baseUrl)
        val skipped = mutableMapOf<String, Int>()
        val sampleUrls = mutableListOf<String>()
        val items = fetched.parsed.images.mapNotNull { dto ->
            mapDtoToFunStyle(label, dto, fetched.originBaseUrl)?.also { style ->
                sampleUrls.add(style.afterImageUrl)
            } ?: run {
                recordSkip(skipped, skipReasonFun(dto))
                CategoryImagesLogger.skip(label, dto, skipReasonFun(dto))
                null
            }
        }
        CategoryImagesLogger.mapped(label, fetched.parsed.images.size, items.size, skipped, sampleUrls)
        return buildFunPage(items, fetched.parsed, page, limit)
    }

    private fun mapDtoToFunStyle(
        label: String,
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
        val beforeUrl = resolveImageUrl(label, dto.id, originBaseUrl, beforeRelative)
        val afterUrl = resolveImageUrl(label, dto.id, originBaseUrl, afterRelative)

        return FunStyle(
            id = dto.id,
            title = title,
            description = dto.description?.trim().orEmpty(),
            hairstyleApiValue = hairstyleApiValue,
            hairColorApiValue = hairColorApiValue,
            beforeImageUrl = beforeUrl,
            afterImageUrl = afterUrl
        )
    }

    private fun loadBeardStylesPage(
        label: String,
        imageCategory: ImageApiCategory,
        page: Int,
        limit: Int
    ): PagedBeardStyles {
        val baseUrl = remoteConfigManager.getImagesBaseUrl()
        val apiUrl = buildImagesApiUrl(baseUrl, imageCategory, page, limit)
        val fetched = fetchImages(label, apiUrl, baseUrl)
        val skipped = mutableMapOf<String, Int>()
        val sampleUrls = mutableListOf<String>()
        val items = fetched.parsed.images.mapNotNull { dto ->
            mapDtoToBeardStyle(label, dto, fetched.originBaseUrl)?.also { style ->
                sampleUrls.add(style.afterImageUrl)
            } ?: run {
                recordSkip(skipped, skipReasonBeard(dto))
                CategoryImagesLogger.skip(label, dto, skipReasonBeard(dto))
                null
            }
        }
        CategoryImagesLogger.mapped(label, fetched.parsed.images.size, items.size, skipped, sampleUrls)
        return buildBeardPage(items, fetched.parsed, page, limit)
    }

    private fun mapDtoToBeardStyle(
        label: String,
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
            beforeImageUrl = resolveImageUrl(label, dto.id, originBaseUrl, beforeRelative),
            afterImageUrl = resolveImageUrl(label, dto.id, originBaseUrl, afterRelative)
        )
    }

    private fun loadImages(
        label: String,
        imageCategory: ImageApiCategory,
        page: Int,
        limit: Int
    ): PagedCategoryImages {
        val baseUrl = remoteConfigManager.getImagesBaseUrl()
        val apiUrl = buildImagesApiUrl(baseUrl, imageCategory, page, limit)
        val fetched = fetchImages(label, apiUrl, baseUrl)
        val skipped = mutableMapOf<String, Int>()
        val sampleUrls = mutableListOf<String>()
        val items = fetched.parsed.images.mapNotNull { dto ->
            mapDtoToCategoryHairstyle(label, dto, imageCategory, fetched.originBaseUrl)?.also { style ->
                sampleUrls.add(style.imageUrl)
            } ?: run {
                recordSkip(skipped, skipReasonHairstyle(dto, imageCategory))
                CategoryImagesLogger.skip(
                    label,
                    dto,
                    skipReasonHairstyle(dto, imageCategory)
                )
                null
            }
        }
        CategoryImagesLogger.mapped(label, fetched.parsed.images.size, items.size, skipped, sampleUrls)
        return buildCategoryPage(items, fetched.parsed, page, limit)
    }

    private fun mapDtoToCategoryHairstyle(
        label: String,
        dto: CategoryImageDto,
        imageCategory: ImageApiCategory,
        originBaseUrl: String
    ): CategoryHairstyle? {
        val relative = dto.image ?: dto.url ?: dto.afterImage ?: return null
        val apiValue = resolveApiValue(dto, imageCategory) ?: return null
        val title = dto.title?.trim().orEmpty().ifBlank { apiValue }
        val fullUrl = resolveImageUrl(label, dto.id, originBaseUrl, relative)

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
            else -> dto.value ?: dto.hairstyle ?: dto.title
        }?.trim().orEmpty()
        return value.takeIf { it.isNotBlank() }
    }

    private fun buildImagesApiUrl(
        baseUrl: String,
        imageCategory: ImageApiCategory,
        page: Int,
        limit: Int
    ): String {
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

    private fun resolveImageUrl(
        label: String,
        dtoId: Int,
        originBaseUrl: String,
        relativePath: String
    ): String {
        val fullUrl = if (relativePath.startsWith("http")) {
            relativePath
        } else {
            originBaseUrl.trimEnd('/') + "/" + relativePath.trimStart('/')
        }
        CategoryImagesLogger.resolvedUrl(label, dtoId, relativePath, fullUrl)
        return fullUrl
    }

    private fun String.toOriginBaseUrl(): String {
        val uri = toUri()
        val port = if (uri.port != -1) ":${uri.port}" else ""
        return "${uri.scheme}://${uri.host}$port"
    }

    private fun recordSkip(skipped: MutableMap<String, Int>, reason: String) {
        skipped[reason] = (skipped[reason] ?: 0) + 1
    }

    private fun skipReasonHairstyle(dto: CategoryImageDto, category: ImageApiCategory): String {
        if (dto.image.isNullOrBlank() && dto.url.isNullOrBlank() && dto.afterImage.isNullOrBlank()) {
            return "missing_image_url_afterImage"
        }
        if (resolveApiValue(dto, category) == null) return "missing_api_value"
        return "unknown"
    }

    private fun skipReasonBeard(dto: CategoryImageDto): String = when {
        dto.beforeImage.isNullOrBlank() -> "missing_beforeImage"
        dto.afterImage.isNullOrBlank() -> "missing_afterImage"
        dto.value.isNullOrBlank() && dto.title.isNullOrBlank() -> "missing_value"
        else -> "unknown"
    }

    private fun skipReasonFun(dto: CategoryImageDto): String = when {
        dto.beforeImage.isNullOrBlank() -> "missing_beforeImage"
        dto.afterImage.isNullOrBlank() -> "missing_afterImage"
        (dto.value ?: dto.hairstyle).isNullOrBlank() -> "missing_hairstyle_value"
        dto.hairColor.isNullOrBlank() -> "missing_hairColor"
        else -> "unknown"
    }

    private fun buildCategoryPage(
        items: List<CategoryHairstyle>,
        parsed: CategoryImagesResponse,
        page: Int,
        limit: Int
    ): PagedCategoryImages {
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

    private fun buildBeardPage(
        items: List<BeardStyle>,
        parsed: CategoryImagesResponse,
        page: Int,
        limit: Int
    ): PagedBeardStyles {
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

    private fun buildFunPage(
        items: List<FunStyle>,
        parsed: CategoryImagesResponse,
        page: Int,
        limit: Int
    ): PagedFunStyles {
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

    companion object {
        private const val LOG_MEN = "home_men"
        private const val LOG_WOMEN = "home_women"
        private const val LOG_CHILDREN = "home_children"
        private const val LOG_HAIR_COLOR = "hair_color"
        private const val LOG_BEARD = "beard"
        private const val LOG_FUN_MEN = "fun_men"
        private const val LOG_FUN_WOMEN = "fun_women"
        private const val LOG_FUN_BABY = "fun_baby"
    }
}

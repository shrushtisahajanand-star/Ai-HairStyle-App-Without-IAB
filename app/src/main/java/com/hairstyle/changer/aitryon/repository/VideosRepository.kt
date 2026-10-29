package com.hairstyle.changer.aitryon.repository

import android.content.Context
import com.google.gson.Gson
import com.hairstyle.changer.aitryon.model.VideoItemDto
import com.hairstyle.changer.aitryon.model.VideosApiResponse
import com.hairstyle.changer.aitryon.remoteconfig.RemoteConfigManager
import com.hairstyle.changer.aitryon.domain.model.HomeHeroSlide
import com.hairstyle.changer.aitryon.domain.model.PromoImageItem
import com.hairstyle.changer.aitryon.utils.AESHelper
import com.hairstyle.changer.aitryon.utils.PrefManager
import com.hairstyle.changer.aitryon.utils.VideoCacheManager
import com.hairstyle.changer.aitryon.utils.VideoTitleKeys
import com.hairstyle.changer.aitryon.utils.VideoUrlManager
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.net.URI
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class VideosRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val remoteConfigManager: RemoteConfigManager,
    private val okHttpClient: OkHttpClient,
    private val gson: Gson
) {
    private val catalogMutex = Mutex()
    private val preloadScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val startedPreloads = ConcurrentHashMap.newKeySet<String>()

    @Volatile
    private var catalogFetched = false

    /**
     * Fetches the videos API JSON and stores URLs in prefs.
     * Does not download video files — use [preloadIntroVideos] / [preloadPremiumVideo] /
     * [preloadHomeVideos] so Splash is not blocked by full media cache.
     */
    suspend fun fetchAndSaveAllVideos() = withContext(Dispatchers.IO) {
        catalogMutex.withLock {
            if (catalogFetched) return@withLock
            fetchAndSaveCatalogLocked()
            catalogFetched = true
        }
    }

    private fun fetchAndSaveCatalogLocked() {
        val apiUrl = remoteConfigManager.getVideosApiUrl()
        require(apiUrl.isNotBlank()) { "videos_url missing from Remote Config" }

        val request = Request.Builder()
            .url(apiUrl)
            .header("User-Agent", "AIHairstyleStudio/1.0")
            .get()
            .build()

        val response = okHttpClient.newCall(request).execute()
        if (!response.isSuccessful) {
            error("Videos API failed (${response.code})")
        }

        val body = response.body?.string().orEmpty()
        require(body.isNotBlank()) { "Empty videos API response" }

        val parsed = gson.fromJson(body, VideosApiResponse::class.java)
        require(parsed.success) { "Videos API returned success=false" }

        val items = buildList {
            parsed.data?.homeVideo?.let(::addAll)
            parsed.data?.introVideo?.let(::addAll)
            parsed.data?.premiumVideo?.let(::addAll)
        }

        items.forEach { item ->
            saveVideoItem(apiUrl, item)
        }

        saveHomeHeroImages(apiUrl, parsed.data?.homeImages.orEmpty())

        val promoSources = buildList {
            parsed.data?.introImages?.let(::addAll)
            parsed.data?.femaleImages?.let(::addAll)
            parsed.data?.maleImages?.let(::addAll)
            addAll(parsed.data?.homeImages.orEmpty().filter { isPromoImageTitle(it.title) })
        }
        savePromoImages(apiUrl, promoSources)
    }

    fun preloadIntroVideos() {
        preloadInBackground(
            VideoTitleKeys.INTRO_VIDEO_1,
            VideoTitleKeys.INTRO_VIDEO_2,
            VideoTitleKeys.INTRO_VIDEO_3
        )
    }

    fun preloadPremiumVideo() {
        preloadInBackground(VideoTitleKeys.PREMIUM_SCREEN)
    }

    fun preloadHomeVideos() {
        preloadInBackground(
            VideoTitleKeys.HOME_VIDEO_1,
            VideoTitleKeys.HOME_VIDEO_2,
            VideoTitleKeys.HOME_VIDEO_3
        )
    }

    /**
     * Waits for this title's file (reuses an in-flight download).
     * Fetch catalog first if the URL is not in prefs yet.
     */
    suspend fun ensureCached(videoTitleKey: String) = withContext(Dispatchers.IO) {
        if (videoTitleKey.isBlank()) return@withContext
        if (!VideoUrlManager.hasVideoUrl(videoTitleKey)) {
            runCatching { fetchAndSaveAllVideos() }
        }
        val url = VideoUrlManager.getVideoUrl(videoTitleKey)?.takeIf { it.isNotBlank() }
            ?: return@withContext
        VideoCacheManager.cacheVideo(context, okHttpClient, videoTitleKey, url)
    }

    private fun preloadInBackground(vararg videoTitleKeys: String) {
        val keys = videoTitleKeys.distinct().filter { startedPreloads.add(it) }
        if (keys.isEmpty()) return
        preloadScope.launch {
            keys.map { key ->
                async {
                    val url = VideoUrlManager.getVideoUrl(key)?.takeIf { it.isNotBlank() }
                    if (url == null) {
                        startedPreloads.remove(key)
                        return@async
                    }
                    VideoCacheManager.cacheVideo(context, okHttpClient, key, url)
                        ?: startedPreloads.remove(key)
                }
            }.awaitAll()
        }
    }

    fun getHomeHeroSlides(): List<HomeHeroSlide> {
        val json = PrefManager.getHomeHeroImagesJson()
        if (json.isBlank()) return emptyList()
        return runCatching {
            gson.fromJson(json, Array<HomeHeroSlide>::class.java)?.toList().orEmpty()
        }.getOrDefault(emptyList())
    }

    private fun savePromoImages(apiUrl: String, items: List<VideoItemDto>) {
        val promoItems = items.mapNotNull { item ->
            if (!isPromoImageTitle(item.title)) return@mapNotNull null
            saveVideoItem(apiUrl, item)
            toPromoImageItem(apiUrl, item)
        }
        if (promoItems.isNotEmpty()) {
            PrefManager.setPromoImagesJson(gson.toJson(promoItems))
        }
    }

    private fun toPromoImageItem(apiUrl: String, item: VideoItemDto): PromoImageItem? {
        val title = item.title?.trim().orEmpty()
        val path = item.url?.trim().orEmpty()
        if (title.isBlank() || path.isBlank() || !isPromoImageTitle(title)) return null
        return PromoImageItem(
            title = title,
            imageUrl = resolveFullVideoUrl(apiUrl, path),
            header = item.header?.trim().orEmpty(),
            description = item.description?.trim().orEmpty()
        )
    }

    private fun isPromoImageTitle(title: String?): Boolean {
        val normalized = title?.trim().orEmpty().lowercase()
        return normalized.startsWith("female_images_") || normalized.startsWith("male_image_")
    }

    private fun saveHomeHeroImages(apiUrl: String, items: List<VideoItemDto>) {
        val slides = items.mapNotNull { item ->
            if (isPromoImageTitle(item.title)) return@mapNotNull null
            val path = item.url?.trim().orEmpty()
            if (path.isBlank()) return@mapNotNull null
            HomeHeroSlide(
                id = item.id,
                imageUrl = resolveFullVideoUrl(apiUrl, path),
                title = item.title?.trim().orEmpty()
            )
        }
        PrefManager.setHomeHeroImagesJson(gson.toJson(slides))
    }

    private fun saveVideoItem(apiUrl: String, item: VideoItemDto) {
        val title = item.title?.trim().orEmpty()
        val path = item.url?.trim().orEmpty()
        if (title.isBlank() || path.isBlank()) return

        val fullUrl = resolveFullVideoUrl(apiUrl, path)
        PrefManager.setVideoUrl(title, AESHelper.encrypt(fullUrl))
    }

    private fun resolveFullVideoUrl(apiUrl: String, path: String): String {
        if (path.startsWith("http://", ignoreCase = true) ||
            path.startsWith("https://", ignoreCase = true)
        ) {
            return path
        }

        val origin = runCatching {
            val uri = URI(apiUrl)
            "${uri.scheme}://${uri.host}"
        }.getOrDefault("https://aihair.brainfungames.com")

        return origin.trimEnd('/') + path
    }

    companion object {
        private const val TAG = "VideosRepository"
    }
}

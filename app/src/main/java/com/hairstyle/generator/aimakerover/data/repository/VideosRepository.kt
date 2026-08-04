package com.hairstyle.generator.aimakerover.data.repository

import android.content.Context
import android.util.Log
import com.google.gson.Gson
import com.hairstyle.generator.aimakerover.data.model.VideoItemDto
import com.hairstyle.generator.aimakerover.data.model.VideosApiResponse
import com.hairstyle.generator.aimakerover.data.remoteconfig.RemoteConfigManager
import com.hairstyle.generator.aimakerover.domain.model.HomeHeroSlide
import com.hairstyle.generator.aimakerover.domain.model.PromoImageItem
import com.hairstyle.generator.aimakerover.utils.AESHelper
import com.hairstyle.generator.aimakerover.utils.PrefManager
import com.hairstyle.generator.aimakerover.utils.VideoCacheManager
import com.hairstyle.generator.aimakerover.utils.VideoUrlManager
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.net.URI
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class VideosRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val remoteConfigManager: RemoteConfigManager,
    private val okHttpClient: OkHttpClient,
    private val gson: Gson
) {

    suspend fun fetchAndSaveAllVideos() = withContext(Dispatchers.IO) {
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

//        Log.d(TAG, "Saved ${items.size} video URLs to SharedPreferences")

        // Background cache for remaining videos (non-blocking)
        CoroutineScope(Dispatchers.IO).launch {
            items.forEach { item ->
                val title = item.title?.trim().orEmpty()
                val videoUrl = VideoUrlManager.getVideoUrl(title)
                if (!videoUrl.isNullOrBlank()) {
                    VideoCacheManager.cacheVideo(context, okHttpClient, title, videoUrl)
                }
            }
        }
    }

    /**
     * Splash: wait until listed videos are on disk (or [timeoutMs] elapses).
     * Call after [fetchAndSaveAllVideos] so URLs exist in prefs.
     */
    suspend fun awaitCacheVideos(
        videoTitleKeys: List<String>,
        timeoutMs: Long = 15_000L
    ) = withContext(Dispatchers.IO) {
        kotlinx.coroutines.withTimeoutOrNull(timeoutMs) {
            videoTitleKeys.forEach { key ->
                val url = VideoUrlManager.getVideoUrl(key)?.takeIf { it.isNotBlank() } ?: return@forEach
                if (VideoCacheManager.getCachedMediaFile(context, key, url) != null) {
//                    Log.d(TAG, "Already cached: $key")
                    return@forEach
                }
//                Log.d(TAG, "Caching for splash wait: $key")
                VideoCacheManager.cacheVideo(context, okHttpClient, key, url)
            }
        } ?: run {
            // Log.w(TAG, "Video cache wait timed out after ${timeoutMs}ms")
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
            // Log.d(TAG, "Saved ${promoItems.size} promo image URLs")
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
        // Log.d(TAG, "Saved ${slides.size} home hero images")
    }

    private fun saveVideoItem(apiUrl: String, item: VideoItemDto) {
        val title = item.title?.trim().orEmpty()
        val path = item.url?.trim().orEmpty()
        if (title.isBlank() || path.isBlank()) return

        val fullUrl = resolveFullVideoUrl(apiUrl, path)
        PrefManager.setVideoUrl(title, AESHelper.encrypt(fullUrl))
        // Log.d(TAG, "video[$title] saved")
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
        }.getOrDefault("https://hair.champslevl.com")

        return origin.trimEnd('/') + path
    }

    companion object {
        private const val TAG = "VideosRepository"
    }
}

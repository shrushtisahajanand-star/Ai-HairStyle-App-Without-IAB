package com.hairstyle.changer.aitryon.repository

import android.util.Log
import com.hairstyle.changer.aitryon.BuildConfig
import com.hairstyle.changer.aitryon.model.CategoryImageDto
import com.hairstyle.changer.aitryon.model.CategoryImagesResponse

/** Logcat filter: {@code CategoryImages} */
internal object CategoryImagesLogger {

    private const val TAG = "CategoryImages"
    private const val MAX_BODY_LOG = 3500

    fun request(label: String, url: String, baseUrl: String? = null) {
        Log.d(TAG, "[$label] GET $url")
        baseUrl?.let { Log.d(TAG, "[$label] base_url=$it") }
    }

    fun response(label: String, httpCode: Int, body: String, parsed: CategoryImagesResponse) {
        Log.d(
            TAG,
            "[$label] HTTP $httpCode | success=${parsed.success} | apiCategory=${parsed.category} | " +
                "page=${parsed.page} limit=${parsed.limit} count=${parsed.count} | " +
                "totalCount=${parsed.totalCount} totalPages=${parsed.totalPages} | " +
                "imagesInResponse=${parsed.images.size}"
        )
        parsed.images.firstOrNull()?.let { sample ->
            Log.d(
                TAG,
                "[$label] sample[0] id=${sample.id} title=${sample.title} | " +
                    "image=${sample.image} url=${sample.url} | " +
                    "beforeImage=${sample.beforeImage} afterImage=${sample.afterImage} | " +
                    "value=${sample.value} hairstyle=${sample.hairstyle} hairColor=${sample.hairColor}"
            )
        }
        if (BuildConfig.DEBUG) {
            val preview = if (body.length <= MAX_BODY_LOG) body else body.take(MAX_BODY_LOG) + "…(${body.length} chars)"
            Log.d(TAG, "[$label] responseBody=$preview")
        } else {
            Log.d(TAG, "[$label] responseBodyLength=${body.length}")
        }
    }

    fun mapped(
        label: String,
        rawCount: Int,
        mappedCount: Int,
        skippedReasons: Map<String, Int>,
        sampleUrls: List<String>
    ) {
        Log.d(
            TAG,
            "[$label] mappedItems=$mappedCount/$rawCount | skipped=$skippedReasons"
        )
        sampleUrls.take(3).forEachIndexed { index, url ->
            Log.d(TAG, "[$label] resolvedUrl[$index]=$url")
        }
        if (mappedCount == 0 && rawCount > 0) {
            Log.w(
                TAG,
                "[$label] All ${rawCount} items were skipped — check image/url/afterImage and value fields in response"
            )
        }
    }

    fun resolvedUrl(label: String, dtoId: Int, relative: String, fullUrl: String) {
        if (BuildConfig.DEBUG) {
            Log.v(TAG, "[$label] id=$dtoId relative=$relative -> $fullUrl")
        }
    }

    fun skip(label: String, dto: CategoryImageDto, reason: String) {
        if (BuildConfig.DEBUG) {
            Log.v(TAG, "[$label] skip id=${dto.id} title=${dto.title} reason=$reason")
        }
    }

    fun failure(label: String, message: String, throwable: Throwable? = null) {
        Log.e(TAG, "[$label] $message", throwable)
    }

    fun coilLoad(url: String, success: Boolean, error: String? = null) {
        if (success) {
            Log.d(TAG, "[Coil] loaded OK url=$url")
        } else {
            Log.e(TAG, "[Coil] load FAILED url=$url error=$error")
        }
    }
}

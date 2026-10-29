package com.hairstyle.changer.aitryon.Ads

import android.app.Activity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.LinearLayout
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.widget.NestedScrollView
import androidx.fragment.app.Fragment
import com.facebook.shimmer.ShimmerFrameLayout
import com.hairstyle.changer.aitryon.R
import com.hairstyle.changer.aitryon.databinding.IncludeScreenBottomAdsBinding
import com.hairstyle.changer.aitryon.databinding.IncludeSettingCenterAdsBinding

/**
 * Loads Firebase-driven bottom ads on every screen.
 * Banner → [ADSBannerSmall] (Language keeps [ADSBannerAdaptive] separately).
 * Native → [ADSNativeDisplay] with size small|medium|big.
 */
object ScreenAdsHelper {

    private const val TAG = "ScreenAdsHelper"

    data class Config(
        val show: Boolean,
        val adsType: String,
        val adsSize: String,
        val nativeId: String,
        val bannerId: String
    )

    fun load(fragment: Fragment, root: View, config: Config) {
        val activity = fragment.activity ?: return
        load(activity, root, config)
    }

    fun load(activity: Activity, root: View, config: Config) {
        if (activity.isFinishing || activity.isDestroyed) return

        val slot = ensureBottomSlot(root) ?: return
        bindAds(
            activity = activity,
            container = slot.root,
            cardNative = slot.cardNativeAd,
            shimmer = slot.shimmerScreenAd,
            flNative = slot.flNativeAd,
            flBanner = slot.flBannerAd,
            config = config,
            useAdaptiveBanner = false
        )
    }

    fun loadSettingCenter(fragment: Fragment, centerContainer: View, config: Config) {
        val activity = fragment.activity ?: return
        loadSettingCenter(activity, centerContainer, config)
    }

    fun loadSettingCenter(activity: Activity, centerContainer: View, config: Config) {
        if (activity.isFinishing || activity.isDestroyed) return
        val slot = IncludeSettingCenterAdsBinding.bind(centerContainer)
        bindAds(
            activity = activity,
            container = slot.root,
            cardNative = slot.cardSettingCenterNativeAd,
            shimmer = slot.shimmerSettingCenterAd,
            flNative = slot.flSettingCenterNativeAd,
            flBanner = slot.flSettingCenterBannerAd,
            config = config,
            useAdaptiveBanner = false
        )
    }

    private fun bindAds(
        activity: Activity,
        container: View,
        cardNative: View,
        shimmer: ShimmerFrameLayout,
        flNative: FrameLayout,
        flBanner: FrameLayout,
        config: Config,
        useAdaptiveBanner: Boolean
    ) {
        val adsFree = ADSMainClass.getAds_Free()
        val canRequestAds = ADSConsentManager.canRequestAds(activity)
        if (!config.show || adsFree || !canRequestAds) {
            //Log.d(
//                TAG,
//                "Ads blocked → show=${config.show} adsFree=$adsFree " +
//                    "ads_blocked=${ADSMainClass.getAdsBlocked()} " +
//                    "canRequestAds=$canRequestAds " +
//                    "type=${config.adsType} nativeId=${config.nativeId} bannerId=${config.bannerId}"
//            )
            container.visibility = View.GONE
            return
        }

        val type = normalizeAdsType(config.adsType)
        val size = ADSNativeDisplay.normalizeNativeSize(config.adsSize)
        container.visibility = View.VISIBLE

        if (type == "native") {
            val nativeId = config.nativeId.trim()
            if (nativeId.isEmpty()) {
                container.visibility = View.GONE
                return
            }
            flBanner.visibility = View.GONE
            cardNative.visibility = View.VISIBLE
            shimmer.visibility = View.VISIBLE
            flNative.visibility = View.VISIBLE
            ADSNativeDisplay.loadAdmobNativeAdBig(
                nativeId,
                flNative,
                shimmer,
                size,
                activity
            )
        } else {
            val bannerId = config.bannerId.trim()
            if (bannerId.isEmpty()) {
                container.visibility = View.GONE
                return
            }
            cardNative.visibility = View.GONE
            flNative.visibility = View.GONE
            flBanner.visibility = View.VISIBLE
            shimmer.visibility = View.VISIBLE
            if (useAdaptiveBanner) {
                ADSBannerAdaptive.loadAdMobBanner(bannerId, flBanner, shimmer, activity)
            } else {
                ADSBannerSmall.loadAdMobBanner(bannerId, flBanner, shimmer, activity)
            }
        }
    }

    /**
     * Home bottom Native reload: shimmer → new request → show when ready.
     * [onFinished] runs after load success or failure (so the next timer can start from Show).
     */
    fun reloadHomeBottomNative(
        activity: Activity,
        root: View,
        onFinished: (() -> Unit)? = null
    ) {
        if (activity.isFinishing || activity.isDestroyed) {
            onFinished?.invoke()
            return
        }
        if (!ADSMainClass.getNativeAdsReloading()) {
            onFinished?.invoke()
            return
        }
        val config = home()
        if (normalizeAdsType(config.adsType) != "native") {
            onFinished?.invoke()
            return
        }
        val adsFree = ADSMainClass.getAds_Free()
        val canRequestAds = ADSConsentManager.canRequestAds(activity)
        if (!config.show || adsFree || !canRequestAds) {
            onFinished?.invoke()
            return
        }
        val nativeId = config.nativeId.trim()
        if (nativeId.isEmpty()) {
            onFinished?.invoke()
            return
        }
        val slot = ensureBottomSlot(root) ?: run {
            onFinished?.invoke()
            return
        }
        val size = ADSNativeDisplay.normalizeNativeSize(config.adsSize)
        ADSNativeDisplay.reloadAdmobNativeAd(
            nativeId,
            slot.flNativeAd,
            slot.shimmerScreenAd,
            size,
            activity,
            onFinished?.let { done -> Runnable { done() } }
        )
    }

    fun hide(root: View) {
        root.findViewById<View>(R.id.screenAdContainer)?.visibility = View.GONE
    }

    private fun normalizeAdsType(raw: String?): String {
        return when (raw?.trim()?.lowercase()) {
            "banner" -> "banner"
            else -> "native"
        }
    }

    private fun ensureBottomSlot(root: View): IncludeScreenBottomAdsBinding? {
        // Prefer XML-reserved slot so ads sit below UI (never overlay CTAs).
        root.findViewById<View>(R.id.screenAdContainer)?.let {
            return IncludeScreenBottomAdsBinding.bind(it)
        }
        // Host FrameLayout that only wraps the include
        if (root is ViewGroup) {
            root.findViewById<View>(R.id.screenBottomAds)?.let { host ->
                host.findViewById<View>(R.id.screenAdContainer)?.let {
                    return IncludeScreenBottomAdsBinding.bind(it)
                }
            }
        }

        val host: ViewGroup = when (root) {
            is NestedScrollView -> (root.getChildAt(0) as? ViewGroup) ?: return null
            is ViewGroup -> root
            else -> return null
        }

        // Never gravity-overlay FrameLayouts — that covers buttons.
        if (host is FrameLayout) return null

        val binding = IncludeScreenBottomAdsBinding.inflate(
            LayoutInflater.from(host.context),
            host,
            false
        )

        when (host) {
            is ConstraintLayout -> {
                val adId = binding.root.id
                val wrapContentHost =
                    host.layoutParams?.height == ViewGroup.LayoutParams.WRAP_CONTENT

                if (!wrapContentHost) {
                    for (i in 0 until host.childCount) {
                        val child = host.getChildAt(i) ?: continue
                        val childLp = child.layoutParams as? ConstraintLayout.LayoutParams ?: continue
                        if (childLp.bottomToBottom == ConstraintLayout.LayoutParams.PARENT_ID) {
                            childLp.bottomToBottom = ConstraintLayout.LayoutParams.UNSET
                            childLp.bottomToTop = adId
                            child.layoutParams = childLp
                        }
                    }
                }

                val adLp = ConstraintLayout.LayoutParams(
                    ConstraintLayout.LayoutParams.MATCH_CONSTRAINT,
                    ConstraintLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    startToStart = ConstraintLayout.LayoutParams.PARENT_ID
                    endToEnd = ConstraintLayout.LayoutParams.PARENT_ID
                    bottomToBottom = ConstraintLayout.LayoutParams.PARENT_ID
                    if (wrapContentHost) {
                        var anchorId = View.NO_ID
                        for (i in 0 until host.childCount) {
                            val child = host.getChildAt(i) ?: continue
                            if (child.id != View.NO_ID) anchorId = child.id
                        }
                        if (anchorId != View.NO_ID) {
                            topToBottom = anchorId
                        }
                    }
                }
                host.addView(binding.root, adLp)
            }
            is LinearLayout -> {
                host.addView(
                    binding.root,
                    ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                    )
                )
            }
            else -> {
                host.addView(binding.root)
            }
        }
        return binding
    }

    // region Firebase screen configs

    fun intro1(): Config = Config(
        show = ADSMainClass.getBooleanValue(ADSMainClass.INTRO1_SHOW_BOTTOM_ADS),
        adsType = ADSMainClass.getStringValue(ADSMainClass.INTRO1_ADS_TYPE),
        adsSize = ADSMainClass.getStringValue(ADSMainClass.INTRO1_ADS_SIZE),
        nativeId = ADSMainClass.getStringValue(ADSMainClass.INTRO1_NATIVE),
        bannerId = ADSMainClass.getStringValue(ADSMainClass.INTRO1_BANNER)
    )

    /** Falls back to intro_screen1 until Firebase adds intro_screen1b/1c/1d. */
    fun intro1b(): Config = introSplitConfigOrFallback(
        showKey = ADSMainClass.INTRO1B_SHOW_BOTTOM_ADS,
        typeKey = ADSMainClass.INTRO1B_ADS_TYPE,
        sizeKey = ADSMainClass.INTRO1B_ADS_SIZE,
        nativeKey = ADSMainClass.INTRO1B_NATIVE,
        bannerKey = ADSMainClass.INTRO1B_BANNER
    )

    fun intro1c(): Config = introSplitConfigOrFallback(
        showKey = ADSMainClass.INTRO1C_SHOW_BOTTOM_ADS,
        typeKey = ADSMainClass.INTRO1C_ADS_TYPE,
        sizeKey = ADSMainClass.INTRO1C_ADS_SIZE,
        nativeKey = ADSMainClass.INTRO1C_NATIVE,
        bannerKey = ADSMainClass.INTRO1C_BANNER
    )

    fun intro1d(): Config = introSplitConfigOrFallback(
        showKey = ADSMainClass.INTRO1D_SHOW_BOTTOM_ADS,
        typeKey = ADSMainClass.INTRO1D_ADS_TYPE,
        sizeKey = ADSMainClass.INTRO1D_ADS_SIZE,
        nativeKey = ADSMainClass.INTRO1D_NATIVE,
        bannerKey = ADSMainClass.INTRO1D_BANNER
    )

    private fun introSplitConfigOrFallback(
        showKey: String,
        typeKey: String,
        sizeKey: String,
        nativeKey: String,
        bannerKey: String
    ): Config {
        val show = ADSMainClass.getBooleanValue(showKey)
        val adsType = ADSMainClass.getStringValue(typeKey)
        val adsSize = ADSMainClass.getStringValue(sizeKey)
        val nativeId = ADSMainClass.getStringValue(nativeKey)
        val bannerId = ADSMainClass.getStringValue(bannerKey)
        // Never applied from Firebase → reuse intro_screen1 so ads still show
        if (!show && adsType.isBlank() && nativeId.isBlank() && bannerId.isBlank()) {
            return intro1()
        }
        return Config(
            show = show,
            adsType = adsType,
            adsSize = adsSize,
            nativeId = nativeId,
            bannerId = bannerId
        )
    }

    fun intro2(): Config = Config(
        show = ADSMainClass.getBooleanValue(ADSMainClass.INTRO2_SHOW_BOTTOM_ADS),
        adsType = ADSMainClass.getStringValue(ADSMainClass.INTRO2_ADS_TYPE),
        adsSize = ADSMainClass.getStringValue(ADSMainClass.INTRO2_ADS_SIZE),
        nativeId = ADSMainClass.getStringValue(ADSMainClass.INTRO2_NATIVE),
        bannerId = ADSMainClass.getStringValue(ADSMainClass.INTRO2_BANNER)
    )

    fun intro3(): Config = Config(
        show = ADSMainClass.getBooleanValue(ADSMainClass.INTRO3_SHOW_BOTTOM_ADS),
        adsType = ADSMainClass.getStringValue(ADSMainClass.INTRO3_ADS_TYPE),
        adsSize = ADSMainClass.getStringValue(ADSMainClass.INTRO3_ADS_SIZE),
        nativeId = ADSMainClass.getStringValue(ADSMainClass.INTRO3_NATIVE),
        bannerId = ADSMainClass.getStringValue(ADSMainClass.INTRO3_BANNER)
    )

    fun home(): Config = Config(
        show = ADSMainClass.getHomeScreenBottomShow(),
        adsType = ADSMainClass.getHomeScreenAdsType(),
        adsSize = ADSMainClass.getStringValue(ADSMainClass.HOME_SCREEN_ADS_SIZE),
        nativeId = ADSMainClass.getStringValue(ADSMainClass.HOME_SCREEN_NATIVE),
        bannerId = ADSMainClass.getStringValue(ADSMainClass.HOME_SCREEN_BANNER)
    )

    fun selectImage(): Config = Config(
        show = ADSMainClass.getBooleanValue(ADSMainClass.SELECT_IMAGE_SHOW_BOTTOM_ADS),
        adsType = ADSMainClass.getStringValue(ADSMainClass.SELECT_IMAGE_ADS_TYPE),
        adsSize = ADSMainClass.getStringValue(ADSMainClass.SELECT_IMAGE_ADS_SIZE),
        nativeId = ADSMainClass.getStringValue(ADSMainClass.SELECT_IMAGE_NATIVE),
        bannerId = ADSMainClass.getStringValue(ADSMainClass.SELECT_IMAGE_BANNER)
    )

    fun addPhoto(): Config = Config(
        show = ADSMainClass.getBooleanValue(ADSMainClass.ADD_PHOTO_SHOW_BOTTOM_ADS),
        adsType = ADSMainClass.getStringValue(ADSMainClass.ADD_PHOTO_ADS_TYPE),
        adsSize = ADSMainClass.getStringValue(ADSMainClass.ADD_PHOTO_ADS_SIZE),
        nativeId = ADSMainClass.getStringValue(ADSMainClass.ADD_PHOTO_NATIVE),
        bannerId = ADSMainClass.getStringValue(ADSMainClass.ADD_PHOTO_BANNER)
    )

    fun galleryPhotos(): Config = Config(
        show = ADSMainClass.getBooleanValue(ADSMainClass.GALLERY_PHOTOS_SHOW_BOTTOM_ADS),
        adsType = ADSMainClass.getStringValue(ADSMainClass.GALLERY_PHOTOS_ADS_TYPE),
        adsSize = ADSMainClass.getStringValue(ADSMainClass.GALLERY_PHOTOS_ADS_SIZE),
        nativeId = ADSMainClass.getStringValue(ADSMainClass.GALLERY_PHOTOS_NATIVE),
        bannerId = ADSMainClass.getStringValue(ADSMainClass.GALLERY_PHOTOS_BANNER)
    )

    fun progress(): Config = Config(
        show = ADSMainClass.getBooleanValue(ADSMainClass.PROGRESS_SHOW_BOTTOM_ADS),
        adsType = ADSMainClass.getStringValue(ADSMainClass.PROGRESS_ADS_TYPE),
        adsSize = ADSMainClass.getStringValue(ADSMainClass.PROGRESS_ADS_SIZE),
        nativeId = ADSMainClass.getStringValue(ADSMainClass.PROGRESS_NATIVE),
        bannerId = ADSMainClass.getStringValue(ADSMainClass.PROGRESS_BANNER)
    )

    fun result(): Config = Config(
        show = ADSMainClass.getBooleanValue(ADSMainClass.RESULT_SHOW_BOTTOM_ADS),
        adsType = ADSMainClass.getStringValue(ADSMainClass.RESULT_ADS_TYPE),
        adsSize = ADSMainClass.getStringValue(ADSMainClass.RESULT_ADS_SIZE),
        nativeId = ADSMainClass.getStringValue(ADSMainClass.RESULT_NATIVE),
        bannerId = ADSMainClass.getStringValue(ADSMainClass.RESULT_BANNER)
    )

    fun settingBottom(): Config = Config(
        show = ADSMainClass.getSettingBottomAdsShow(),
        adsType = ADSMainClass.getSettingAdsType(),
        adsSize = ADSMainClass.getStringValue(ADSMainClass.SETTING_SCREEN_ADS_SIZE),
        nativeId = ADSMainClass.getStringValue(ADSMainClass.SETTING_SCREEN_NATIVE),
        bannerId = ADSMainClass.getStringValue(ADSMainClass.SETTING_SCREEN_BANNER)
    )

    fun settingCenter(): Config = Config(
        show = ADSMainClass.getBooleanValue(ADSMainClass.SETTING_CENTER_ADS_SHOW),
        adsType = ADSMainClass.getStringValue(ADSMainClass.SETTING_CENTER_ADS_TYPE),
        adsSize = ADSMainClass.getStringValue(ADSMainClass.SETTING_CENTER_ADS_SIZE),
        nativeId = ADSMainClass.getStringValue(ADSMainClass.SETTING_CENTER_NATIVE),
        bannerId = ADSMainClass.getStringValue(ADSMainClass.SETTING_CENTER_BANNER)
    )

    fun userCreation(): Config = Config(
        show = ADSMainClass.getBooleanValue(ADSMainClass.USER_CREATION_SHOW_BOTTOM_ADS),
        adsType = ADSMainClass.getStringValue(ADSMainClass.USER_CREATION_ADS_TYPE),
        adsSize = ADSMainClass.getStringValue(ADSMainClass.USER_CREATION_ADS_SIZE),
        nativeId = ADSMainClass.getStringValue(ADSMainClass.USER_CREATION_NATIVE),
        bannerId = ADSMainClass.getStringValue(ADSMainClass.USER_CREATION_BANNER)
    )

    fun feedback(): Config = Config(
        show = ADSMainClass.getBooleanValue(ADSMainClass.FEEDBACK_SHOW_BOTTOM_ADS),
        adsType = ADSMainClass.getStringValue(ADSMainClass.FEEDBACK_ADS_TYPE),
        adsSize = ADSMainClass.getStringValue(ADSMainClass.FEEDBACK_ADS_SIZE),
        nativeId = ADSMainClass.getStringValue(ADSMainClass.FEEDBACK_NATIVE),
        bannerId = ADSMainClass.getStringValue(ADSMainClass.FEEDBACK_BANNER)
    )

    // endregion
}

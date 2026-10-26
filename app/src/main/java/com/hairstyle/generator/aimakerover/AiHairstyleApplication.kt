package com.hairstyle.generator.aimakerover

import android.app.Activity
import android.app.Application
import android.content.Context
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.decode.GifDecoder
import coil.decode.ImageDecoderDecoder
import coil.disk.DiskCache
import coil.memory.MemoryCache
import coil.request.CachePolicy
import com.hairstyle.generator.aimakeover.BuildConfig
import com.hairstyle.generator.aimakerover.Advertisement.ADSAppManage
import com.hairstyle.generator.aimakerover.fcm.FcmTokenHelper
import com.hairstyle.generator.aimakerover.fcm.NotificationHelper
import com.hairstyle.generator.aimakerover.utils.LocaleManager
import com.hairstyle.generator.aimakerover.utils.PermissionHelper
import com.hairstyle.generator.aimakerover.utils.PrefManager
import com.hairstyle.generator.aimakerover.utils.PremiumPlanManager
import com.hairstyle.generator.aimakerover.utils.SecureConfig
import com.revenuecat.purchases.LogLevel
import com.revenuecat.purchases.Purchases
import com.revenuecat.purchases.PurchasesConfiguration
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class AiHairstyleApplication : Application(), ImageLoaderFactory {

    override fun attachBaseContext(base: Context) {
        PrefManager.init(base)
        super.attachBaseContext(LocaleManager.wrap(base))
    }

    override fun onCreate() {
        super.onCreate()
        PrefManager.init(this)
        PermissionHelper.resetStaleDeniedFlags()
        LocaleManager.applyStoredLocale()
        configureRevenueCat()
        ADSAppManage.bind(this)
        // Ads_Free is persisted; re-derive it from the real plan state so a stale
        // "true" (e.g. left over from a billing test) can never keep ads off.
        // Must run after bind() — ADSMainClass prefs need the app context.
        runCatching { PremiumPlanManager.refreshLocalExpiryOnLaunch() }
        NotificationHelper.ensureChannel(this)
        FcmTokenHelper.init()
        // Block screenshots + screen recording on every Activity window.
//        registerActivityLifecycleCallbacks(SecureWindowCallbacks())
    }

    private fun configureRevenueCat() {
        if (BuildConfig.DEBUG) {
            Purchases.logLevel = LogLevel.DEBUG
        }
        Purchases.configure(
            PurchasesConfiguration.Builder(this, SecureConfig.revenueCatApiKey()).build()
        )
    }

    override fun newImageLoader(): ImageLoader {
        return ImageLoader.Builder(this)
            .crossfade(true)
            .components {
                if (Build.VERSION.SDK_INT >= 28) {
                    add(ImageDecoderDecoder.Factory())
                } else {
                    add(GifDecoder.Factory())
                }
            }
            .memoryCache {
                MemoryCache.Builder(this)
                    .maxSizePercent(0.25)
                    .build()
            }
            .diskCache {
                DiskCache.Builder()
                    .directory(cacheDir.resolve("image_cache"))
                    .maxSizePercent(0.02)
                    .build()
            }
            .diskCachePolicy(CachePolicy.ENABLED)
            .memoryCachePolicy(CachePolicy.ENABLED)
            .build()
    }

    private class SecureWindowCallbacks : ActivityLifecycleCallbacks {
        override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {
            applySecureFlag(activity)
        }

        override fun onActivityStarted(activity: Activity) = Unit

        override fun onActivityResumed(activity: Activity) {
            // Re-apply in case theme / edge-to-edge clears window flags.
            applySecureFlag(activity)
        }

        override fun onActivityPaused(activity: Activity) = Unit
        override fun onActivityStopped(activity: Activity) = Unit
        override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) = Unit
        override fun onActivityDestroyed(activity: Activity) = Unit

        private fun applySecureFlag(activity: Activity) {
            activity.window.setFlags(
                WindowManager.LayoutParams.FLAG_SECURE,
                WindowManager.LayoutParams.FLAG_SECURE
            )
        }
    }
}

package com.hairstyle.changer.aitryon

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
import com.hairstyle.changer.aitryon.Ads.ADSAppManage
import com.hairstyle.changer.aitryon.di.fcm.FcmTokenHelper
import com.hairstyle.changer.aitryon.di.fcm.NotificationHelper
import com.hairstyle.changer.aitryon.utils.LocaleManager
import com.hairstyle.changer.aitryon.utils.PermissionHelper
import com.hairstyle.changer.aitryon.utils.PrefManager
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
        ADSAppManage.bind(this)
        NotificationHelper.ensureChannel(this)
        FcmTokenHelper.init()
        // Block screenshots + screen recording on every Activity window.
//        registerActivityLifecycleCallbacks(SecureWindowCallbacks())
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
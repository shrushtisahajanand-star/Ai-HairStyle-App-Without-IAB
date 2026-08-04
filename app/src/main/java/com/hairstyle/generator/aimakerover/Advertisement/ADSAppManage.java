package com.hairstyle.generator.aimakerover.Advertisement;

import android.app.Activity;
import android.app.Application;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.work.Configuration;
import androidx.work.WorkManager;

import com.google.android.gms.ads.MobileAds;
import com.google.android.gms.ads.RequestConfiguration;

import java.util.Arrays;
import java.util.List;

public class ADSAppManage extends Application {

    private static Application appInstance;
    public static boolean FastStart = false;
    public static boolean isActivityChecked = false;
    public static boolean AppStartingScreenOpen = false;
    private static int NumStarted = 0;
    public static boolean dialogboolean = true;
    public static long appOpenBlockUntil = 0;
    public static boolean isAppOpenBlocked = false;

    private static boolean mobileAdsInitialized = false;
    private static boolean lifecycleRegistered = false;

    public static boolean isMobileAdsInitialized() {
        return mobileAdsInitialized;
    }

    public static void initializeMobileAdsIfNeeded(Context context) {
        initializeMobileAdsIfNeeded(context, null);
    }

    public static void initializeMobileAdsIfNeeded(Context context, @Nullable Runnable onComplete) {
        if (mobileAdsInitialized) {
            if (onComplete != null) {
                onComplete.run();
            }
            return;
        }
        AdSdkInitHelper.runIfWebViewAvailable(context, () -> {
            List<String> testDeviceIds = Arrays.asList("");
            RequestConfiguration configuration = new RequestConfiguration.Builder()
                    .setTestDeviceIds(testDeviceIds)
                    .build();
            MobileAds.setRequestConfiguration(configuration);
            MobileAds.initialize(context, initializationStatus -> {
                mobileAdsInitialized = true;
                preloadAppOpenAdIfNeeded();
                if (onComplete != null) {
                    onComplete.run();
                }
            });
        }, () -> {
            if (onComplete != null) {
                onComplete.run();
            }
        });
    }


    public static void blockAppOpenAd(long durationMs) {
        appOpenBlockUntil = System.currentTimeMillis() + durationMs;
    }

    /**
     * Call before leaving the app (Share / Rate / Privacy / Play Store / Camera).
     * Next background→foreground App Open will be skipped once.
     */
    public static void skipNextAppOpenAd() {
        isAppOpenBlocked = true;
    }

    /**
     * Bind from the real {@link Application} ({@code AiHairstyleApplication}).
     * Required because this class is not registered in the manifest.
     */
    public static void bind(Application application) {
        if (application == null) return;
        appInstance = application;
        ADSMainClass.initAppContext(application);
        registerAppOpenLifecycle(application);
    }

    public static Application getApp() {
        return appInstance;
    }


    @Override
    public void onCreate() {
        super.onCreate();
        bind(this);
    }

    public static void preloadAppOpenAdIfNeeded() {
        if (appInstance == null) {
            return;
        }
        if (ADSMainClass.getAds_Free()) {
            return;
        }
        if (!canShowAppOpenAd()) {
            return;
        }
        String appOpenId = ADSMainClass.getStringValue(ADSMainClass.APP_OPEN_ID);
        if (appOpenId == null || appOpenId.isEmpty()) {
            return;
        }
        ADSAppBackground.preloadAppOpenAd(appInstance);
    }

    private static void registerAppOpenLifecycle(Application application) {
        if (lifecycleRegistered) return;
        lifecycleRegistered = true;

        application.registerActivityLifecycleCallbacks(new ActivityLifecycleCallbacks() {
            @Override
            public void onActivityCreated(Activity activity, Bundle bundle) {
            }

            @Override
            public void onActivityStarted(Activity activity) {
                isActivityChecked = true;
                NumStarted++;
                String className = activity.getClass().getSimpleName();
                if (className.equals("LanguageActivity") ||
                        className.equals("CallEndActivity") ||
                        className.equals("SplashActivity") ||
                        className.equals("FullMActivity") ||
                        className.equals("OverlayPermissionActivity")
                ) {
                    return;
                }
                if (AppStartingScreenOpen) {
                    preloadAppOpenAdIfNeeded();
                }
                if (System.currentTimeMillis() < appOpenBlockUntil) {
                    return;
                }
                if (isAppOpenBlocked) {
                    isAppOpenBlocked = false;
                    return;
                }
                if (!ADSMainClass.getComingSoon()) {
                    if (AppStartingScreenOpen) {
                        if (NumStarted == 1) {
                            if (!FastStart) {
                                if (ADSUtilitis.IsNetworkConnected(activity)) {
                                    // Background resume: App Open ONLY — never Interstitial
                                    // Controlled by ads.splash_screen.app_open_show_in_background
                                    if (!ADSMainClass.getAppOpenBackgroundShow()) {
                                        return;
                                    }
                                    if (!canShowAppOpenAd()) {
                                        return;
                                    }
                                    String appOpenId = ADSMainClass.getStringValue(ADSMainClass.APP_OPEN_ID);
                                    if (appOpenId == null || appOpenId.isEmpty()) {
                                        return;
                                    }
                                    new Handler().postDelayed(new Runnable() {
                                        @Override
                                        public void run() {
                                            if (ADSAppBackground.isAppOpenAdReady()) {
                                                ADSAppBackground.showPreloadedAppOpen(activity);
                                            } else {
                                                preloadAppOpenAdIfNeeded();
                                            }
                                        }
                                    }, 100);
                                }
                            } else {
                                FastStart = false;
                            }
                        }
                    }
                }
            }

            @Override
            public void onActivityResumed(Activity activity) {
            }

            @Override
            public void onActivityPaused(Activity activity) {
            }

            @Override
            public void onActivityStopped(Activity activity) {

                NumStarted--;
                if (NumStarted == 0) {
                    preloadAppOpenAdIfNeeded();
                }
            }

            @Override
            public void onActivitySaveInstanceState(Activity activity, Bundle bundle) {
            }

            @Override
            public void onActivityDestroyed(Activity activity) {
            }
        });

    }


    public static boolean canShowAppOpenAd() {
        if (appInstance == null) return false;

        int limit = ADSMainClass.getAppOpenAdDailyLimit();
        if (limit <= 0) {
            return false;
        }

        android.content.SharedPreferences prefs = appInstance.getSharedPreferences("AdPrefs", Context.MODE_PRIVATE);
        String todayString = new java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).format(new java.util.Date());
        String savedDate = prefs.getString("last_ad_date", "");

        if (!todayString.equals(savedDate)) {
            return true;
        }

        int count = prefs.getInt("ad_count_today", 0);
        return count < limit;
    }

    public static void incrementAdCount() {
        if (appInstance == null) return;
        android.content.SharedPreferences prefs = appInstance.getSharedPreferences("AdPrefs", Context.MODE_PRIVATE);
        String todayString = new java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).format(new java.util.Date());
        String savedDate = prefs.getString("last_ad_date", "");
        int count = prefs.getInt("ad_count_today", 0);

        if (!todayString.equals(savedDate)) {
            count = 0;
            prefs.edit().putString("last_ad_date", todayString).apply();
        }
        prefs.edit().putInt("ad_count_today", count + 1).apply();
    }


    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(base);
        appInstance = this;
        ADSMainClass.initAppContext(this);
    }

//    @NonNull
//    @Override
//    public Configuration getWorkManagerConfiguration() {
//        return new Configuration.Builder()
//                .setMinimumLoggingLevel(Log.INFO)
//                .build();
//    }


    @Override
    public Intent registerReceiver(@Nullable BroadcastReceiver receiver, IntentFilter filter) {
        if (Build.VERSION.SDK_INT >= 34 && getApplicationInfo().targetSdkVersion >= 34) {
            return super.registerReceiver(receiver, filter, Context.RECEIVER_EXPORTED);
        } else {
            return super.registerReceiver(receiver, filter);
        }
    }

}

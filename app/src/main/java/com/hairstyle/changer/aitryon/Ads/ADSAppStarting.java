package com.hairstyle.changer.aitryon.Ads;


import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.util.Log;

import androidx.annotation.NonNull;

import com.google.ads.mediation.admob.AdMobAdapter;
import com.google.android.gms.ads.AdError;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdValue;
import com.google.android.gms.ads.FullScreenContentCallback;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.OnPaidEventListener;
import com.google.android.gms.ads.appopen.AppOpenAd;
import com.google.android.gms.ads.interstitial.InterstitialAd;
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback;

import java.util.function.Consumer;

/**
 * Splash ad. One request only:
 * <ol>
 *   <li>{@link #prefetchSplashAd} sends the request as soon as the Firebase config is in (Splash
 *       does not wait for consent dialogs / SDK init to finish first).</li>
 *   <li>{@link #AdsSplashAppStartingDisplay} shows it if loaded, waits for it while it is still
 *       loading (up to {@link #SPLASH_AD_TIMEOUT_MS} after the request), and on fail / timeout
 *       just continues to the next screen. Nothing is requested twice.</li>
 * </ol>
 */
public class ADSAppStarting {

    public static OnFinishAds onFinishAds;
    public static boolean LoadingCheck = true;
    public static AppOpenAd AppStartingAd;
    public static boolean ADSIsShowing = false;
    private static InterstitialAd AdmobmInterstitialAd;

    private static final long SPLASH_AD_TIMEOUT_MS = 6000L;
    private static final long MIN_WAIT_AFTER_DISPLAY_MS = 1500L;

    private static final int IDLE = 0;
    private static final int LOADING = 1;
    private static final int LOADED = 2;
    private static final int FAILED = 3;

    private static final Handler splashHandler = new Handler(Looper.getMainLooper());
    private static int state = IDLE;
    private static boolean displayRequested;
    private static boolean gaveUp;
    private static long splashAdStartMs;
    /** Shows the loaded ad on the given (Splash) activity. */
    private static Consumer<Activity> pendingShow;

    private static void adMark(String stage) {
        Log.d("SplashTime", "  [ad +" + (SystemClock.elapsedRealtime() - splashAdStartMs) + "ms] " + stage);
    }

    /** Call when Splash opens, so a previous launch's state is never reused. */
    public static void resetSplashAd() {
        onSettled = null;
        splashHandler.removeCallbacksAndMessages(null);
        state = IDLE;
        displayRequested = false;
        gaveUp = false;
        pendingShow = null;
        splashActivity = null;
    }

    /** True once the splash ad is loaded or failed (nothing left to wait for). */
    public static boolean isSplashAdSettled() {
        return state == LOADED || state == FAILED;
    }

    /** Sends the single splash ad request now. Safe to call more than once. */
    public static void prefetchSplashAd(Activity context) {
        if (state != IDLE) return;
        if (ADSMainClass.getAds_Free()) return;
        state = LOADING;
        splashAdStartMs = SystemClock.elapsedRealtime();
        adMark("request started");
        ADSSplashAppStartingLoad(context);
    }

    public static void AdsSplashAppStartingDisplay(Activity context, OnFinishAds onFinishAd, boolean... doShowAds) {
        onFinishAds = onFinishAd;
        if (ADSMainClass.getAds_Free()) {
            onFinishAds.onFinishAds(true);
            return;
        }

        displayRequested = true;
        if (state == IDLE) {
            prefetchSplashAd(context);
        }

        if (state == FAILED) {
            adMark("failed earlier -> next");
            onFinishAds.onFinishAds(true);
            return;
        }
        if (state == LOADED) {
            adMark("already loaded -> show");
            pendingShow.accept(context);
            return;
        }

        // Still loading: wait for what is left of the budget, then give up.
        splashActivity = context;
        long left = SPLASH_AD_TIMEOUT_MS - (SystemClock.elapsedRealtime() - splashAdStartMs);
        splashHandler.removeCallbacksAndMessages(null);
        splashHandler.postDelayed(() -> {
            if (state == LOADING) {
                gaveUp = true;
                adMark("TIMEOUT -> next");
                fireSettled();
                onFinishAds.onFinishAds(true);
            }
        }, Math.max(left, MIN_WAIT_AFTER_DISPLAY_MS));
    }

    private static Activity splashActivity;

    private static Runnable onSettled;

    /**
     * Runs {@code r} once the splash ad has loaded / failed / timed out - so other requests
     * (e.g. the Language native) do not compete with it for bandwidth. Runs at once when there is
     * no splash ad in play.
     */
    public static void runWhenSplashAdSettled(boolean splashAdExpected, Runnable r) {
        boolean settled = state == LOADED || state == FAILED || gaveUp
                || (!splashAdExpected && state == IDLE);
        if (settled) {
            r.run();
        } else {
            onSettled = r;
        }
    }

    private static void fireSettled() {
        Runnable r = onSettled;
        onSettled = null;
        if (r != null) r.run();
    }

    private static void onSplashAdLoaded(Consumer<Activity> showAction) {
        adMark("loaded (gaveUp=" + gaveUp + ")");
        if (gaveUp) { fireSettled(); return; }
        state = LOADED;
        pendingShow = showAction;
        splashHandler.removeCallbacksAndMessages(null);
        if (displayRequested && splashActivity != null
                && !splashActivity.isFinishing() && !splashActivity.isDestroyed()) {
            showAction.accept(splashActivity);
        }
        fireSettled();
    }

    private static void onSplashAdFailed() {
        adMark("failed / no ad");
        if (gaveUp) { fireSettled(); return; }
        state = FAILED;
        splashHandler.removeCallbacksAndMessages(null);
        if (displayRequested && onFinishAds != null) {
            onFinishAds.onFinishAds(true);
        }
        fireSettled();
    }

    public static void ADSSplashAppStartingLoad(Activity context) {
        String splashType = ADSMainClass.getSplashADType();
        boolean isAppOpenType = splashType != null && splashType.equalsIgnoreCase("AppOpen");
        boolean isInterType = splashType != null
                && (splashType.equalsIgnoreCase("Inter")
                || splashType.equalsIgnoreCase("Interstitial"));

        if (isInterType) {
            AdmobInterstitialAd(context);
            return;
        }

        if (!isAppOpenType) {
            onSplashAdFailed();
            return;
        }

        String appOpenId = ADSMainClass.getStringValue(ADSMainClass.APP_OPEN_ID);

        if (appOpenId == null || appOpenId.isEmpty()) {
            // No AppOpen id — fall back to interstitial if available, else skip
            AdmobInterstitialAd(context);
            return;
        }

        if (ADSAppBackground.isAppOpenAdReady()) {
            onSplashAdLoaded(activity ->
                    ADSAppBackground.showPreloadedAppOpenForSplash(activity, onFinishAds));
        } else {
            loadAppOpen(context, appOpenId);
        }
    }

    private static void loadAppOpen(Activity context, String appOpenId) {
        try {
            Bundle bundle = new Bundle();
            AdRequest adRequest = new AdRequest.Builder()
                    .addNetworkExtrasBundle(AdMobAdapter.class, bundle)
                    .build();

            AppOpenAd.load(context, appOpenId, adRequest, new AppOpenAd.AppOpenAdLoadCallback() {
                @Override
                public void onAdLoaded(@NonNull AppOpenAd ad) {
                    ad.setOnPaidEventListener(adValue -> ADSUtilitis.logAdRevenue(context, adValue));
                    AppStartingAd = ad;
                    ADSIsShowing = false;
                    ADSUtilitis.trackScreen(context, "Splash_AppOpen_Load");
                    onSplashAdLoaded(activity ->
                            ADSAppBackground.showPreloadedAppOpenForSplash(activity, onFinishAds));
                }

                @Override
                public void onAdFailedToLoad(@NonNull LoadAdError loadAdError) {
                    ADSUtilitis.trackScreen(context, "Splash_AppOpen_Fail");
                    // No second request: a failed App Open goes straight to the next screen.
                    onSplashAdFailed();
                }
            });
        } catch (Exception e) {
            onSplashAdFailed();
        }
    }

    private static void AdmobInterstitialAd(Activity context) {
        String interstitialId = ADSMainClass.getStringValue(ADSMainClass.INTER_FIRST_TIME);
        if (interstitialId == null || interstitialId.isEmpty()) {
            onSplashAdFailed();
            return;
        }

        AdRequest adRequest = new AdRequest.Builder().build();
        AdmobmInterstitialAd = null;

        InterstitialAd.load(context, interstitialId, adRequest,
                new InterstitialAdLoadCallback() {
                    @Override
                    public void onAdLoaded(@NonNull InterstitialAd interstitialAd) {
                        interstitialAd.setOnPaidEventListener(new OnPaidEventListener() {
                            @Override
                            public void onPaidEvent(AdValue adValue) {
                                ADSUtilitis.logAdRevenue(context, adValue);
                            }
                        });

                        LoadingCheck = true;
                        AdmobmInterstitialAd = interstitialAd;
                        onSplashAdLoaded(activity -> {
                            onAdsLoadAdListener(activity);
                            FullscreenAdWindowHelper.prepare(activity);
                            AdmobmInterstitialAd.show(activity);
                        });
                    }

                    @Override
                    public void onAdFailedToLoad(@NonNull LoadAdError loadAdError) {
                        onSplashAdFailed();
                    }
                });
    }

    public static void onAdsLoadAdListener(Context context) {
        AdmobmInterstitialAd.setFullScreenContentCallback(new FullScreenContentCallback() {
            @Override
            public void onAdClicked() {
            }

            @Override
            public void onAdDismissedFullScreenContent() {
                adMark("dismissed");
                if (context instanceof Activity) FullscreenAdWindowHelper.restore((Activity) context);
                onFinishAds.onFinishAds(true);
            }

            @Override
            public void onAdFailedToShowFullScreenContent(AdError adError) {
                adMark("failed to show");
                if (context instanceof Activity) FullscreenAdWindowHelper.restore((Activity) context);
                onFinishAds.onFinishAds(true);
            }

            @Override
            public void onAdImpression() {
            }

            @Override
            public void onAdShowedFullScreenContent() {
                adMark("shown");
            }
        });
    }

    public interface OnFinishAds {
        void onFinishAds(boolean b);
    }

}

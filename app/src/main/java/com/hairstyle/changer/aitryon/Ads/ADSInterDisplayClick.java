package com.hairstyle.changer.aitryon.Ads;


import android.app.Activity;
import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.gms.ads.AdError;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdValue;
import com.google.android.gms.ads.FullScreenContentCallback;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.OnPaidEventListener;
import com.google.android.gms.ads.interstitial.InterstitialAd;
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback;

/**
 * Click / back / bottom-nav interstitial with Firebase counter +
 * {@code inter_ads_load_type}: {@code PreLoad} | {@code Show}.
 * {@code inter_ads_show_type}: {@code Click} (count) | {@code Time} (since last show).
 *
 * <p>Click = 3 → show on the 3rd click (after click 1, 2, 3).
 * <ul>
 *   <li>{@code Show}/{@code Load}: load + show when threshold hits</li>
 *   <li>{@code PreLoad}: show cached ad; after dismiss load the next one</li>
 * </ul>
 */
public class ADSInterDisplayClick {

    public static int ADSClick = 0;
    public static int BackADClick = 0;
    public static int BottomNavADClick = 0;
    /** Intro1–3 Continue / option clicks — uses {@code intro_inter_ads_click_count}. */
    public static int IntroADClick = 0;
    public static Boolean ADSDisplayCheck = false;
    public static int ArrayIndexInterstitialId = 0;

    public static OnFinishAds onFinishAds;
    public static com.facebook.ads.InterstitialAd ADSFBInterstitial;

    /** Cached ad ready for next PreLoad show. */
    private static InterstitialAd ADSAdmobmInterstitial;
    /** Ad currently on screen — must not be cleared when next PreLoad finishes. */
    @Nullable
    private static InterstitialAd showingInterstitial;
    public static boolean LoadingCheck = true;
    /** Last time an interstitial was closed. 0 = never (first eligible trigger can show). */
    private static long lastInterstitialClosedAtMs = 0L;


    public static void ADSInterstitialShowing(Activity context, String adsId, OnFinishAds onFinishAd, boolean... doShowAds) {
        onFinishAds = onFinishAd;
        if (ADSMainClass.getAds_Free() || !ADSMainClass.getInterAdsShow()) {
            onFinishAds.onFinishAds(true);
            return;
        }
        if (hitClickThreshold(true)) {
            ADSInterDisplayClick.ArrayIndexInterstitialId = 0;
            ADSMedDisplay(context, adsId);
        } else {
            onFinishAds.onFinishAds(true);
        }
    }

    /**
     * Intro screens only — counter uses {@code intro_inter_ads_click_count},
     * not global {@code inter_ads_click}.
     */
    public static void ADSIntroInterstitialShowing(Activity context, String adsId, OnFinishAds onFinishAd) {
        onFinishAds = onFinishAd;
        if (ADSMainClass.getAds_Free() || !ADSMainClass.getInterAdsShow()) {
            onFinishAds.onFinishAds(true);
            return;
        }
        if (hitIntroClickThreshold()) {
            ADSInterDisplayClick.ArrayIndexInterstitialId = 0;
            ADSMedDisplay(context, adsId);
        } else {
            onFinishAds.onFinishAds(true);
        }
    }


    public static void ADSBackDisplayInterstitial(Activity context, String adsId, OnFinishAds onFinishAd, boolean... doShowAds) {
        onFinishAds = onFinishAd;
        if (ADSMainClass.getAds_Free() || !ADSMainClass.getInterAdsOnBackShow()) {
            onFinishAds.onFinishAds(true);
            return;
        }
        if (hitClickThreshold(false)) {
            ADSInterDisplayClick.ArrayIndexInterstitialId = 0;
            ADSMedDisplay(context, adsId);
        } else {
            onFinishAds.onFinishAds(true);
        }
    }

    /** Bottom navigation tab clicks — uses {@code bottom_navigation_click*} Firebase keys. */
    public static void ADSBottomNavInterstitial(Activity context, String adsId, OnFinishAds onFinishAd) {
        onFinishAds = onFinishAd;
        if (ADSMainClass.getAds_Free() || !ADSMainClass.getBottomNavigationClickAdsShow()) {
            onFinishAds.onFinishAds(true);
            return;
        }
        if (hitBottomNavThreshold()) {
            ADSInterDisplayClick.ArrayIndexInterstitialId = 0;
            ADSMedDisplay(context, adsId);
        } else {
            onFinishAds.onFinishAds(true);
        }
    }

    /**
     * {@code inter_ads_show_type = Click}: {@code inter_ads_click}/{@code inter_back_ads_click}
     * = 3 → show on click 3 (counts 1, 2, then show). Threshold {@code <= 0} → every click.
     * {@code Time}: shared interval for forward + back + bottom-nav; timer starts after ad close.
     */
    private static boolean hitClickThreshold(boolean forward) {
        if (ADSMainClass.isInterAdsTimeMode()) {
            return hasInterAdsTimeElapsed();
        }
        int threshold = forward ? ADSMainClass.getAdsClick() : ADSMainClass.getAdsBackClick();
        if (forward) {
            ADSClick++;
            if (threshold <= 0 || ADSClick >= threshold) {
                ADSClick = 0;
                return true;
            }
            return false;
        }
        BackADClick++;
        if (threshold <= 0 || BackADClick >= threshold) {
            BackADClick = 0;
            return true;
        }
        return false;
    }

    private static boolean hasInterAdsTimeElapsed() {
        int seconds = ADSMainClass.getInterAdsTimeSeconds();
        if (seconds <= 0) {
            return true;
        }
        if (lastInterstitialClosedAtMs <= 0L) {
            return true;
        }
        return System.currentTimeMillis() - lastInterstitialClosedAtMs >= seconds * 1000L;
    }

    /**
     * {@code intro_inter_ads_click_count} = 1 → every intro Continue; = 3 → every 3rd intro click.
     * Shared across Intro1 / 1b / 1c / 1d / 2 / 3 only.
     */
    private static boolean hitIntroClickThreshold() {
        int threshold = ADSMainClass.getIntroInterAdsClick();
        IntroADClick++;
        if (threshold <= 0 || IntroADClick >= threshold) {
            IntroADClick = 0;
            return true;
        }
        return false;
    }

    private static boolean hitBottomNavThreshold() {
        if (ADSMainClass.isInterAdsTimeMode()) {
            return hasInterAdsTimeElapsed();
        }
        int threshold = ADSMainClass.getBottomNavigationClick();
        BottomNavADClick++;
        if (threshold <= 0 || BottomNavADClick >= threshold) {
            BottomNavADClick = 0;
            return true;
        }
        return false;
    }

    /** Preload when Firebase {@code inter_ads_load_type = PreLoad}. */
    public static void preloadIfNeeded(Activity context, String adsId) {
        if (context == null || ADSMainClass.getAds_Free()) return;
        if (ADSMainClass.isInterShowLoadMode()) return;
        if (adsId == null || adsId.trim().isEmpty()) return;
        if (ADSAdmobmInterstitial != null || showingInterstitial != null || !LoadingCheck) return;
        LoadingCheck = false;
        AdmobInterstitialAd(context, adsId.trim(), false);
    }

    public static void ADSMedDisplay(Activity context, String adsId) {
        if (adsId == null || adsId.trim().isEmpty()) {
            if (onFinishAds != null) onFinishAds.onFinishAds(true);
            return;
        }
        adsId = adsId.trim();

        // Show / Load: request now, dialog, then show when ready
        if (ADSMainClass.isInterShowLoadMode()) {
            if (LoadingCheck) {
                LoadingCheck = false;
                ADSAppManage.isActivityChecked = true;
                AdmobInterstitialAd(context, adsId, true);
            } else if (onFinishAds != null) {
                onFinishAds.onFinishAds(true);
            }
            return;
        }

        // PreLoad: show cached ad if ready; load next only after dismiss
        if (ADSAdmobmInterstitial != null) {
            showingInterstitial = ADSAdmobmInterstitial;
            ADSAdmobmInterstitial = null;
            AdsDisplayCheck(false);
            onAdsLoadAdListener(context, showingInterstitial, adsId);
            showInterstitial(context, showingInterstitial);
        } else {
            if (onFinishAds != null) onFinishAds.onFinishAds(true);
            // Not ready yet — start preload so next threshold can show
            preloadNext(context, adsId);
        }
    }

    private static void preloadNext(Activity context, String adsId) {
        if (context == null || ADSMainClass.getAds_Free()) return;
        if (ADSMainClass.isInterShowLoadMode()) return;
        if (adsId == null || adsId.trim().isEmpty()) return;
        if (ADSAdmobmInterstitial != null || !LoadingCheck) return;
        LoadingCheck = false;
        AdmobInterstitialAd(context, adsId.trim(), false);
    }

    public static void AdsDisplayCheck(Boolean aBoolean) {
        if (aBoolean) {
            ADSAppStarting.ADSIsShowing = false;
            ADSAppManage.FastStart = false;
            ADSDisplayCheck = true;
        } else {
            ADSAppStarting.ADSIsShowing = true;
            ADSDisplayCheck = false;
            ADSAppManage.FastStart = true;
        }
    }

    public static void AdmobInterstitialAd(Activity context, String AdsID) {
        AdmobInterstitialAd(context, AdsID, ADSMainClass.isInterShowLoadMode());
    }

    private static void AdmobInterstitialAd(Activity context, String AdsID, boolean showWhenLoaded) {
        if (showWhenLoaded) {
            ADSUtilitis.MassageBoxFull(context);
        }
        AdRequest adRequest = new AdRequest.Builder().build();
        // Do not clear a PreLoad cache while another interstitial is on screen.
        if (showWhenLoaded || showingInterstitial == null) {
            ADSAdmobmInterstitial = null;
        }
        InterstitialAd.load(context, AdsID.trim(), adRequest,
                new InterstitialAdLoadCallback() {
                    @Override
                    public void onAdLoaded(@NonNull InterstitialAd interstitialAd) {
                        ADSUtilitis.trackScreen(context, "Inter_Load");
                        interstitialAd.setOnPaidEventListener(new OnPaidEventListener() {
                            @Override
                            public void onPaidEvent(AdValue adValue) {
                                ADSUtilitis.logAdRevenue(context, adValue);
                            }
                        });

                        LoadingCheck = true;
                        ADSAdmobmInterstitial = interstitialAd;

                        if (showWhenLoaded) {
                            ADSUtilitis.MassageBoxFullDismiss();
                            if (ADSAppManage.isActivityChecked) {
                                ADSAppManage.isActivityChecked = false;
                                showingInterstitial = ADSAdmobmInterstitial;
                                ADSAdmobmInterstitial = null;
                                AdsDisplayCheck(false);
                                onAdsLoadAdListener(context, showingInterstitial, AdsID.trim());
                                showInterstitial(context, showingInterstitial);
                            } else if (onFinishAds != null) {
                                onFinishAds.onFinishAds(true);
                            }
                        }
                    }

                    @Override
                    public void onAdFailedToLoad(@NonNull LoadAdError loadAdError) {
                        ADSUtilitis.trackScreen(context, "Inter_Fail");
                        ADSUtilitis.MassageBoxFullDismiss();
                        LoadingCheck = true;
                        if (showingInterstitial == null) {
                            ADSAdmobmInterstitial = null;
                        }
                        if (showWhenLoaded && onFinishAds != null) {
                            onFinishAds.onFinishAds(true);
                        }
                    }
                });
    }

    private static void onAdsLoadAdListener(
            Context context,
            @NonNull InterstitialAd ad,
            @NonNull String adsId
    ) {
        ad.setFullScreenContentCallback(new FullScreenContentCallback() {
            @Override
            public void onAdClicked() {
            }

            @Override
            public void onAdDismissedFullScreenContent() {
                lastInterstitialClosedAtMs = System.currentTimeMillis();
                AdsDisplayCheck(true);
                if (context instanceof Activity) {
                    FullscreenAdWindowHelper.restore((Activity) context);
                }
                if (showingInterstitial == ad) {
                    showingInterstitial = null;
                }
                // Never wipe a next PreLoad that finished while this ad was open.
                if (ADSAdmobmInterstitial == ad) {
                    ADSAdmobmInterstitial = null;
                }
                if (onFinishAds != null) onFinishAds.onFinishAds(true);
                // PreLoad: after show → load next ad for the following 1,2,3 cycle
                if (context instanceof Activity) {
                    preloadNext((Activity) context, adsId);
                }
            }

            @Override
            public void onAdFailedToShowFullScreenContent(AdError adError) {
                AdsDisplayCheck(true);
                if (context instanceof Activity) {
                    FullscreenAdWindowHelper.restore((Activity) context);
                }
                if (showingInterstitial == ad) {
                    showingInterstitial = null;
                }
                if (ADSAdmobmInterstitial == ad) {
                    ADSAdmobmInterstitial = null;
                }
                if (onFinishAds != null) onFinishAds.onFinishAds(true);
                if (context instanceof Activity) {
                    preloadNext((Activity) context, adsId);
                }
            }

            @Override
            public void onAdImpression() {
            }

            @Override
            public void onAdShowedFullScreenContent() {
            }
        });
    }

    private static void showInterstitial(Activity activity, InterstitialAd ad) {
        FullscreenAdWindowHelper.prepare(activity);
        ad.show(activity);
    }

    public interface OnFinishAds {
        void onFinishAds(boolean b);
    }

    /** Cached interstitial of the unlock ad unit (kept apart from the normal inter cache). */
    @Nullable
    private static InterstitialAd unlockInterstitial;
    @Nullable
    private static String unlockInterstitialId;
    private static boolean unlockInterLoading;

    /** Caches the next unlock interstitial for [adsId]; the caller only calls it in PreLoad mode. */
    public static void preloadForUnlock(@Nullable Activity activity, @Nullable String adsId) {
        if (activity == null || activity.isFinishing() || activity.isDestroyed()) return;
        if (ADSMainClass.getAds_Free()) return;
        String id = adsId != null ? adsId.trim() : "";
        if (id.isEmpty() || unlockInterLoading) return;
        if (unlockInterstitial != null && id.equals(unlockInterstitialId)) return;
        if (!ADSConsentManager.canRequestAds(activity)) return;

        unlockInterLoading = true;
        unlockInterstitial = null;
        InterstitialAd.load(activity.getApplicationContext(), id, new AdRequest.Builder().build(),
                new InterstitialAdLoadCallback() {
                    @Override
                    public void onAdLoaded(@NonNull InterstitialAd interstitialAd) {
                        unlockInterLoading = false;
                        unlockInterstitial = interstitialAd;
                        unlockInterstitialId = id;
                    }

                    @Override
                    public void onAdFailedToLoad(@NonNull LoadAdError loadAdError) {
                        unlockInterLoading = false;
                    }
                });
    }

    /** Result of an interstitial that must really be shown (unlock ads). */
    public interface UnlockListener {
        /** Ad was shown and closed. */
        void onClosed();

        /** Ad could not be loaded / shown. */
        void onFailed();
    }

    /**
     * Shows an interstitial now, ignoring the click counter and {@code inter_ads_show}.
     * Load type is decided by the caller (unlock_items.ads_load_type): a cached ad is used when
     * present, otherwise one is loaded on demand. Reports failure instead of skipping.
     */
    public static void showForUnlock(
            @NonNull Activity activity,
            @Nullable String adsId,
            @NonNull UnlockListener listener
    ) {
        String id = adsId != null ? adsId.trim() : "";
        if (id.isEmpty() || activity.isFinishing() || activity.isDestroyed()) {
            listener.onFailed();
            return;
        }

        if (unlockInterstitial != null
                && id.equals(unlockInterstitialId)) {
            InterstitialAd cached = unlockInterstitial;
            unlockInterstitial = null;
            showUnlockAd(activity, cached, id, listener);
            return;
        }

        if (!ADSConsentManager.canRequestAds(activity)) {
            listener.onFailed();
            return;
        }

        ADSUtilitis.MassageBoxFull(activity);
        InterstitialAd.load(activity, id, new AdRequest.Builder().build(),
                new InterstitialAdLoadCallback() {
                    @Override
                    public void onAdLoaded(@NonNull InterstitialAd interstitialAd) {
                        ADSUtilitis.MassageBoxFullDismiss();
                        ADSUtilitis.trackScreen(activity, "Inter_Load");
                        showUnlockAd(activity, interstitialAd, id, listener);
                    }

                    @Override
                    public void onAdFailedToLoad(@NonNull LoadAdError loadAdError) {
                        ADSUtilitis.MassageBoxFullDismiss();
                        ADSUtilitis.trackScreen(activity, "Inter_Fail");
                        listener.onFailed();
                    }
                });
    }

    private static void showUnlockAd(
            @NonNull Activity activity,
            @NonNull InterstitialAd ad,
            @NonNull String adsId,
            @NonNull UnlockListener listener
    ) {
        if (activity.isFinishing() || activity.isDestroyed()) {
            listener.onFailed();
            return;
        }
        ad.setFullScreenContentCallback(new FullScreenContentCallback() {
            @Override
            public void onAdDismissedFullScreenContent() {
                lastInterstitialClosedAtMs = System.currentTimeMillis();
                AdsDisplayCheck(true);
                FullscreenAdWindowHelper.restore(activity);
                listener.onClosed();
                preloadForUnlock(activity, adsId);
            }

            @Override
            public void onAdFailedToShowFullScreenContent(@NonNull AdError adError) {
                AdsDisplayCheck(true);
                FullscreenAdWindowHelper.restore(activity);
                listener.onFailed();
                preloadForUnlock(activity, adsId);
            }
        });
        AdsDisplayCheck(false);
        showInterstitial(activity, ad);
    }
}

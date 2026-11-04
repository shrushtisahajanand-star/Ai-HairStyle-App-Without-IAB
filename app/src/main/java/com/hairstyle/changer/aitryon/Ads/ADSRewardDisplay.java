package com.hairstyle.changer.aitryon.Ads;

import android.app.Activity;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.gms.ads.AdError;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.FullScreenContentCallback;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.OnUserEarnedRewardListener;
import com.google.android.gms.ads.rewarded.RewardItem;
import com.google.android.gms.ads.rewarded.RewardedAd;
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback;

/**
 * Rewarded ads â€” always {@code onLoad} (load now â†’ show).
 * <ul>
 *   <li>{@code reward_ads_show = true} â†’ load &amp; show; reward only via {@code onUserEarnedReward}</li>
 *   <li>{@code reward_ads_show = false} (or ads free) â†’ skip ad, grant reward immediately (free users)</li>
 * </ul>
 */
public final class ADSRewardDisplay {

    private static final String TAG = "ADSRewardDisplay";

    private static boolean isLoading;

    /** Cached rewarded ad for unlock ads (unlock_items.ads_load_type = PreLoad); generate and save always load on demand. */
    @Nullable
    private static RewardedAd preloadedAd;
    private static long preloadedAtMs;
    /** Ad unit the cached ad was loaded for (generate and unlock use different units). */
    @Nullable
    private static String preloadedUnitId;
    private static boolean isPreloading;
    /** Rewarded ads expire after about an hour; drop older cached ones. */
    private static final long PRELOAD_MAX_AGE_MS = 50L * 60L * 1000L;

    private ADSRewardDisplay() {
    }

    /** Caches the next rewarded ad for unlock ads. The caller decides when (PreLoad mode only). */
    public static void preload(
            @NonNull Activity activity,
            @Nullable String adUnitId,
            boolean ignoreRewardFlag
    ) {
        if (ADSMainClass.getAds_Free()) return;
        if (!ignoreRewardFlag && !ADSMainClass.getRewardAdsShow()) return;
        if (activity.isFinishing() || activity.isDestroyed()) return;
        if (isPreloading || isLoading) return;
        if (!ADSConsentManager.canRequestAds(activity)) return;

        String unitId = adUnitId != null ? adUnitId.trim() : "";
        if (unitId.isEmpty()) {
            unitId = ADSMainClass.getStringValue(ADSMainClass.REWARD_AD_ID).trim();
        }
        if (unitId.isEmpty()) return;
        if (preloadedAd != null && unitId.equals(preloadedUnitId)
                && System.currentTimeMillis() - preloadedAtMs <= PRELOAD_MAX_AGE_MS) return;
        final String loadUnitId = unitId;

        isPreloading = true;
        preloadedAd = null;
        android.util.Log.d(TAG, "Reward preload start: unit=" + unitId);
        RewardedAd.load(
                activity.getApplicationContext(),
                unitId,
                new AdRequest.Builder().build(),
                new RewardedAdLoadCallback() {
                    @Override
                    public void onAdLoaded(@NonNull RewardedAd rewardedAd) {
                        isPreloading = false;
                        preloadedAd = rewardedAd;
                        preloadedAtMs = System.currentTimeMillis();
                        preloadedUnitId = loadUnitId;
                        android.util.Log.d(TAG, "Reward preload loaded: unit=" + loadUnitId);
                    }

                    @Override
                    public void onAdFailedToLoad(@NonNull LoadAdError loadAdError) {
                        isPreloading = false;
                        android.util.Log.w(TAG, "Reward preload fail: code=" + loadAdError.getCode() + " " + loadAdError.getMessage() + " unit=" + loadUnitId);
                    }
                }
        );
    }

    @Nullable
    private static RewardedAd takePreloaded(@NonNull String unitId) {
        if (!unitId.equals(preloadedUnitId)) return null;
        RewardedAd ad = preloadedAd;
        preloadedAd = null;
        if (ad == null || System.currentTimeMillis() - preloadedAtMs > PRELOAD_MAX_AGE_MS) return null;
        return ad;
    }

    public interface Listener {
        /** User earned reward (watched ad) OR ads were skipped by config. */
        void onRewardEarned();

        /** Load or show failed (only when ads were required). */
        void onAdFailed();

        /** Ad dismissed without earning reward. */
        void onClosedWithoutReward();
    }

    /**
     * Always onLoad: loading dialog â†’ RewardedAd.load â†’ show.
     * If {@link ADSMainClass#getRewardAdsShow()} is false â†’ skip and call {@link Listener#onRewardEarned()}.
     */
    public static void loadAndShow(
            @NonNull Activity activity,
            @Nullable String adUnitId,
            @NonNull Listener listener
    ) {
        loadAndShow(activity, adUnitId, listener, false);
    }

    /** @param ignoreRewardFlag {@code true} for unlock ads: always show, ignore {@code reward_ads_show}. */
    public static void loadAndShow(
            @NonNull Activity activity,
            @Nullable String adUnitId,
            @NonNull Listener listener,
            boolean ignoreRewardFlag
    ) {
        loadAndShow(activity, adUnitId, listener, ignoreRewardFlag, 0L);
    }

    /**
     * @param loadTimeoutMs when &gt; 0 and the ad is not loaded in that time, the loading dialog is
     *                      closed and {@link Listener#onAdFailed()} is called; a late ad is dropped.
     */
    public static void loadAndShow(
            @NonNull Activity activity,
            @Nullable String adUnitId,
            @NonNull Listener listener,
            boolean ignoreRewardFlag,
            long loadTimeoutMs
    ) {
        if (activity.isFinishing() || activity.isDestroyed()) {
            listener.onAdFailed();
            return;
        }

        // Free users: reward_ads_show false â†’ skip ads, grant action
        if (ADSMainClass.getAds_Free() || (!ignoreRewardFlag && !ADSMainClass.getRewardAdsShow())) {
            android.util.Log.d(TAG, "Reward ads skipped (reward_ads_show=false or ads free)");
            listener.onRewardEarned();
            return;
        }

        if (!ADSConsentManager.canRequestAds(activity)) {
            android.util.Log.w(TAG, "Reward load blocked: consent (canRequestAds=false)");
            listener.onAdFailed();
            return;
        }

        String unitId = adUnitId != null ? adUnitId.trim() : "";
        if (unitId.isEmpty()) {
            unitId = ADSMainClass.getStringValue(ADSMainClass.REWARD_AD_ID).trim();
        }
        if (unitId.isEmpty()) {
            android.util.Log.w(TAG, "Reward load fail: empty ad unit");
            listener.onAdFailed();
            return;
        }

        RewardedAd cached = ignoreRewardFlag ? takePreloaded(unitId) : null;
        if (cached != null) {
            android.util.Log.d(TAG, "Reward using preloaded ad: unit=" + unitId);
            showLoadedAd(activity, cached, listener);
            preload(activity, unitId, ignoreRewardFlag);
            return;
        }

        if (isLoading) {
            android.util.Log.d(TAG, "Reward already loading");
            return;
        }

        isLoading = true;
        android.util.Log.d(TAG, "Reward load start: unit=" + unitId);
        ADSUtilitis.MassageBoxFull(activity);

        final String finalUnitId = unitId;
        AdRequest request = new AdRequest.Builder().build();
        final boolean[] timedOut = {false};
        final android.os.Handler timeoutHandler = new android.os.Handler(android.os.Looper.getMainLooper());
        if (loadTimeoutMs > 0) {
            timeoutHandler.postDelayed(() -> {
                if (!isLoading) return;
                timedOut[0] = true;
                isLoading = false;
                ADSUtilitis.MassageBoxFullDismiss();
                android.util.Log.w(TAG, "Reward load timeout after " + loadTimeoutMs + "ms");
                listener.onAdFailed();
            }, loadTimeoutMs);
        }
        // Always onLoad â€” no PreLoad path
        RewardedAd.load(
                activity,
                finalUnitId,
                request,
                new RewardedAdLoadCallback() {
                    @Override
                    public void onAdLoaded(@NonNull RewardedAd rewardedAd) {
                        timeoutHandler.removeCallbacksAndMessages(null);
                        if (timedOut[0]) return; // caller already moved on
                        isLoading = false;
                        ADSUtilitis.MassageBoxFullDismiss();
                        ADSUtilitis.trackScreen(activity, "Reward_Load");
                        android.util.Log.d(TAG, "Reward loaded: unit=" + finalUnitId);
                        showLoadedAd(activity, rewardedAd, listener);
                    }

                    @Override
                    public void onAdFailedToLoad(@NonNull LoadAdError loadAdError) {
                        timeoutHandler.removeCallbacksAndMessages(null);
                        if (timedOut[0]) return;
                        isLoading = false;
                        ADSUtilitis.MassageBoxFullDismiss();
                        ADSUtilitis.trackScreen(activity, "Reward_Fail");
                        android.util.Log.w(TAG, "Reward load fail: code=" + loadAdError.getCode() + " " + loadAdError.getMessage() + " unit=" + finalUnitId);
                        listener.onAdFailed();
                    }
                }
        );
    }

    private static void showLoadedAd(
            @NonNull Activity activity,
            @NonNull RewardedAd rewardedAd,
            @NonNull Listener listener
    ) {
        final boolean[] earned = {false};

        rewardedAd.setFullScreenContentCallback(new FullScreenContentCallback() {
            @Override
            public void onAdDismissedFullScreenContent() {
                ADSAppStarting.ADSIsShowing = false;
                ADSAppManage.FastStart = false;
                FullscreenAdWindowHelper.restore(activity);
                android.util.Log.d(TAG, "Reward dismissed: earned=" + earned[0]);
                if (!earned[0]) {
                    listener.onClosedWithoutReward();
                }
            }

            @Override
            public void onAdFailedToShowFullScreenContent(@NonNull AdError adError) {
                ADSAppStarting.ADSIsShowing = false;
                ADSAppManage.FastStart = false;
                FullscreenAdWindowHelper.restore(activity);
                android.util.Log.w(TAG, "Reward show fail: " + adError.getMessage());
                if (!earned[0]) {
                    listener.onAdFailed();
                }
            }

            @Override
            public void onAdShowedFullScreenContent() {
                ADSAppStarting.ADSIsShowing = true;
                ADSAppManage.FastStart = true;
                android.util.Log.d(TAG, "Reward shown");
            }
        });

        if (activity.isFinishing() || activity.isDestroyed()) {
            listener.onAdFailed();
            return;
        }

        FullscreenAdWindowHelper.prepare(activity);
        rewardedAd.show(activity, new OnUserEarnedRewardListener() {
            @Override
            public void onUserEarnedReward(@NonNull RewardItem rewardItem) {
                earned[0] = true;
                ADSUtilitis.trackScreen(activity, "Reward_Earned");
                android.util.Log.d(TAG, "Reward earned: type=" + rewardItem.getType() + " amount=" + rewardItem.getAmount());
                listener.onRewardEarned();
            }
        });
    }
}

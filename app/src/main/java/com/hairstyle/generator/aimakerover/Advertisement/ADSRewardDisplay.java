package com.hairstyle.generator.aimakerover.Advertisement;

import android.app.Activity;
import android.util.Log;

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

    private ADSRewardDisplay() {
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
        if (activity.isFinishing() || activity.isDestroyed()) {
            listener.onAdFailed();
            return;
        }

        // Free users: reward_ads_show false â†’ skip ads, grant action
        if (ADSMainClass.getAds_Free() || !ADSMainClass.getRewardAdsShow()) {
            // Log.d(TAG, "Reward ads skipped (reward_ads_show=false or ads free)");
            listener.onRewardEarned();
            return;
        }

        if (!ADSConsentManager.canRequestAds(activity)) {
            // Log.d(TAG, "Reward load blocked: consent");
            listener.onAdFailed();
            return;
        }

        String unitId = adUnitId != null ? adUnitId.trim() : "";
        if (unitId.isEmpty()) {
            unitId = ADSMainClass.getStringValue(ADSMainClass.REWARD_AD_ID).trim();
        }
        if (unitId.isEmpty()) {
            // Log.d(TAG, "Reward load fail: empty ad unit");
            listener.onAdFailed();
            return;
        }

        if (isLoading) {
            // Log.d(TAG, "Reward already loading");
            return;
        }

        isLoading = true;
        ADSUtilitis.MassageBoxFull(activity);

        final String finalUnitId = unitId;
        AdRequest request = new AdRequest.Builder().build();
        // Always onLoad â€” no PreLoad path
        RewardedAd.load(
                activity,
                finalUnitId,
                request,
                new RewardedAdLoadCallback() {
                    @Override
                    public void onAdLoaded(@NonNull RewardedAd rewardedAd) {
                        isLoading = false;
                        ADSUtilitis.MassageBoxFullDismiss();
                        ADSUtilitis.trackScreen(activity, "Reward_Load");
                        showLoadedAd(activity, rewardedAd, listener);
                    }

                    @Override
                    public void onAdFailedToLoad(@NonNull LoadAdError loadAdError) {
                        isLoading = false;
                        ADSUtilitis.MassageBoxFullDismiss();
                        ADSUtilitis.trackScreen(activity, "Reward_Fail");
                        // Log.e(TAG, "Reward load fail: " + loadAdError.getMessage());
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
                if (!earned[0]) {
                    listener.onClosedWithoutReward();
                }
            }

            @Override
            public void onAdFailedToShowFullScreenContent(@NonNull AdError adError) {
                ADSAppStarting.ADSIsShowing = false;
                ADSAppManage.FastStart = false;
                FullscreenAdWindowHelper.restore(activity);
                // Log.e(TAG, "Reward show fail: " + adError.getMessage());
                if (!earned[0]) {
                    listener.onAdFailed();
                }
            }

            @Override
            public void onAdShowedFullScreenContent() {
                ADSAppStarting.ADSIsShowing = true;
                ADSAppManage.FastStart = true;
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
                listener.onRewardEarned();
            }
        });
    }
}

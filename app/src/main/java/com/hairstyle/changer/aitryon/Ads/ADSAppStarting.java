package com.hairstyle.changer.aitryon.Ads;


import android.app.Activity;
import android.content.Context;
import android.os.Bundle;

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

public class ADSAppStarting {

    public static OnFinishAds onFinishAds;
    public static boolean LoadingCheck = true;
    public static AppOpenAd AppStartingAd;
    public static boolean ADSIsShowing = false;
    private static InterstitialAd AdmobmInterstitialAd;

    public static void AdsSplashAppStartingDisplay(Activity context, OnFinishAds onFinishAd, boolean... doShowAds) {
        onFinishAds = onFinishAd;
        if (ADSMainClass.getAds_Free()) {
            onFinishAds.onFinishAds(true);
            return;
        }

        ADSSplashAppStartingLoad(context);
    }

    public static void ADSSplashAppStartingLoad(Activity context) {
//        if (ADSMainClass.getAdsTypeManage().equals("Load")) {
//            ADSUtilitis.MassageBoxFull(context);
//        }

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
//            if (ADSMainClass.getAdsTypeManage().equals("Load")) {
//                ADSUtilitis.MassageBoxFullDismiss();
//            }
            onFinishAds.onFinishAds(true);
            return;
        }

        String appOpenId = ADSMainClass.getStringValue(ADSMainClass.APP_OPEN_ID);

        if (appOpenId == null || appOpenId.isEmpty()) {
            // No AppOpen id — fall back to interstitial if available, else skip
            AdmobInterstitialAd(context);
            return;
        }

        if (ADSAppBackground.isAppOpenAdReady()) {
            ADSAppBackground.showPreloadedAppOpenForSplash(context, onFinishAds);
        } else {
            loadAndShowAppOpen(context, appOpenId);
        }
    }

    private static void loadAndShowAppOpen(Activity context, String appOpenId) {
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
                    ADSAppBackground.showPreloadedAppOpenForSplash(context, onFinishAds);
                }

                @Override
                public void onAdFailedToLoad(@NonNull LoadAdError loadAdError) {
//                    ADSUtilitis.MassageBoxFullDismiss();
                    ADSUtilitis.trackScreen(context, "Splash_AppOpen_Fail");
                    // Fallback to interstitial when AppOpen fails
                    AdmobInterstitialAd(context);
                }
            });
        } catch (Exception e) {
//            ADSUtilitis.MassageBoxFullDismiss();
            onFinishAds.onFinishAds(true);
        }
    }

    private static void AdmobInterstitialAd(Activity context) {
        String interstitialId = ADSMainClass.getStringValue(ADSMainClass.INTER_FIRST_TIME);
        if (interstitialId == null || interstitialId.isEmpty()) {
//            ADSUtilitis.MassageBoxFullDismiss();
            onFinishAds.onFinishAds(true);
            return;
        }

//        if (ADSMainClass.getAdsTypeManage().equals("Load")) {
//            ADSUtilitis.MassageBoxFull(context);
//        }

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
//                        if (ADSMainClass.getAdsTypeManage().equals("Load")) {
//                            ADSUtilitis.MassageBoxFullDismiss();
//                        }
                        AdmobmInterstitialAd.show(context);
                        onAdsLoadAdListener(context);
                    }

                    @Override
                    public void onAdFailedToLoad(@NonNull LoadAdError loadAdError) {
//                        ADSUtilitis.MassageBoxFullDismiss();
                        onFinishAds.onFinishAds(true);
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
                onFinishAds.onFinishAds(true);
            }

            @Override
            public void onAdFailedToShowFullScreenContent(AdError adError) {
                onFinishAds.onFinishAds(true);
            }

            @Override
            public void onAdImpression() {
            }

            @Override
            public void onAdShowedFullScreenContent() {
            }
        });
    }

    public interface OnFinishAds {
        void onFinishAds(boolean b);
    }

}

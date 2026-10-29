//package com.hairstyle.changer.aitryon.Advertisement;
//
//
//import android.app.Activity;
//import android.content.Context;
//import android.util.Log;
//
//import androidx.annotation.NonNull;
//
//import com.google.android.gms.ads.AdError;
//import com.google.android.gms.ads.AdRequest;
//import com.google.android.gms.ads.AdValue;
//import com.google.android.gms.ads.FullScreenContentCallback;
//import com.google.android.gms.ads.LoadAdError;
//import com.google.android.gms.ads.OnPaidEventListener;
//import com.google.android.gms.ads.appopen.AppOpenAd;
//import com.google.android.gms.ads.interstitial.InterstitialAd;
//import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback;
//
//@SuppressWarnings("all")
//public class CallEndInterAd {
//
//    public static int adsClickEvent = 0;
//    public static int adsBackClick = 0;
//    public static Boolean isAdsShowEnable = false;
//    public static int fullScreenAdsPosition = 0;
//    public static boolean fullScreenAdsFailed = false;
//    public static OnCompeteAds onCompleteAdCallBack;
//    public static boolean isAdsEnabled = true;
//    private static InterstitialAd fullScreenAds;
//    private static AppOpenAd appOpenAd;
//    public static boolean isAdShowing = true;
//    public static void fullScreenAdShow(Activity context, OnCompeteAds onFinishAd, boolean... doShowAds) {
//        onCompleteAdCallBack = onFinishAd;
//        if (ADSMainClass.getAds_Free()) {
//            if (onCompleteAdCallBack != null) {
//                onCompleteAdCallBack.onCompeteAds(true);
//            }
//            return;
//        }
//
//        if (ADSMainClass.getCallEndInterAdsType().equalsIgnoreCase("appopen")) {
//            admobAppOpenAd(context);
//        } else {
//            admobFullScreenAd(context);
//        }
//    }
//
//    public static void admobAppOpenAd(Activity context) {
//        if (appOpenAd != null) {
//            adsShowCheckEvent(false);
//            try {
//                appOpenAd.show(context);
//                appOpenAd = null;
//            } catch (Exception e) {
//                appOpenAd = null;
//                adsShowCheckEvent(true);
//                if (onCompleteAdCallBack != null) {
//                    onCompleteAdCallBack.onCompeteAds(false);
//                }
//            }
//        } else {
//            adsShowCheckEvent(true);
//            if (onCompleteAdCallBack != null) {
//                onCompleteAdCallBack.onCompeteAds(false);
//                onCompleteAdCallBack = null;
//            }
//        }
//
//        if (isAdsEnabled) {
//            isAdsEnabled = false;
//            admobAppOpenAdLoad(context);
//        }
//    }
//
//    public static void admobFullScreenAd(Activity context) {
//
//        if (fullScreenAds != null) {
////            Log.d("TAG", "CallEndAd: ad is ready, showing now (22222)");
//            adsShowCheckEvent(false);
//            try {
//                fullScreenAds.show(context);
//                fullScreenAds = null; // Important: Clear the ad after showing so a new one can be loaded
//            } catch (Exception e) {
////                Log.e("TAG", "CallEndAd: Error showing ad: " + e.getMessage());
//                fullScreenAds = null;
//                adsShowCheckEvent(true);
//                if (onCompleteAdCallBack != null) {
//                    onCompleteAdCallBack.onCompeteAds(false);
//                }
//            }
//
//        } else {
////            Log.d("TAG", "CallEndAd: ad is NULL, cannot show (33333)");
//            adsShowCheckEvent(true);
//            if (onCompleteAdCallBack != null) {
//                onCompleteAdCallBack.onCompeteAds(false); // Return false because show failed
//                onCompleteAdCallBack = null;
//            }
//        }
//
////        if (isAdsEnabled) {
//////            Log.d("TAG", "CallEndAd: Triggering reload for next time (44444)");
////            isAdsEnabled = false;
////            if (ADSMainClass.getCallEndInterAdsType().equalsIgnoreCase("appopen")) {
////                admobAppOpenAdLoad(context);
////            } else {
////                admobFullScreenAdLoad(context);
////            }
////        }
//    }
//
//    public static void admobAppOpenAdLoad(Activity context) {
//        if (appOpenAd != null) {
//            isAdsEnabled = true;
//            return;
//        }
//
//        AdRequest adRequest = new AdRequest.Builder().build();
//        AppOpenAd.load(context, ADSMainClass.getStringValue(ADSMainClass.APP_OPEN_ID), adRequest,
//                new AppOpenAd.AppOpenAdLoadCallback() {
//                    @Override
//                    public void onAdLoaded(@NonNull AppOpenAd ad) {
//                        ad.setOnPaidEventListener(adValue -> {
//                            ADSUtilitis.logAdRevenue(context, adValue);
//                        });
//
//                        isAdsEnabled = true;
//                        appOpenAd = ad;
//                        ADSUtilitis.trackScreen(context, "CallEndAppOpen_Load");
//                        onAppOpenListner(context);
//                    }
//
//                    @Override
//                    public void onAdFailedToLoad(@NonNull LoadAdError loadAdError) {
//                        ADSUtilitis.trackScreen(context, "CallEndAppOpen_Fail");
//                        isAdsEnabled = true;
//                    }
//                });
//    }
//
//    private static void onAppOpenListner(Activity context) {
//        appOpenAd.setFullScreenContentCallback(new FullScreenContentCallback() {
//            @Override
//            public void onAdDismissedFullScreenContent() {
//                adsShowCheckEvent(true);
//                if (onCompleteAdCallBack != null) {
//                    onCompleteAdCallBack.onCompeteAds(true);
//                    onCompleteAdCallBack = null;
//                }
//            }
//
//            @Override
//            public void onAdFailedToShowFullScreenContent(AdError adError) {
////                ADSUtilitis.trackScreen(context, "CallEndInter_FailedToShow");
//                adsShowCheckEvent(true);
//                if (onCompleteAdCallBack != null) {
//                    onCompleteAdCallBack.onCompeteAds(false);
//                    onCompleteAdCallBack = null;
//                }
//            }
//        });
//    }
//
//    public static void adsShowCheckEvent(Boolean aBoolean) {
//
//        if (aBoolean) {
//            isAdShowing = false;
////            AdViewController.isAppFastStart = false;
//            isAdsShowEnable = true;
//        } else {
//            isAdShowing = true;
//            isAdsShowEnable = false;
////            AdViewController.isAppFastStart = true;
//        }
//
//    }
//
////    public static void fullScreenInterstailAdsId( List<String> interAdsIds) {
////        try {
////            if (interAdsIds != null && interAdsIds.size() != 0 && interAdsIds.size() != fullScreenAdsPosition) {
////                fullScreenAdsFailed = true;
////                SetterMethodAdView.set_CallEndInter_AdsUnit_Id(interAdsIds.get(fullScreenAdsPosition));
////                fullScreenAdsPosition = fullScreenAdsPosition + 1;
////            } else {
////                fullScreenAdsFailed = false;
////            }
////        } catch (NullPointerException n) {
////            n.printStackTrace();
////        } catch (Exception e) {
////            e.printStackTrace();
////        }
////    }
//
//    public static void loadAd(Activity context) {
//        if (ADSMainClass.getCallEndInterAdsType().equalsIgnoreCase("appopen")) {
//            admobAppOpenAdLoad(context);
//        } else {
//            admobFullScreenAdLoad(context);
//        }
//    }
//
//    private static void admobFullScreenAdLoad(Activity context) {
//        if (fullScreenAds != null) {
//            isAdsEnabled = true;
//            return;
//        }
//        AdRequest adRequest = new AdRequest.Builder().build();
//
//        InterstitialAd.load(context, ADSMainClass.getStringValue(ADSMainClass.CALL_END_Inter), adRequest,
//                new InterstitialAdLoadCallback() {
//                    @Override
//                    public void onAdLoaded(@NonNull InterstitialAd interstitialAd) {
//                        interstitialAd.setOnPaidEventListener(new OnPaidEventListener() {
//                            @Override
//                            public void onPaidEvent(AdValue adValue) {
//                                ADSUtilitis.logAdRevenue(context, adValue);
//                            }
//                        });
//
//                        isAdsEnabled = true;
//                        CallEndInterAd.fullScreenAds = interstitialAd;
//                        CallEndInterAd.fullScreenAds = interstitialAd;
//                        ADSUtilitis.trackScreen(context, "CallEndInter_Load");
//                        onContactListner(context);
//                    }
//
//                    @Override
//                    public void onAdFailedToLoad(@NonNull LoadAdError loadAdError) {
//                        ADSUtilitis.trackScreen(context, "CallEndInter_Fail");
//                        isAdsEnabled = true;
//                    }
//                });
//    }
//
//    public static void onContactListner(Context context) {
//        fullScreenAds.setFullScreenContentCallback(new FullScreenContentCallback() {
//            @Override
//            public void onAdClicked() {
//            }
//
//            @Override
//            public void onAdDismissedFullScreenContent() {
//                adsShowCheckEvent(true);
//                if (onCompleteAdCallBack != null) {
//                    onCompleteAdCallBack.onCompeteAds(true);
//                    onCompleteAdCallBack = null;
//                }
//            }
//
//            @Override
//            public void onAdFailedToShowFullScreenContent(AdError adError) {
////                ADSUtilitis.trackScreen(context, "CallEndInter_FailedToShow");
//                adsShowCheckEvent(true);
//                if (onCompleteAdCallBack != null) {
//                    onCompleteAdCallBack.onCompeteAds(false);
//                    onCompleteAdCallBack = null;
//                }
//            }
//
//            @Override
//            public void onAdImpression() {
//            }
//
//            @Override
//            public void onAdShowedFullScreenContent() {
//            }
//        });
//    }
//
//    public interface OnCompeteAds {
//        void onCompeteAds(boolean b);
//    }
//
//}

package com.hairstyle.generator.aimakerover.Advertisement;



import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.RatingBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import com.facebook.ads.NativeAdLayout;
import com.facebook.shimmer.ShimmerFrameLayout;
import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.AdLoader;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdValue;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.MediaContent;
import com.google.android.gms.ads.OnPaidEventListener;
import com.google.android.gms.ads.VideoController;
import com.google.android.gms.ads.VideoOptions;
import com.google.android.gms.ads.nativead.NativeAd;
import com.google.android.gms.ads.nativead.NativeAdOptions;
import com.google.android.gms.ads.nativead.NativeAdView;
import com.hairstyle.generator.aimakeover.R;

import java.lang.ref.WeakReference;
import java.util.Objects;

public class ADSNativeDisplay {
    public static final String SIZE_SMALL = "small";
    public static final String SIZE_MEDIUM = "medium";
    public static final String SIZE_BIG = "big";

    public static int ArrayIndex = 0;
    public static boolean FailArrayId = false;
    public static Context contexts;
    public static int NativeByPage = 0;
    //    public static com.facebook.ads.NativeAd FBNativeAdpater;
    public static NativeAdLayout NativeAdLayout;
    public static boolean LoadingCheck = true;
    public static NativeAd AdmobNativeAd;
    /** Current native layout size: {@link #SIZE_SMALL}, {@link #SIZE_MEDIUM}, or {@link #SIZE_BIG}. */
    public static String AdsDisplayType = SIZE_SMALL;

    /** True while Splash PreLoad request is in flight (do not hide Language native during this). */
    private static boolean isNativePreloading = false;
    private static WeakReference<FrameLayout> pendingNativeContainer;
    private static WeakReference<ShimmerFrameLayout> pendingNativeShimmer;
    private static WeakReference<Context> pendingNativeContext;
    private static String pendingNativeAdsId;
    private static String pendingNativeSize = SIZE_SMALL;

    /**
     * Firebase size values only: {@code small} | {@code medium} | {@code big}.
     * Also accepts {@code mid} → medium. Anything else → default {@link #SIZE_SMALL}.
     */
    public static String normalizeNativeSize(String size) {
        if (size == null || size.trim().isEmpty()) {
            return SIZE_SMALL;
        }
        String s = size.trim().toLowerCase();
        if (SIZE_BIG.equals(s)) {
            return SIZE_BIG;
        }
        if (SIZE_MEDIUM.equals(s) || "mid".equals(s)) {
            return SIZE_MEDIUM;
        }
        if (SIZE_SMALL.equals(s)) {
            return SIZE_SMALL;
        }
        return SIZE_SMALL;
    }

    private static void setDisplaySize(String size) {
        AdsDisplayType = normalizeNativeSize(size);
    }

    private static int shimmerLayoutForSize(String size) {
        switch (normalizeNativeSize(size)) {
            case SIZE_BIG:
                return R.layout.layout_big_native_ad_load;
            case SIZE_MEDIUM:
                return R.layout.layout_medium_native_ad_load;
            case SIZE_SMALL:
            default:
                return R.layout.layout_small_native_ad_load;
        }
    }

    /** Swap shimmer child to match native ad size (small / medium / big). */
    private static void applyNativeShimmerPlaceholder(ShimmerFrameLayout shimmerFrameLayout, String size) {
        if (shimmerFrameLayout == null) {
            return;
        }
        Context ctx = shimmerFrameLayout.getContext();
        if (ctx == null) {
            return;
        }
        shimmerFrameLayout.removeAllViews();
        LayoutInflater.from(ctx).inflate(shimmerLayoutForSize(size), shimmerFrameLayout, true);
    }

    private static void hideNativeShimmer(ShimmerFrameLayout shimmerFrameLayout, FrameLayout linearLayout) {
        Runnable hideAction = () -> {
            if (shimmerFrameLayout != null) {
                shimmerFrameLayout.stopShimmer();
                shimmerFrameLayout.setVisibility(View.GONE);
            }
            if (linearLayout != null) {
                linearLayout.setVisibility(View.GONE);
            }
        };
        if (shimmerFrameLayout != null) {
            shimmerFrameLayout.post(hideAction);
        } else if (linearLayout != null) {
            linearLayout.post(hideAction);
        } else {
            hideAction.run();
        }
    }

    private static void showNativeShimmer(ShimmerFrameLayout shimmerFrameLayout, FrameLayout linearLayout) {
        showNativeShimmer(shimmerFrameLayout, linearLayout, AdsDisplayType);
    }

    private static void showNativeShimmer(
            ShimmerFrameLayout shimmerFrameLayout,
            FrameLayout linearLayout,
            String size
    ) {
        if (linearLayout != null) {
            linearLayout.setVisibility(View.GONE);
        }
        if (shimmerFrameLayout == null) {
            return;
        }
        applyNativeShimmerPlaceholder(shimmerFrameLayout, size);
        shimmerFrameLayout.setVisibility(View.VISIBLE);
        shimmerFrameLayout.setLayerType(View.LAYER_TYPE_SOFTWARE, null);
        shimmerFrameLayout.post(() -> {
            if (shimmerFrameLayout.getVisibility() == View.VISIBLE) {
                shimmerFrameLayout.startShimmer();
            }
        });
    }

    /**
     * Splash PreLoad: load native into {@link #AdmobNativeAd} without attaching to UI.
     * Used when {@code Ads_Loading_Types = PreLoad}.
     * Language / other screens then show instantly via {@link #AdmobBigDisplay}.
     * <p>
     * Important: do not gate Language display on {@link #LoadingCheck} alone — when Splash
     * AppOpen/Inter is skipped, Language opens while preload is still in flight. That used to
     * hide the native slot ({@code LoadingCheck == false}). Pending UI is delivered when preload finishes.
     */
    public static void preloadNativeAd(Context context, String adsId) {
        if (context == null) return;
        if (ADSMainClass.getAds_Free()) return;
        if (adsId == null || adsId.isEmpty()) return;

        String type = ADSMainClass.getAdsTypeManage();
        if (type == null || !type.equalsIgnoreCase("PreLoad")) {
            return;
        }
        if (AdmobNativeAd != null || isNativePreloading) {
            return;
        }

        contexts = context.getApplicationContext();
        isNativePreloading = true;
        // Keep LoadingCheck true so a screen that opens before preload finishes can still
        // fall back to a live load instead of permanently hiding the slot.

        AdLoader adLoader = new AdLoader.Builder(context, adsId)
                .forNativeAd(nativeAd -> {
                    nativeAd.setOnPaidEventListener(adValue -> ADSUtilitis.logAdRevenue(context, adValue));
                    if (AdmobNativeAd != null) {
                        AdmobNativeAd.destroy();
                    }
                    AdmobNativeAd = nativeAd;
                    isNativePreloading = false;
                    LoadingCheck = true;
                    ADSUtilitis.trackScreen(context, "Native_PreLoad");
                    deliverPendingNativeIfNeeded();
                })
                .withNativeAdOptions(new NativeAdOptions.Builder()
                        .setVideoOptions(new VideoOptions.Builder().setStartMuted(true).build())
                        .setMediaAspectRatio(NativeAdOptions.NATIVE_MEDIA_ASPECT_RATIO_ANY)
                        .build())
                .withAdListener(new AdListener() {
                    @Override
                    public void onAdFailedToLoad(@NonNull LoadAdError adError) {
                        isNativePreloading = false;
                        LoadingCheck = true;
                        AdmobNativeAd = null;
                        ADSUtilitis.trackScreen(context, "Native_PreLoad_Fail");
                        Context pendingCtx = pendingNativeContext != null ? pendingNativeContext.get() : null;
                        FrameLayout pendingLayout = pendingNativeContainer != null ? pendingNativeContainer.get() : null;
                        ShimmerFrameLayout pendingShimmer = pendingNativeShimmer != null ? pendingNativeShimmer.get() : null;
                        String pendingId = pendingNativeAdsId;
                        String pendingSize = pendingNativeSize;
                        clearPendingNativeDisplay();
                        if (pendingCtx != null && pendingLayout != null && pendingId != null && !pendingId.isEmpty()) {
                            setDisplaySize(pendingSize);
                            AdmobFullNative(pendingCtx, pendingId, pendingShimmer, pendingLayout, false, true);
                        }
                    }
                })
                .build();
        adLoader.loadAd(new AdRequest.Builder().build());
    }

    public static boolean isNativePreloaded() {
        return AdmobNativeAd != null;
    }

    private static void registerPendingNativeDisplay(
            Context context,
            String adsId,
            FrameLayout linearLayout,
            ShimmerFrameLayout shimmerFrameLayout,
            String size
    ) {
        pendingNativeContext = new WeakReference<>(context);
        pendingNativeContainer = new WeakReference<>(linearLayout);
        pendingNativeShimmer = shimmerFrameLayout != null ? new WeakReference<>(shimmerFrameLayout) : null;
        pendingNativeAdsId = adsId;
        pendingNativeSize = normalizeNativeSize(size);
        setDisplaySize(pendingNativeSize);
        showNativeShimmer(shimmerFrameLayout, linearLayout);
    }

    private static void clearPendingNativeDisplay() {
        pendingNativeContext = null;
        pendingNativeContainer = null;
        pendingNativeShimmer = null;
        pendingNativeAdsId = null;
    }

    private static void deliverPendingNativeIfNeeded() {
        FrameLayout linearLayout = pendingNativeContainer != null ? pendingNativeContainer.get() : null;
        ShimmerFrameLayout shimmerFrameLayout = pendingNativeShimmer != null ? pendingNativeShimmer.get() : null;
        Context context = pendingNativeContext != null ? pendingNativeContext.get() : null;
        String size = pendingNativeSize;
        clearPendingNativeDisplay();
        if (context == null || linearLayout == null || AdmobNativeAd == null) {
            return;
        }
        setDisplaySize(size);
        bindNativeAdToLayout(context, linearLayout, shimmerFrameLayout);
    }

    /**
     * Inflate layout for current {@link #AdsDisplayType}:
     * small → {@code admob_small_native_ad}, medium → {@code admob_medium_native_ad}, big → {@code admob_big_native_ad}.
     */
    private static void bindNativeAdToLayout(
            Context context,
            FrameLayout linearLayout,
            ShimmerFrameLayout shimmerFrameLayout
    ) {
        if (context == null || linearLayout == null || AdmobNativeAd == null) {
            return;
        }
        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        String size = normalizeNativeSize(AdsDisplayType);
        AdsDisplayType = size;
        NativeAdView adView;

        switch (size) {
            case SIZE_BIG:
                adView = (NativeAdView) inflater.inflate(R.layout.admob_big_native_ad, null);
                PopulateUnifiedFullNativeAdView(AdmobNativeAd, adView, false);
                break;
            case SIZE_MEDIUM:
                adView = (NativeAdView) inflater.inflate(R.layout.admob_medium_native_ad, null);
                populateUnifiedNativeSmallAdView(AdmobNativeAd, adView);
                break;
            case SIZE_SMALL:
            default:
                adView = (NativeAdView) inflater.inflate(R.layout.admob_small_native_ad, null);
                populateUnifiedNativeSmallAdView(AdmobNativeAd, adView);
                break;
        }

        if (shimmerFrameLayout != null) {
            shimmerFrameLayout.stopShimmer();
            shimmerFrameLayout.setVisibility(View.GONE);
        }
        linearLayout.setVisibility(View.VISIBLE);
        linearLayout.removeAllViews();
        linearLayout.addView(adView);
        ADSAdpater.admob_nativehashmap.put(ADSAdpater.pos, adView);
    }

    /**
     * Load & show native ad.
     *
     * @param ads_type Firebase size only: {@code "small"} | {@code "medium"} | {@code "big"}
     *                 (anything else → small)
     */
    public static void loadAdmobNativeAdBig(String adsId, final FrameLayout linearLayout, ShimmerFrameLayout shimmerFrameLayout, String ads_type, final Context context) {
        setDisplaySize(ads_type);
        contexts = context;
        ArrayIndex = 0;

        if (ADSMainClass.getAds_Free()) {
            linearLayout.setVisibility(View.GONE);
            if (shimmerFrameLayout != null) {
                shimmerFrameLayout.stopShimmer();
                shimmerFrameLayout.setVisibility(View.GONE);
            }
            return;
        }
        if (adsId == null || adsId.isEmpty()) {
            linearLayout.setVisibility(View.GONE);
            if (shimmerFrameLayout != null) {
                shimmerFrameLayout.stopShimmer();
                shimmerFrameLayout.setVisibility(View.GONE);
            }
            return;
        }

        if (NativeByPage == ADSMainClass.getNativeByPage()) {
            NativeByPage = 0;
            showNativeBySize(context, adsId, linearLayout, shimmerFrameLayout, AdsDisplayType);
        } else {
            NativeByPage++;
            hideNativeShimmer(shimmerFrameLayout, linearLayout);
        }
    }

    /** Shared path for small / medium / big — layout chosen from {@code size}. */
    private static void showNativeBySize(
            Context context,
            String adsId,
            FrameLayout linearLayout,
            ShimmerFrameLayout shimmerFrameLayout,
            String size
    ) {
        setDisplaySize(size);
        showNativeShimmer(shimmerFrameLayout, linearLayout);

        if (ADSMainClass.getAdsTypeManage() != null
                && ADSMainClass.getAdsTypeManage().equals("Load")) {
            if (LoadingCheck) {
                LoadingCheck = false;
                AdmobFullNative(context, adsId, shimmerFrameLayout, linearLayout, false, true);
            } else {
                hideNativeShimmer(shimmerFrameLayout, linearLayout);
            }
            return;
        }

        // PreLoad mode
        if (AdmobNativeAd != null) {
            bindNativeAdToLayout(context, linearLayout, shimmerFrameLayout);
            // Refill cache only for small (legacy behavior); medium/big keep shown ad
            if (SIZE_SMALL.equals(AdsDisplayType)) {
                AdmobFullNative(context, adsId, shimmerFrameLayout, linearLayout, false, false);
            }
        } else if (isNativePreloading) {
            registerPendingNativeDisplay(context, adsId, linearLayout, shimmerFrameLayout, AdsDisplayType);
        } else if (LoadingCheck) {
            LoadingCheck = false;
            AdmobFullNative(context, adsId, shimmerFrameLayout, linearLayout, false, true);
        } else {
            AdmobFullNative(context, adsId, shimmerFrameLayout, linearLayout, false, true);
        }
    }

    public static void AdmobBigDisplay(Context context, String adsId, final FrameLayout linearLayout, ShimmerFrameLayout shimmerFrameLayout, Boolean banner_flag) {
        showNativeBySize(context, adsId, linearLayout, shimmerFrameLayout, SIZE_BIG);
    }

    public static void AdmobMediumDisplay(Context context, String adsId, final FrameLayout linearLayout, ShimmerFrameLayout shimmerFrameLayout) {
        showNativeBySize(context, adsId, linearLayout, shimmerFrameLayout, SIZE_MEDIUM);
    }

    public static void AdmobSmallDisplay(Context context, String adsId, final FrameLayout linearLayout, ShimmerFrameLayout shimmerFrameLayout, Boolean banner_flag) {
        showNativeBySize(context, adsId, linearLayout, shimmerFrameLayout, SIZE_SMALL);
    }

    public static void AdmobFullNative(final Context context, String adsId, ShimmerFrameLayout shimmerFrameLayout, final FrameLayout linearLayout, boolean banner_flag) {
        AdmobFullNative(context, adsId, shimmerFrameLayout, linearLayout, banner_flag,
                ADSMainClass.getAdsTypeManage() != null && ADSMainClass.getAdsTypeManage().equals("Load"));
    }

    public static void AdmobFullNative(final Context context, String adsId, ShimmerFrameLayout shimmerFrameLayout, final FrameLayout linearLayout, boolean banner_flag, boolean showWhenLoaded) {

//        if (!FailArrayId) {
//            LoadingCheck = true;
//            if (ADSMainClass.getAdsTypeManage().equals("Load")) {
//                linearLayout.setVisibility(View.GONE);
//                shimmerFrameLayout.setVisibility(View.GONE);
//            }
//
//            return;
//        }
//        Log.d("SSSSSSSSSS", "adsId: " + adsId);

        AdLoader.Builder builder = new AdLoader.Builder(context, adsId)
                .forNativeAd(new NativeAd.OnNativeAdLoadedListener() {

                    @Override
                    public void onNativeAdLoaded(NativeAd nativeAd) {
//                        Log.d("SSSSSSSSSS", "onNativeAdLoaded: ");
                        nativeAd.setOnPaidEventListener(new OnPaidEventListener() {
                            @Override
                            public void onPaidEvent(AdValue adValue) {
                                ADSUtilitis.logAdRevenue(context, adValue);
                            }
                        });

                        LoadingCheck = true;
                        isNativePreloading = false;
                        if (AdmobNativeAd != null) {
                            AdmobNativeAd.destroy();
                            AdmobNativeAd = null;
                        }
                        AdmobNativeAd = nativeAd;

//                        if (AdsDisplayType.equals("big")) {
                        ADSUtilitis.trackScreen(context, "Native_Load");
//                        } else {
//                            ADSUtilitis.trackScreen(context, "Native_Small_Loaded");
//                        }

//                        if (ADSMainClass.getAdsOneByOneIDS()) {
//                            if (ADSMainClass.getAdmobFullNativeIDList() != null && ADSMainClass.getAdmobFullNativeIDList().size() != 0 && ADSMainClass.getAdmobFullNativeIDList().size() == ArrayIndex) {
//                                ArrayIndex = 0;
//                            }
//                        } else {
//                            ArrayIndex = 0;
//                        }

                        if (showWhenLoaded || (ADSMainClass.getAdsTypeManage() != null
                                && ADSMainClass.getAdsTypeManage().equals("Load"))) {
                            if (AdmobNativeAd != null && linearLayout != null) {
                                bindNativeAdToLayout(context, linearLayout, shimmerFrameLayout);
                            }
                        }
                    }

                });

        VideoOptions videoOptions = new VideoOptions.Builder()
                .setStartMuted(true)
                .build();
        NativeAdOptions adOptions = new NativeAdOptions.Builder()
                .setVideoOptions(videoOptions)
                .setMediaAspectRatio(NativeAdOptions.NATIVE_MEDIA_ASPECT_RATIO_ANY)
                .build();
        builder.withNativeAdOptions(adOptions);

        AdLoader adLoader = builder.withAdListener(new AdListener() {
                    @Override
                    public void onAdFailedToLoad(LoadAdError adError) {
//                        Log.d("SSSSSSSSSS", "onAdFailedToLoad: " + adError.getMessage());
                        ADSUtilitis.trackScreen(context, "Native_Fail");
                        LoadingCheck = true;
                        isNativePreloading = false;
                        boolean adAlreadyVisible = linearLayout != null
                                && linearLayout.getVisibility() == View.VISIBLE
                                && linearLayout.getChildCount() > 0;
                        if (adAlreadyVisible) {
                            hideNativeShimmer(shimmerFrameLayout, null);
                        } else {
                            AdmobNativeAd = null;
                            hideNativeShimmer(shimmerFrameLayout, linearLayout);
                        }
                    }

                    @Override
                    public void onAdClicked() {
                        ADSAppManage.FastStart = true;
//                        AdsDisplayCheck(false);
                    }
                })
                .build();
        adLoader.loadAd(new AdRequest.Builder().build());
    }

    public static void PopulateUnifiedFullNativeAdView(NativeAd nativeAd, NativeAdView adView, boolean flag) {

        com.google.android.gms.ads.nativead.MediaView mediaView = adView.findViewById(R.id.ad_media);

        adView.setMediaView(mediaView);
        adView.setHeadlineView(adView.findViewById(R.id.ad_headline));
        adView.setBodyView(adView.findViewById(R.id.ad_body));
        adView.setCallToActionView(adView.findViewById(R.id.ad_call_to_action));
        adView.setIconView(adView.findViewById(R.id.ad_app_icon));
//        adView.setPriceView(adView.findViewById(R.id.ad_price));
        adView.setStarRatingView(adView.findViewById(R.id.ad_stars));
        adView.setStoreView(adView.findViewById(R.id.ad_store));
        adView.setAdvertiserView(adView.findViewById(R.id.ad_advertiser));
        ((TextView) adView.getHeadlineView()).setText(nativeAd.getHeadline());

        if (nativeAd.getBody() == null) {
            adView.getBodyView().setVisibility(View.INVISIBLE);
        } else {
            adView.getBodyView().setVisibility(View.VISIBLE);
            ((TextView) adView.getBodyView()).setText(nativeAd.getBody());
        }

        if (nativeAd.getCallToAction() == null) {
            adView.getCallToActionView().setVisibility(View.INVISIBLE);
        } else {
            adView.getCallToActionView().setVisibility(View.VISIBLE);
            ((TextView) adView.getCallToActionView()).setText(nativeAd.getCallToAction());
            applyNativeCtaColors(adView.getCallToActionView());
        }

        if (nativeAd.getIcon() == null) {
            adView.getIconView().setVisibility(View.GONE);
        } else {
            ((ImageView) adView.getIconView()).setImageDrawable(
                    nativeAd.getIcon().getDrawable());
            adView.getIconView().setVisibility(View.VISIBLE);
        }

//        if (nativeAd.getPrice() == null) {
//            adView.getPriceView().setVisibility(View.GONE);
//        } else {
//            adView.getPriceView().setVisibility(View.VISIBLE);
//            ((TextView) adView.getPriceView()).setText(nativeAd.getPrice());
//        }

        if (nativeAd.getStore() == null) {
            adView.getStoreView().setVisibility(View.INVISIBLE);
        } else {
            adView.getStoreView().setVisibility(View.VISIBLE);
            ((TextView) adView.getStoreView()).setText(nativeAd.getStore());
        }

        if (nativeAd.getStarRating() == null) {
            if (flag) {
                adView.getStarRatingView().setVisibility(View.GONE);
            } else {
                adView.getStarRatingView().setVisibility(View.GONE);
            }
        } else {
            ((RatingBar) adView.getStarRatingView())
                    .setRating(nativeAd.getStarRating().floatValue());
            if (flag) {
                adView.getStarRatingView().setVisibility(View.GONE);
            } else {
                adView.getStarRatingView().setVisibility(View.VISIBLE);
            }
        }


        if (nativeAd.getAdvertiser() == null) {
            if (flag) {
                adView.getAdvertiserView().setVisibility(View.GONE);
            } else {
                adView.getAdvertiserView().setVisibility(View.GONE);
            }
        } else {
            ((TextView) adView.getAdvertiserView()).setText(nativeAd.getAdvertiser());
            if (flag) {
                adView.getAdvertiserView().setVisibility(View.GONE);
            } else {
                adView.getAdvertiserView().setVisibility(View.VISIBLE);
            }
        }

        adView.setNativeAd(nativeAd);

        applyNativeThemeColors(adView);

        if (mediaView != null) {
            MediaContent vc = nativeAd.getMediaContent();
            if (vc != null) {
                mediaView.setVisibility(View.VISIBLE);
                mediaView.setImageScaleType(ImageView.ScaleType.CENTER_CROP);
                if (vc.hasVideoContent() && vc.getVideoController() != null) {
                    vc.getVideoController().setVideoLifecycleCallbacks(
                            new VideoController.VideoLifecycleCallbacks() {
                                @Override
                                public void onVideoEnd() {
                                    super.onVideoEnd();
                                }
                            });
                }
            } else {
                // No image/video asset on this creative
                mediaView.setVisibility(View.GONE);
            }
        }
    }

//    public static void displayAlreadyLoadedAdSmall(NativeAd ad, FrameLayout fl_adplaceholder, NativeAdLayout flNativeFBSmall, Activity activity, ShimmerFrameLayout shimmerFrameLayout) {
//        if (shimmerFrameLayout != null) {
//            shimmerFrameLayout.setVisibility(View.GONE);
//            shimmerFrameLayout.stopShimmer();
//        }
//        if (ad != null) {
//            try {
//                // Optimization: If the same ad is already displayed, skip re-inflation to prevent blinking
//                if (fl_adplaceholder.getChildCount() > 0) {
//                    View firstChild = fl_adplaceholder.getChildAt(0);
//                    if (firstChild instanceof NativeAdView && firstChild.getTag() == ad) {
//                        fl_adplaceholder.setVisibility(View.VISIBLE);
//                        return;
//                    }
//                }
//
//                NativeAdView adView = (NativeAdView) LayoutInflater.from(activity).inflate(R.layout.ad_unifiled_small, null);
//                adView.setTag(ad); // Tag it to identify during refreshes
//                populateUnifiedNativeSmallAdView(ad, adView);
//                fl_adplaceholder.setVisibility(View.VISIBLE);
//                fl_adplaceholder.removeAllViews();
//                fl_adplaceholder.addView(adView);
//            } catch (Exception e) {
//                e.printStackTrace();
//            }
//        }
//    }

//    public static void showNativeAdsSmallMainWithListener(String adunit, FrameLayout fl_adplaceholder, Activity activity, ShimmerFrameLayout shimmerFrameLayout, AdLoadListener listener) {
////        if (!ADSConsentManager.canRequestAds(activity)) {
////            if (shimmerFrameLayout != null) shimmerFrameLayout.setVisibility(View.GONE);
////            if (fl_adplaceholder != null) fl_adplaceholder.setVisibility(View.GONE);
////            if (listener != null) listener.onAdFailed();
////            return;
////        }
//        if (shimmerFrameLayout != null) {
//            shimmerFrameLayout.setVisibility(View.VISIBLE);
//            if (!shimmerFrameLayout.isShimmerStarted()) {
//                shimmerFrameLayout.startShimmer();
//            }
//        }
//        if (!ADSUtilitis.IsNetworkConnected(activity)) {
//            if (shimmerFrameLayout != null && shimmerFrameLayout.isShimmerStarted()) {
//                shimmerFrameLayout.stopShimmer();
//            }
//            if (shimmerFrameLayout != null) shimmerFrameLayout.setVisibility(View.GONE);
//            if (listener != null) listener.onAdFailed();
//            return;
//        }
//        /*if (AdmobNativeAd != null) {
//            AdmobNativeAd.destroy();
//            AdmobNativeAd = null;
//        }*/
//        loadAdmobNativeAdSmallWithListener(adunit, fl_adplaceholder, shimmerFrameLayout, activity, listener);
//    }

    public interface AdLoadListener {
        void onAdLoaded(NativeAd nativeAd);

        void onAdFailed();
    }

//    @SuppressLint("MissingPermission")
//    public static void loadAdmobNativeAdSmallWithListener(String adId, FrameLayout fl_adplaceholder, ShimmerFrameLayout shimmerFrameLayout, Activity activity, AdLoadListener listener) {
//        String adUnitId;
//        adUnitId = adId;
//        if (TextUtils.isEmpty(adUnitId)) {
//            shimmerFrameLayout.setVisibility(View.GONE);
//            if (listener != null) listener.onAdFailed();
//            return;
//        }
//
//        AdLoader.Builder builder = new AdLoader.Builder(activity, adUnitId);
//
//        builder.forNativeAd(nativeAd -> {
//            AdmobNativeAd = nativeAd;
//            showNativeOptionAdSmall(nativeAd, fl_adplaceholder, activity);
//            if (shimmerFrameLayout.isShimmerStarted()) {
//                shimmerFrameLayout.stopShimmer();
//            }
//            shimmerFrameLayout.setVisibility(View.GONE);
//            ADSUtilitis.trackScreen(activity, "Native_Load");
//            if (listener != null) listener.onAdLoaded(nativeAd);
//        }).withAdListener(new AdListener() {
//
//            @Override
//            public void onAdLoaded() {
//                super.onAdLoaded();
//            }
//
//            @Override
//            public void onAdFailedToLoad(@NonNull LoadAdError loadAdError) {
//                super.onAdFailedToLoad(loadAdError);
//            }
//        });
//
//        VideoOptions videoOptions = new VideoOptions.Builder().setStartMuted(true).build();
//
//        NativeAdOptions adOptions = new NativeAdOptions.Builder().setVideoOptions(videoOptions).build();
//
//        builder.withNativeAdOptions(adOptions);
//        AdLoader adLoader = builder.withAdListener(new AdListener() {
//
//            @Override
//            public void onAdLoaded() {
//                super.onAdLoaded();
////                Log.d("TAGNative", "onAdLoaded: ");
//                shimmerFrameLayout.setVisibility(View.GONE);
//                shimmerFrameLayout.stopShimmer();
//                AdmobNativeAd.setOnPaidEventListener(new OnPaidEventListener() {
//                    @Override
//                    public void onPaidEvent(AdValue adValue) {
//                        ADSUtilitis.logAdRevenue(activity, adValue);
//                    }
//                });
//            }
//
//            @Override
//            public void onAdFailedToLoad(@NonNull LoadAdError adError) {
////                Log.d("TAGNative", "onAdLoaded: " +adError);
////                loadAdxNativeAdSmall(fl_adplaceholder, flNativeFBSmall, shimmerFrameLayout, activity);
//                ADSUtilitis.trackScreen(activity, "Native_Fail");
//                shimmerFrameLayout.setVisibility(View.GONE);
//                shimmerFrameLayout.stopShimmer();
////                Log.d("loadAdmobNativeAdSmall", "onAdFailedToLoad: 2222222222  " + adError);
//
//            }
//        }).build();
//
//        adLoader.loadAd(new AdRequest.Builder().build());
//    }

//    public static void showNativeOptionAdSmall(NativeAd nativeAd, FrameLayout fl_adplaceholder, Activity activity) {
//        try {
//            if (nativeAd != null) {
//                // Optimization: If already showing the same ad, don't re-inflate
//                if (fl_adplaceholder.getChildCount() > 0) {
//                    View firstChild = fl_adplaceholder.getChildAt(0);
//                    if (firstChild instanceof NativeAdView && firstChild.getTag() == nativeAd) {
//                        fl_adplaceholder.setVisibility(View.VISIBLE);
//                        return;
//                    }
//                }
//
//                NativeAdView adView = (NativeAdView) LayoutInflater.from(activity).inflate(R.layout.ad_unifiled_small, null);
//                adView.setTag(nativeAd);
//                populateUnifiedNativeSmallAdView(nativeAd, adView);
//                fl_adplaceholder.setVisibility(View.VISIBLE);
//                fl_adplaceholder.removeAllViews();
//                fl_adplaceholder.addView(adView);
//
//            } else {
//                fl_adplaceholder.setVisibility(View.GONE);
////                flNativeFBSmall.setVisibility(View.GONE);
//            }
//        } catch (Exception e) {
//            e.printStackTrace();
//            fl_adplaceholder.setVisibility(View.GONE);
////            flNativeFBSmall.setVisibility(View.GONE);
//
//        }
//    }

    public static void populateUnifiedNativeSmallAdView(NativeAd nativeAd, NativeAdView adView) {
        adView.setHeadlineView(adView.findViewById(R.id.ad_headline));
        adView.setBodyView(adView.findViewById(R.id.ad_body));
        adView.setCallToActionView(adView.findViewById(R.id.ad_call_to_action));
        adView.setIconView(adView.findViewById(R.id.ad_app_icon));
//        adView.setPriceView(adView.findViewById(R.id.ad_price));
        adView.setStarRatingView(adView.findViewById(R.id.ad_stars));
        adView.setStoreView(adView.findViewById(R.id.ad_store));
        adView.setAdvertiserView(adView.findViewById(R.id.ad_advertiser));

        try {
            ((TextView) adView.getHeadlineView()).setText(nativeAd.getHeadline());
        } catch (Exception e) {
            e.printStackTrace();
        }

        if (nativeAd.getBody() == null) {
            Objects.requireNonNull(adView.getBodyView()).setVisibility(View.GONE);
        } else {
            Objects.requireNonNull(adView.getBodyView()).setVisibility(View.VISIBLE);
            ((TextView) adView.getBodyView()).setText(nativeAd.getBody());
        }

        if (nativeAd.getCallToAction() == null) {
            Objects.requireNonNull(adView.getCallToActionView()).setVisibility(View.GONE);
        } else {
            Objects.requireNonNull(adView.getCallToActionView()).setVisibility(View.VISIBLE);
            ((TextView) adView.getCallToActionView()).setText(nativeAd.getCallToAction());
            applyNativeCtaColors(adView.getCallToActionView());
        }
        if (nativeAd.getIcon() == null) {
            Objects.requireNonNull(adView.getIconView()).setVisibility(View.GONE);
        } else {
            ((ImageView) Objects.requireNonNull(adView.getIconView())).setImageDrawable(nativeAd.getIcon().getDrawable());
            adView.getIconView().setVisibility(View.VISIBLE);
        }

//        if (nativeAd.getPrice() == null) {
//            Objects.requireNonNull(adView.getPriceView()).setVisibility(View.GONE);
//        } else {
//            Objects.requireNonNull(adView.getPriceView()).setVisibility(View.VISIBLE);
//            ((TextView) adView.getPriceView()).setText(nativeAd.getPrice());
//        }


        if (nativeAd.getStore() == null) {
            Objects.requireNonNull(adView.getStoreView()).setVisibility(View.GONE);
        } else {
            Objects.requireNonNull(adView.getStoreView()).setVisibility(View.VISIBLE);
            ((TextView) adView.getStoreView()).setText(nativeAd.getStore());
        }

        if (nativeAd.getStarRating() == null) {
            Objects.requireNonNull(adView.getStarRatingView()).setVisibility(View.GONE);
        } else {
            ((RatingBar) Objects.requireNonNull(adView.getStarRatingView())).setRating(nativeAd.getStarRating().floatValue());
            adView.getStarRatingView().setVisibility(View.VISIBLE);
        }

        if (nativeAd.getAdvertiser() == null) {
            Objects.requireNonNull(adView.getAdvertiserView()).setVisibility(View.GONE);
        } else {
            ((TextView) Objects.requireNonNull(adView.getAdvertiserView())).setText(nativeAd.getAdvertiser());
            adView.getAdvertiserView().setVisibility(View.VISIBLE);
        }
        adView.getStoreView().setVisibility(View.GONE);
//        adView.getPriceView().setVisibility(View.GONE);
        adView.setNativeAd(nativeAd);
        applyNativeThemeColors(adView);
    }

    /**
     * Applies Firebase {@code ads.ads_button_color} / {@code ads.ads_button_text_color}
     * to the native CTA button when values are present and valid hex colors.
     */
    private static void applyNativeCtaColors(View ctaView) {
        if (!(ctaView instanceof TextView)) return;
        TextView cta = (TextView) ctaView;
        try {
            String bg = ADSMainClass.getNativeButtonColor();
            if (!TextUtils.isEmpty(bg)) {
                cta.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor(bg.trim())));
            }
            String text = ADSMainClass.getNativeButtonTextColor();
            if (!TextUtils.isEmpty(text)) {
                cta.setTextColor(Color.parseColor(text.trim()));
            }
        } catch (Exception ignored) {
            // Invalid color from Remote Config — keep layout defaults.
        }
    }

    /**
     * Firebase theme for small / medium / big native cards:
     * <ul>
     *   <li>{@code ads_layout_background_color} / {@code ads_layout_text_color} → {@code adsLayout}</li>
     *   <li>{@code ads_background_color} → {@code llMainLayout}</li>
     *   <li>{@code ads_all_text_color} → headline, body, advertiser, price, store</li>
     * </ul>
     * CTA keeps {@link #applyNativeCtaColors}.
     */
    private static void applyNativeThemeColors(NativeAdView adView) {
        if (adView == null) return;
        try {
            View main = adView.findViewById(R.id.llMainLayout);
            String cardBg = ADSMainClass.getAdsBackgroundColor();
            if (main != null && !TextUtils.isEmpty(cardBg)) {
                main.setBackgroundColor(Color.parseColor(cardBg.trim()));
            }

            View badge = adView.findViewById(R.id.adsLayout);
            if (badge instanceof TextView) {
                TextView adBadge = (TextView) badge;
                String badgeBg = ADSMainClass.getAdsLayoutBackgroundColor();
                if (!TextUtils.isEmpty(badgeBg)) {
                    adBadge.setBackgroundTintList(
                            ColorStateList.valueOf(Color.parseColor(badgeBg.trim()))
                    );
                }
                String badgeText = ADSMainClass.getAdsLayoutTextColor();
                if (!TextUtils.isEmpty(badgeText)) {
                    adBadge.setTextColor(Color.parseColor(badgeText.trim()));
                }
            }

            String allText = ADSMainClass.getAdsAllTextColor();
            if (!TextUtils.isEmpty(allText)) {
                int color = Color.parseColor(allText.trim());
                applyTextColorIfPresent(adView.findViewById(R.id.ad_headline), color);
                applyTextColorIfPresent(adView.findViewById(R.id.ad_body), color);
                applyTextColorIfPresent(adView.findViewById(R.id.ad_advertiser), color);
                applyTextColorIfPresent(adView.findViewById(R.id.ad_price), color);
                applyTextColorIfPresent(adView.findViewById(R.id.ad_store), color);
            }
        } catch (Exception ignored) {
            // Invalid color from Remote Config — keep layout defaults.
        }
    }

    private static void applyTextColorIfPresent(View view, int color) {
        if (view instanceof TextView) {
            ((TextView) view).setTextColor(color);
        }
    }
}

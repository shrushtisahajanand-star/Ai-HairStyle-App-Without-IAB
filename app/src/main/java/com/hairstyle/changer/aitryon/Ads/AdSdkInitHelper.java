package com.hairstyle.changer.aitryon.Ads;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.os.Build;
import android.webkit.WebView;

public final class AdSdkInitHelper {

    private static final String TAG = "AdSdkInitHelper";
    private static Boolean webViewAvailable;

    private AdSdkInitHelper() {
    }

    public static boolean isWebViewAvailable(Context context) {
        if (webViewAvailable != null) {
            return webViewAvailable;
        }
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                PackageInfo webViewPackage = WebView.getCurrentWebViewPackage();
                webViewAvailable = webViewPackage != null;
            } else {
                new WebView(context.getApplicationContext()).destroy();
                webViewAvailable = true;
            }
        } catch (Throwable t) {
            // Log.w(TAG, "WebView not available; ad SDK init will be skipped", t);
            webViewAvailable = false;
        }
        return webViewAvailable;
    }

    public static void runIfWebViewAvailable(Context context, Runnable initAction) {
        if (!isWebViewAvailable(context)) {
            return;
        }
        try {
            initAction.run();
        } catch (Throwable t) {
            // Log.w(TAG, "Ad SDK init failed", t);
        }
    }

    /** Runs [initAction] when WebView is available; otherwise runs [fallback]. */
    public static void runIfWebViewAvailable(Context context, Runnable initAction, Runnable fallback) {
        if (!isWebViewAvailable(context)) {
            if (fallback != null) fallback.run();
            return;
        }
        try {
            initAction.run();
        } catch (Throwable t) {
            // Log.w(TAG, "Ad SDK init failed", t);
            if (fallback != null) fallback.run();
        }
    }
}

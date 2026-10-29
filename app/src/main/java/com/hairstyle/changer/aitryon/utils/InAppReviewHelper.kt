package com.hairstyle.changer.aitryon.utils

import android.app.Activity
import android.util.Log
import com.google.android.play.core.review.ReviewManagerFactory

/**
 * Google Play In-App Review.
 *
 * Play does **not** provide an API to check if the user already reviewed.
 * Failures fall back to the Play Store listing â€” never crash the app.
 */
object InAppReviewHelper {

    private const val TAG = "InAppReviewHelper"

    fun launch(activity: Activity, onDone: () -> Unit = {}) {
        if (activity.isFinishing || activity.isDestroyed) {
            onDone()
            return
        }

        try {
            val manager = ReviewManagerFactory.create(activity)
            manager.requestReviewFlow()
                .addOnCompleteListener { request ->
                    try {
                        if (activity.isFinishing || activity.isDestroyed) {
                            onDone()
                            return@addOnCompleteListener
                        }
                        if (!request.isSuccessful) {
                            // Log.w(TAG, "requestReviewFlow failed", request.exception)
                            AppLinkHelper.openPlayStore(activity)
                            onDone()
                            return@addOnCompleteListener
                        }
                        manager.launchReviewFlow(activity, request.result)
                            .addOnCompleteListener {
                                onDone()
                            }
                            .addOnFailureListener { e ->
                                // Log.w(TAG, "launchReviewFlow failed", e)
                                if (!activity.isFinishing && !activity.isDestroyed) {
                                    AppLinkHelper.openPlayStore(activity)
                                }
                                onDone()
                            }
                    } catch (e: Exception) {
                        // Log.e(TAG, "Review callback error", e)
                        if (!activity.isFinishing && !activity.isDestroyed) {
                            AppLinkHelper.openPlayStore(activity)
                        }
                        onDone()
                    }
                }
                .addOnFailureListener { e ->
                    // Log.w(TAG, "requestReviewFlow listener failed", e)
                    if (!activity.isFinishing && !activity.isDestroyed) {
                        AppLinkHelper.openPlayStore(activity)
                    }
                    onDone()
                }
        } catch (e: Exception) {
            // Log.e(TAG, "In-app review unavailable", e)
            if (!activity.isFinishing && !activity.isDestroyed) {
                AppLinkHelper.openPlayStore(activity)
            }
            onDone()
        }
    }
}

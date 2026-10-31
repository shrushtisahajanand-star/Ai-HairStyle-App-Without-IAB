package com.hairstyle.changer.aitryon.utils

import android.content.Context
import android.os.RemoteException
import android.util.Log
import com.android.installreferrer.api.InstallReferrerClient
import com.android.installreferrer.api.InstallReferrerStateListener
import com.hairstyle.changer.aitryon.Ads.ADSMainClass
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.coroutines.resume

/**
 * Play Install Referrer → paid vs organic, then Splash picks Firebase
 * [ADSMainClass.PAID_USER] or [ADSMainClass.NORMAL_USER] JSON.
 */
object InstallReferrerHelper {

    private const val REFERRER_WAIT_TIMEOUT_MS = 2500L

    suspend fun awaitFetch(context: Context) {
        if (ADSMainClass.isInstallReferrerFetched()) return
        withTimeoutOrNull(REFERRER_WAIT_TIMEOUT_MS) {
            withContext(Dispatchers.IO) {
                fetchOnce(context.applicationContext)
            }
        }
    }

    private suspend fun fetchOnce(context: Context) = suspendCancellableCoroutine { cont ->
        val finished = AtomicBoolean(false)
        val finish: () -> Unit = {
            if (finished.compareAndSet(false, true)) {
                ADSMainClass.setInstallReferrerFetched(true)
                if (cont.isActive) cont.resume(Unit)
            }
        }

        val referrerClient = InstallReferrerClient.newBuilder(context).build()
        try {
            referrerClient.startConnection(object : InstallReferrerStateListener {
                override fun onInstallReferrerSetupFinished(responseCode: Int) {
                    try {
                        if (responseCode == InstallReferrerClient.InstallReferrerResponse.OK) {
                            try {
                                val referrerUrl = referrerClient.installReferrer.installReferrer.orEmpty()
//                                val referrerUrl = "utm_source=marketing"
//                                 val referrerUrl = "gclid=CjwKCAiAxaCvBhBaEiwAvsLmWPrs37AYx_Ol2GN6T1nt1NHn2TJl7xf65ryNQtoWL-24BefvfIYBGRoCx-gQAvD_BwE"
                                ADSMainClass.setReferrerUrl(referrerUrl)
//                                Log.d("TAG", "installReferrer=$referrerUrl paid=${isPaidMarketingReferrer(referrerUrl)}")
                            } catch (_: RemoteException) {
                            }
                        }
                    } finally {
                        runCatching { referrerClient.endConnection() }
                        finish()
                    }
                }

                override fun onInstallReferrerServiceDisconnected() {
                    finish()
                }
            })
        } catch (_: Exception) {
            finish()
        }
        cont.invokeOnCancellation {
            runCatching { referrerClient.endConnection() }
        }
    }

    fun isPaidMarketingReferrer(referrerUrl: String = ADSMainClass.getReferrerUrl().orEmpty()): Boolean {
        if (referrerUrl.isBlank()) return false
//        Log.e("sjdshdjsjj", "isPaidMarketingReferrer: "+referrerUrl )
        val marketingUrl = firstQueryParam(referrerUrl)
        val facebookUser = facebookMarketingConverter(referrerUrl)
        val newString = stringMarketingConverter(referrerUrl)

        val hasGclid = referrerUrl.contains("gclid=", ignoreCase = true) ||
            newString.equals("gclid", ignoreCase = true)

        return hasGclid ||
            facebookUser.equals("utm_source=apps.facebook.com", ignoreCase = true) ||
            facebookUser.equals("utm_source=apps.instagram.com", ignoreCase = true) ||
            marketingUrl.equals("utm_source=marketing", ignoreCase = true) ||
            referrerUrl.contains("utm_source=marketing", ignoreCase = true)
    }

    private fun stringMarketingConverter(str: String): String {
        if (str.isBlank()) return ""
        return str.split("=", limit = 2).firstOrNull().orEmpty()
    }

    private fun firstQueryParam(str: String): String {
        if (str.isBlank()) return ""
        return str.substringBefore('&')
    }

    private fun facebookMarketingConverter(input: String): String {
        if (input.isBlank()) return ""
        val index = input.indexOf('&')
        return if (index != -1) input.substring(0, index) else input
    }
}

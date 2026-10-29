package com.hairstyle.changer.aitryon.di.fcm

import android.util.Log
import com.google.firebase.messaging.FirebaseMessaging
import com.hairstyle.changer.aitryon.utils.PrefManager

object FcmTokenHelper {

    private const val TAG = "FcmTokenHelper"
    private const val KEY_FCM_TOKEN = "fcm_registration_token"

    fun init() {
        FirebaseMessaging.getInstance().isAutoInitEnabled = true
        fetchAndStoreToken()
    }

    fun fetchAndStoreToken() {
        FirebaseMessaging.getInstance().token
            .addOnSuccessListener { token ->
                if (token.isNullOrBlank()) return@addOnSuccessListener
                PrefManager.putString(KEY_FCM_TOKEN, token)
//                Log.d(TAG, "FCM token: $token")
            }
            .addOnFailureListener { e ->
//                Log.w(TAG, "Failed to fetch FCM token", e)
            }
    }

    fun getStoredToken(): String = PrefManager.getString(KEY_FCM_TOKEN, "")
}

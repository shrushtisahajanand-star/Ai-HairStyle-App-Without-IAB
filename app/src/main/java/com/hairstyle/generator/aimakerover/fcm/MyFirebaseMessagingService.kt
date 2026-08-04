package com.hairstyle.generator.aimakerover.fcm

import android.util.Log
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.hairstyle.generator.aimakerover.utils.PrefManager

class MyFirebaseMessagingService : FirebaseMessagingService() {

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        PrefManager.putString(KEY_FCM_TOKEN, token)
        // Log.d(TAG, "onNewToken: $token")
    }

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)

        val title = message.notification?.title
            ?: message.data["title"]
            ?: message.data["Title"]
        val body = message.notification?.body
            ?: message.data["body"]
            ?: message.data["message"]
            ?: message.data["Message"]

        if (title.isNullOrBlank() && body.isNullOrBlank()) {
//            Log.d(TAG, "Empty FCM payload â€” skip notification")
            return
        }

        NotificationHelper.showNotification(
            context = applicationContext,
            title = title,
            body = body,
            data = message.data
        )
    }

    companion object {
        private const val TAG = "MyFCMService"
        private const val KEY_FCM_TOKEN = "fcm_registration_token"
    }
}

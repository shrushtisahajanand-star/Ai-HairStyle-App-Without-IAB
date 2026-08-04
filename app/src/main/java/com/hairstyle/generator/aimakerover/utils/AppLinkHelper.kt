package com.hairstyle.generator.aimakerover.utils

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import com.hairstyle.generator.aimakeover.R
import com.hairstyle.generator.aimakerover.Advertisement.ADSAppManage

object AppLinkHelper {

    fun shareApp(context: Context) {
        ADSAppManage.skipNextAppOpenAd()
        val appName = context.getString(R.string.app_name)
        val storeUrl = playStoreHttpsUrl(context)
        val message = context.getString(R.string.share_app_message, appName, storeUrl)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, appName)
            putExtra(Intent.EXTRA_TEXT, message)
        }
        context.startActivity(
            Intent.createChooser(intent, context.getString(R.string.share_app))
        )
    }

    fun openPlayStore(context: Context) {
        ADSAppManage.skipNextAppOpenAd()
        val packageName = context.packageName
        try {
            context.startActivity(
                Intent(
                    Intent.ACTION_VIEW,
                    Uri.parse("market://details?id=$packageName")
                ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
        } catch (_: ActivityNotFoundException) {
            openUrl(context, playStoreHttpsUrl(context), skipAppOpen = false)
        }
    }

    fun openPrivacyPolicy(context: Context) {
        val url = resolvePrivacyPolicyUrl(context)
        if (url.isBlank()) return
        openUrl(context, url)
    }

    fun resolvePrivacyPolicyUrl(context: Context): String {
        val stored = PrefManager.getPrivacyPolicyUrl().trim()
        if (stored.isNotBlank()) {
            if (stored.startsWith("http", ignoreCase = true)) return stored
            val decrypted = runCatching { AESHelper.decrypt(stored).trim() }.getOrDefault("")
            if (decrypted.isNotBlank()) return decrypted
        }
        return context.getString(R.string.privacy_policy_url).trim()
    }

    fun openUrl(context: Context, url: String, skipAppOpen: Boolean = true) {
        if (url.isBlank()) return
        if (skipAppOpen) {
            ADSAppManage.skipNextAppOpenAd()
        }
        runCatching {
            context.startActivity(
                Intent(Intent.ACTION_VIEW, Uri.parse(url))
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
        }
    }

    private fun playStoreHttpsUrl(context: Context): String {
        return "https://play.google.com/store/apps/details?id=${context.packageName}"
    }
}

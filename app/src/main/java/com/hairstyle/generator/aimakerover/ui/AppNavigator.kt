package com.hairstyle.generator.aimakerover.ui

import android.app.Activity
import android.content.Intent
import com.hairstyle.generator.aimakerover.ui.category.SelectCategoryActivity
import com.hairstyle.generator.aimakerover.ui.creation.UserCreationActivity
import com.hairstyle.generator.aimakerover.ui.creation.UserCreationPreviewActivity
import com.hairstyle.generator.aimakerover.ui.feedback.FeedbackActivity
import com.hairstyle.generator.aimakerover.ui.gallery.GalleryPhotoSelectionActivity
import com.hairstyle.generator.aimakerover.ui.gallery.PhotoSelectActivity
import com.hairstyle.generator.aimakerover.ui.home.HomePageActivity
import com.hairstyle.generator.aimakerover.ui.intro.Intro1Activity
import com.hairstyle.generator.aimakerover.ui.intro.Intro1bActivity
import com.hairstyle.generator.aimakerover.ui.intro.Intro1cActivity
import com.hairstyle.generator.aimakerover.ui.intro.Intro1dActivity
import com.hairstyle.generator.aimakerover.ui.language.LanguageActivity
import com.hairstyle.generator.aimakerover.ui.premium.PremiumActivity
import com.hairstyle.generator.aimakerover.ui.progress.ProgressActivity
import com.hairstyle.generator.aimakerover.ui.result.ResultActivity
import com.hairstyle.generator.aimakerover.ui.setting.ActiveSubscriptionActivity
import com.hairstyle.generator.aimakerover.ui.setting.SettingActivity
import com.hairstyle.generator.aimakerover.ui.viewmodel.GenerationViewModelOwner

object AppNavigator {

    fun openLanguage(from: Activity, fromSettings: Boolean = false, finishFrom: Boolean = false) {
        from.startActivity(LanguageActivity.createIntent(from, fromSettings))
        if (finishFrom) from.finish()
    }

    fun openIntro1(from: Activity, finishFrom: Boolean = false) {
        from.startActivity(Intent(from, Intro1Activity::class.java))
        if (finishFrom) from.finish()
    }

    fun openIntro1b(from: Activity) {
        from.startActivity(Intent(from, Intro1bActivity::class.java))
    }

    fun openIntro1c(from: Activity) {
        from.startActivity(Intent(from, Intro1cActivity::class.java))
    }

    fun openIntro1d(from: Activity) {
        from.startActivity(Intent(from, Intro1dActivity::class.java))
    }

    fun openPremium(from: Activity, fromIntro: Boolean = false) {
        from.startActivity(PremiumActivity.createIntent(from, fromIntro))
    }

    fun openHomeClearTask(from: Activity) {
        GenerationViewModelOwner.clear()
        from.startActivity(
            Intent(from, HomePageActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            }
        )
    }

    fun openSettings(from: Activity) {
        from.startActivity(Intent(from, SettingActivity::class.java))
    }

    fun openFeedback(from: Activity) {
        from.startActivity(Intent(from, FeedbackActivity::class.java))
    }

    fun openActiveSubscription(from: Activity) {
        from.startActivity(Intent(from, ActiveSubscriptionActivity::class.java))
    }

    fun openUserCreation(from: Activity) {
        from.startActivity(Intent(from, UserCreationActivity::class.java))
    }

    fun openUserCreationPreview(from: Activity, creationId: String) {
        from.startActivity(UserCreationPreviewActivity.createIntent(from, creationId))
    }

    fun openSelectCategory(
        from: Activity,
        tab: Int = 0,
        photoAlreadySelected: Boolean = false
    ) {
        from.startActivity(SelectCategoryActivity.createIntent(from, tab, photoAlreadySelected))
    }

    fun openPhotoSelect(from: Activity, photoFirstFlow: Boolean = false) {
        from.startActivity(PhotoSelectActivity.createIntent(from, photoFirstFlow))
    }

    fun openGalleryPhotoSelection(from: Activity, photoFirstFlow: Boolean = false) {
        from.startActivity(GalleryPhotoSelectionActivity.createIntent(from, photoFirstFlow))
    }

    fun openProgress(from: Activity) {
        from.startActivity(Intent(from, ProgressActivity::class.java))
    }

    fun openResult(from: Activity) {
        from.startActivity(Intent(from, ResultActivity::class.java))
        from.finish()
    }
}

package com.hairstyle.changer.aitryon.Ads

/**
 * Per-action interstitial flags from Firebase screen JSON.
 * Defaults keep previous behavior where those clicks already showed inter.
 * Intro Continue / option clicks default false (previously no forward inter).
 */
object ScreenInterAds {

    fun intro1(): Boolean = ADSMainClass.getBooleanValue(ADSMainClass.INTRO1_SHOW_INTER, false)
    fun intro1b(): Boolean = ADSMainClass.getBooleanValue(ADSMainClass.INTRO1B_SHOW_INTER, false)
    fun intro1c(): Boolean = ADSMainClass.getBooleanValue(ADSMainClass.INTRO1C_SHOW_INTER, false)
    fun intro1d(): Boolean = ADSMainClass.getBooleanValue(ADSMainClass.INTRO1D_SHOW_INTER, false)
    fun intro2(): Boolean = ADSMainClass.getBooleanValue(ADSMainClass.INTRO2_SHOW_INTER, false)
    fun intro3(): Boolean = ADSMainClass.getBooleanValue(ADSMainClass.INTRO3_SHOW_INTER, false)

    fun tryIt(): Boolean = ADSMainClass.getBooleanValue(ADSMainClass.TRYIT_SHOW_INTER, true)
    fun seeAll(): Boolean = ADSMainClass.getBooleanValue(ADSMainClass.SEEALL_SHOW_INTER, true)
    fun uploadPhoto(): Boolean =
        ADSMainClass.getBooleanValue(ADSMainClass.UPLOAD_PHOTO_BUTTON_SHOW_INTER, true)

    fun funBeardTryIt(): Boolean =
        ADSMainClass.getBooleanValue(ADSMainClass.FUN_BEARD_TRYIT_SHOW_INTER, true)

    fun settingIcon(): Boolean =
        ADSMainClass.getBooleanValue(ADSMainClass.SETTING_ICON_SHOW_INTER, true)

    fun selectImageNext(): Boolean =
        ADSMainClass.getBooleanValue(ADSMainClass.SELECT_IMAGE_NEXT_BUTTON_SHOW_INTER, true)

    fun progressClose(): Boolean =
        ADSMainClass.getBooleanValue(ADSMainClass.CLOSE_ICON_SHOW_INTER, true)

    fun leaveButton(): Boolean =
        ADSMainClass.getBooleanValue(ADSMainClass.LEAVE_BUTTON_SHOW_INTER, true)

    fun settingRows(): Boolean =
        ADSMainClass.getBooleanValue(ADSMainClass.SETTING_SHOW_INTER, true)
}

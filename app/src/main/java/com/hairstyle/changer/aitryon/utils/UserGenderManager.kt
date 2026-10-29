package com.hairstyle.changer.aitryon.utils

object UserGenderManager {

    private const val KEY_GENDER = "user_gender"

    const val GENDER_MALE = "male"
    const val GENDER_FEMALE = "female"

    fun setGenderFromIntroOptionId(optionId: Int) {
        val gender = if (optionId == 1) GENDER_MALE else GENDER_FEMALE
        PrefManager.putString(KEY_GENDER, gender)
    }

    fun isMale(): Boolean = getGender() == GENDER_MALE

    fun getGender(): String {
        return PrefManager.getString(KEY_GENDER, GENDER_FEMALE)
    }

    fun hasGender(): Boolean = PrefManager.getString(KEY_GENDER, "").isNotBlank()
}

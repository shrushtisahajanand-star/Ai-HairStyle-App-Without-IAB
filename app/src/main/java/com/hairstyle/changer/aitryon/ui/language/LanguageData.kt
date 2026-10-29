package com.hairstyle.changer.aitryon.ui.language

import androidx.annotation.DrawableRes
import com.hairstyle.changer.aitryon.R

data class LanguageItem(
    val code: String,
    val nativeName: String,
    val englishName: String,
    @DrawableRes val flagRes: Int
)

object LanguageData {

    val languages = listOf(
        LanguageItem("en", "English", "English", R.drawable.ic_flag1), // Default
        LanguageItem("en-GB", "English (UK)", "English (UK)", R.drawable.ic_flag2),
        LanguageItem("hi", "हिंदी", "Hindi", R.drawable.ic_flag3),
        LanguageItem("ru", "Русский", "Russian", R.drawable.ic_flag4),
        LanguageItem("it", "Italiano", "Italian", R.drawable.ic_flag5),
        LanguageItem("fr", "Français", "French", R.drawable.ic_flag6),
        LanguageItem("es", "Español", "Spanish", R.drawable.ic_flag7),
        LanguageItem("ja", "日本語", "Japanese", R.drawable.ic_flag8),
        LanguageItem("ko", "한국어", "Korean", R.drawable.ic_flag9),
        LanguageItem("de", "Deutsch", "German", R.drawable.ic_flag10),
        LanguageItem("zh-CN", "简体中文", "Chinese", R.drawable.ic_flag11),
        LanguageItem("th", "ไทย", "Thai", R.drawable.ic_flag12),
        LanguageItem("el", "Ελληνικά", "Greek", R.drawable.ic_flag13),
        LanguageItem("pt", "Português", "Portuguese (Portugal)", R.drawable.ic_flag14),
        LanguageItem("pt-BR", "Português (Brasil)", "Portuguese (Brazil)", R.drawable.ic_flag15),
        LanguageItem("nl", "Nederlands", "Dutch", R.drawable.ic_flag16),
        LanguageItem("fil", "Filipino", "Filipino", R.drawable.ic_flag17),
        LanguageItem("tr", "Türkçe", "Turkish", R.drawable.ic_flag18),
        LanguageItem("id", "Bahasa Indonesia", "Indonesian", R.drawable.ic_flag19),
        LanguageItem("af", "Afrikaans", "Afrikaans", R.drawable.ic_flag20)
    )
}   

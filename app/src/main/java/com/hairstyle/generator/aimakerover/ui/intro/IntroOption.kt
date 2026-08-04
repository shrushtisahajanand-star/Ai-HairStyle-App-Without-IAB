package com.hairstyle.generator.aimakerover.ui.intro

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes

data class IntroOption(
    val id: Int,
    @StringRes val titleRes: Int,
    @DrawableRes val imageRes: Int,
    @StringRes val subtitleRes: Int
)

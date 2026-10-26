package com.hairstyle.generator.aimakerover.ui.intro

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import com.hairstyle.generator.aimakeover.R
import com.hairstyle.generator.aimakerover.utils.VideoTitleKeys

object Intro1Data {

    const val TOTAL_FLOW_DOTS = 4

    val videoPages: List<Intro1Page.VideoPage> = listOf(
        Intro1Page.VideoPage(
            videoKey = VideoTitleKeys.INTRO_VIDEO_1,
            fallbackImageRes = R.drawable.fallbabk_1,
            titleRes = R.string.intro1_page1_title,
            subtitleRes = R.string.intro1_page1_subtitle,
            buttonRes = R.string.get_started
        ),
        Intro1Page.VideoPage(
            videoKey = VideoTitleKeys.INTRO_VIDEO_2,
            fallbackImageRes = R.drawable.fallbabk_2,
            titleRes = R.string.intro1_page2_title,
            subtitleRes = R.string.intro1_page2_subtitle,
            buttonRes = R.string.continue_text
        ),
        Intro1Page.VideoPage(
            videoKey = VideoTitleKeys.INTRO_VIDEO_3,
            fallbackImageRes = R.drawable.fallbabk_3,
            titleRes = R.string.intro1_page3_title,
            subtitleRes = R.string.intro1_page3_subtitle,
            buttonRes = R.string.continue_text
        )
    )

    val ratingPage = Intro1Page.RatingPage(
        titleRes = R.string.intro1_page4_title,
        subtitleRes = R.string.intro1_page4_subtitle,
        buttonRes = R.string.continue_text,
        reviews = IntroReviewData.reviews
    )

    /** Single-screen ViewPager: 3 video pages + rating page. */
    val pages: List<Intro1Page> = videoPages + ratingPage
}

sealed class Intro1Page {
    data class VideoPage(
        val videoKey: String,
        @DrawableRes val fallbackImageRes: Int,
        @StringRes val titleRes: Int,
        @StringRes val subtitleRes: Int,
        @StringRes val buttonRes: Int
    ) : Intro1Page()

    data class RatingPage(
        @StringRes val titleRes: Int,
        @StringRes val subtitleRes: Int,
        @StringRes val buttonRes: Int,
        val reviews: List<IntroReviewItem>
    ) : Intro1Page()
}

data class IntroReviewItem(
    @StringRes val titleRes: Int,
    @StringRes val bodyRes: Int,
    @DrawableRes val flagRes: Int
)

object IntroReviewData {
    val reviews = listOf(
        IntroReviewItem(
            titleRes = R.string.intro_review_1_title,
            bodyRes = R.string.intro_review_1_body,
            flagRes = R.drawable.ic_flag1
        ),
        IntroReviewItem(
            titleRes = R.string.intro_review_2_title,
            bodyRes = R.string.intro_review_2_body,
            flagRes = R.drawable.ic_flag2
        ),
        IntroReviewItem(
            titleRes = R.string.intro_review_3_title,
            bodyRes = R.string.intro_review_3_body,
            flagRes = R.drawable.ic_flag3
        ),
        IntroReviewItem(
            titleRes = R.string.intro_review_4_title,
            bodyRes = R.string.intro_review_4_body,
            flagRes = R.drawable.ic_flag4
        )
    )
}

package com.hairstyle.generator.aimakerover.domain.model

data class FunStyle(
    override val id: Int,
    override val title: String,
    override val description: String,
    val hairstyleApiValue: String,
    val hairColorApiValue: String,
    override val beforeImageUrl: String,
    override val afterImageUrl: String
) : BeforeAfterItem

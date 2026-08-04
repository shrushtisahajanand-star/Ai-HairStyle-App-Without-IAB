package com.hairstyle.generator.aimakerover.domain.model

data class BeardStyle(
    override val id: Int,
    override val title: String,
    override val description: String,
    val apiValue: String,
    override val beforeImageUrl: String,
    override val afterImageUrl: String
) : BeforeAfterItem

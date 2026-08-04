package com.hairstyle.generator.aimakerover.domain.model

data class CategoryHairstyle(
    val id: Int,
    /** UI label, e.g. "Male hairstyle 01" */
    val title: String,
    /** Generate API value, e.g. "male_hairstyle_0001" */
    val apiValue: String,
    val imageUrl: String
)

package com.hairstyle.generator.aimakerover.domain.model

data class UserCreation(
    val id: String,
    val localPath: String,
    val imageUrl: String? = null,
    val createdAt: Long = System.currentTimeMillis()
)

package com.hairstyle.changer.aitryon.domain.model

data class UserCreation(
    val id: String,
    val localPath: String,
    val imageUrl: String? = null,
    val createdAt: Long = System.currentTimeMillis()
)

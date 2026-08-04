package com.hairstyle.generator.aimakerover.domain.model

import android.net.Uri

data class GalleryAlbum(
    val id: String,
    val name: String,
    val coverUri: Uri?
)

data class GalleryPhoto(
    val id: Long,
    val uri: Uri
)

package com.hairstyle.generator.aimakerover.data.repository

import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.provider.MediaStore
import com.hairstyle.generator.aimakeover.R
import com.hairstyle.generator.aimakerover.domain.model.GalleryAlbum
import com.hairstyle.generator.aimakerover.domain.model.GalleryPhoto
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DeviceGalleryRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {

    fun loadAlbums(): List<GalleryAlbum> {
        val albums = linkedMapOf<String, GalleryAlbum>()
        val projection = arrayOf(
            MediaStore.Images.Media.BUCKET_ID,
            MediaStore.Images.Media.BUCKET_DISPLAY_NAME,
            MediaStore.Images.Media._ID
        )
        val sortOrder = "${MediaStore.Images.Media.DATE_ADDED} DESC"
        val collection = MediaStore.Images.Media.EXTERNAL_CONTENT_URI

        context.contentResolver.query(
            collection,
            projection,
            null,
            null,
            sortOrder
        )?.use { cursor ->
            val bucketIdCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.BUCKET_ID)
            val bucketNameCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.BUCKET_DISPLAY_NAME)
            val idCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID)

            while (cursor.moveToNext()) {
                val bucketId = cursor.getString(bucketIdCol) ?: continue
                val bucketName = cursor.getString(bucketNameCol)?.takeIf { it.isNotBlank() } ?: continue
                if (albums.containsKey(bucketId)) continue

                val imageId = cursor.getLong(idCol)
                val coverUri = ContentUris.withAppendedId(collection, imageId)
                albums[bucketId] = GalleryAlbum(
                    id = bucketId,
                    name = bucketName,
                    coverUri = coverUri
                )
            }
        }

        val allCover = albums.values.firstOrNull()?.coverUri
        val allAlbum = GalleryAlbum(
            id = ALBUM_ALL_ID,
            name = context.getString(R.string.gallery_album_all),
            coverUri = allCover
        )
        return listOf(allAlbum) + albums.values.toList()
    }

    fun loadPhotos(albumId: String): List<GalleryPhoto> {
        val photos = mutableListOf<GalleryPhoto>()
        val collection = MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        val projection = arrayOf(MediaStore.Images.Media._ID)
        val sortOrder = "${MediaStore.Images.Media.DATE_ADDED} DESC"

        val selection: String?
        val selectionArgs: Array<String>?
        if (albumId == ALBUM_ALL_ID) {
            selection = null
            selectionArgs = null
        } else {
            selection = "${MediaStore.Images.Media.BUCKET_ID} = ?"
            selectionArgs = arrayOf(albumId)
        }

        context.contentResolver.query(
            collection,
            projection,
            selection,
            selectionArgs,
            sortOrder
        )?.use { cursor ->
            val idCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
            while (cursor.moveToNext()) {
                val id = cursor.getLong(idCol)
                val uri = ContentUris.withAppendedId(collection, id)
                photos += GalleryPhoto(id = id, uri = uri)
            }
        }
        return photos
    }

    companion object {
        const val ALBUM_ALL_ID = "all"
    }
}

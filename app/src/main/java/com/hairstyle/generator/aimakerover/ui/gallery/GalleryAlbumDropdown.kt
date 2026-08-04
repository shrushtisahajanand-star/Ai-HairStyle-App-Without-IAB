package com.hairstyle.generator.aimakerover.ui.gallery

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.PopupWindow
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.hairstyle.generator.aimakeover.R
import com.hairstyle.generator.aimakerover.domain.model.GalleryAlbum

object GalleryAlbumDropdown {

    fun show(
        anchor: View,
        albums: List<GalleryAlbum>,
        selectedAlbumId: String,
        onAlbumSelected: (GalleryAlbum) -> Unit
    ) {
        if (albums.isEmpty()) return

        val contentView = LayoutInflater.from(anchor.context)
            .inflate(R.layout.layout_gallery_album_dropdown, null)
        val recyclerView = contentView.findViewById<RecyclerView>(R.id.rvAlbums)
        val density = anchor.resources.displayMetrics.density
        val screenWidth = anchor.resources.displayMetrics.widthPixels
        val maxDropdownHeight = (anchor.resources.displayMetrics.heightPixels * 0.55f).toInt()
        val popupWidth = maxOf(
            (260 * density).toInt(),
            (screenWidth * 0.68f).toInt(),
            anchor.width
        )
        contentView.minimumWidth = popupWidth

        val popupWindow = PopupWindow(
            contentView,
            popupWidth,
            ViewGroup.LayoutParams.WRAP_CONTENT,
            true
        ).apply {
            elevation = anchor.resources.getDimension(com.intuit.sdp.R.dimen._6sdp)
            isOutsideTouchable = true
            setBackgroundDrawable(null)
        }

        recyclerView.layoutManager = LinearLayoutManager(anchor.context)
        recyclerView.adapter = GalleryAlbumDropdownAdapter(
            albums = albums,
            selectedAlbumId = selectedAlbumId,
            onAlbumClick = { album ->
                onAlbumSelected(album)
                popupWindow.dismiss()
            }
        )

        recyclerView.post {
            recyclerView.measure(
                View.MeasureSpec.makeMeasureSpec(popupWidth, View.MeasureSpec.EXACTLY),
                View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
            )
            val cappedHeight = recyclerView.measuredHeight.coerceAtMost(maxDropdownHeight)
            recyclerView.layoutParams = recyclerView.layoutParams.apply {
                height = cappedHeight
            }
            recyclerView.requestLayout()
        }

        val yOffset = (6 * anchor.resources.displayMetrics.density).toInt()
        popupWindow.showAsDropDown(anchor, 0, yOffset)
    }
}

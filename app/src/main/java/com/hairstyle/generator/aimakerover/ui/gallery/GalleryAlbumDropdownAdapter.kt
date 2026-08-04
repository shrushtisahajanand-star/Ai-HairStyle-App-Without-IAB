package com.hairstyle.generator.aimakerover.ui.gallery

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.core.content.res.ResourcesCompat
import androidx.recyclerview.widget.RecyclerView
import com.hairstyle.generator.aimakeover.R
import com.hairstyle.generator.aimakeover.databinding.ItemGalleryAlbumDropdownBinding
import com.hairstyle.generator.aimakerover.domain.model.GalleryAlbum

class GalleryAlbumDropdownAdapter(
    private val albums: List<GalleryAlbum>,
    private val selectedAlbumId: String,
    private val onAlbumClick: (GalleryAlbum) -> Unit
) : RecyclerView.Adapter<GalleryAlbumDropdownAdapter.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemGalleryAlbumDropdownBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(albums[position], position)
    }

    override fun getItemCount(): Int = albums.size

    inner class ViewHolder(
        private val binding: ItemGalleryAlbumDropdownBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(album: GalleryAlbum, position: Int) {
            val selected = album.id == selectedAlbumId
            binding.divider.isVisible = position < albums.lastIndex
            binding.tvAlbumName.text = album.name
            binding.tvAlbumName.setTextColor(
                ContextCompat.getColor(
                    binding.root.context,
                    if (selected) R.color.white else R.color.nav_inactive
                )
            )
            binding.tvAlbumName.typeface = ResourcesCompat.getFont(
                binding.root.context,
                if (selected) R.font.saans_semibold else R.font.saans_medium
            )
            binding.root.setOnClickListener { onAlbumClick(album) }
        }
    }
}

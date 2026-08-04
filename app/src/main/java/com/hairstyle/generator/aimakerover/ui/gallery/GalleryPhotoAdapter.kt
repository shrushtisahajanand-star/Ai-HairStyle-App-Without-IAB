package com.hairstyle.generator.aimakerover.ui.gallery

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.hairstyle.generator.aimakeover.databinding.ItemGalleryPhotoBinding
import com.hairstyle.generator.aimakerover.domain.model.GalleryPhoto

class GalleryPhotoAdapter(
    private val onPhotoClick: (GalleryPhoto) -> Unit
) : ListAdapter<GalleryPhoto, GalleryPhotoAdapter.ViewHolder>(DiffCallback()) {

    private var selectedPhotoId: Long? = null

    fun setSelectedPhotoId(id: Long?) {
        val previous = selectedPhotoId
        selectedPhotoId = id
        currentList.forEachIndexed { index, photo ->
            if (photo.id == previous || photo.id == id) {
                notifyItemChanged(index)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemGalleryPhotoBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position), getItem(position).id == selectedPhotoId)
    }

    inner class ViewHolder(
        private val binding: ItemGalleryPhotoBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(photo: GalleryPhoto, selected: Boolean) {
            binding.ivPhoto.load(photo.uri) {
                crossfade(true)
            }
            binding.selectionRing.isVisible = selected
            binding.root.setOnClickListener { onPhotoClick(photo) }
        }
    }

    private class DiffCallback : DiffUtil.ItemCallback<GalleryPhoto>() {
        override fun areItemsTheSame(oldItem: GalleryPhoto, newItem: GalleryPhoto) =
            oldItem.id == newItem.id

        override fun areContentsTheSame(oldItem: GalleryPhoto, newItem: GalleryPhoto) =
            oldItem == newItem
    }
}

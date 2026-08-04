package com.hairstyle.generator.aimakerover.ui.home

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import coil.dispose
import com.hairstyle.generator.aimakeover.databinding.ItemHomeHairstyleBinding
import com.hairstyle.generator.aimakerover.domain.model.CategoryHairstyle
import com.hairstyle.generator.aimakerover.utils.hideImageShimmer
import com.hairstyle.generator.aimakerover.utils.loadWithShimmer

class HomeHairstyleAdapter(
    private val onItemClick: (CategoryHairstyle) -> Unit = {}
) : ListAdapter<CategoryHairstyle, HomeHairstyleAdapter.ViewHolder>(DiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemHomeHairstyleBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    override fun onViewRecycled(holder: ViewHolder) {
        holder.clear()
        super.onViewRecycled(holder)
    }

    inner class ViewHolder(
        private val binding: ItemHomeHairstyleBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        private var loadedUrl: String? = null

        fun bind(item: CategoryHairstyle) {
            binding.tvTitle.text = item.title
            if (loadedUrl != item.imageUrl) {
                loadedUrl = item.imageUrl
                binding.ivHairstyle.loadWithShimmer(item.imageUrl, binding.shimmerHairstyle)
            }
            binding.root.setOnClickListener { onItemClick(item) }
        }

        fun clear() {
            loadedUrl = null
            binding.ivHairstyle.dispose()
            binding.shimmerHairstyle.hideImageShimmer()
        }
    }

    private class DiffCallback : DiffUtil.ItemCallback<CategoryHairstyle>() {
        override fun areItemsTheSame(old: CategoryHairstyle, new: CategoryHairstyle) =
            old.id == new.id

        override fun areContentsTheSame(old: CategoryHairstyle, new: CategoryHairstyle) =
            old == new
    }
}

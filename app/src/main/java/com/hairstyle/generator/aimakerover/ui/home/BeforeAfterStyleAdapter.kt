package com.hairstyle.generator.aimakerover.ui.home

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import coil.dispose
import com.hairstyle.generator.aimakeover.databinding.ItemBeardStyleBinding
import com.hairstyle.generator.aimakerover.domain.model.BeforeAfterItem
import com.hairstyle.generator.aimakerover.utils.hideImageShimmer
import com.hairstyle.generator.aimakerover.utils.loadWithShimmer

class BeforeAfterStyleAdapter<T : BeforeAfterItem>(
    private val onTryItClick: (T) -> Unit
) : ListAdapter<T, BeforeAfterStyleAdapter<T>.ViewHolder>(DiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemBeardStyleBinding.inflate(
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
        private val binding: ItemBeardStyleBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: T) {
            binding.tvTitle.text = item.title
            binding.tvDescription.text = item.description
            binding.ivBefore.loadWithShimmer(item.beforeImageUrl, binding.shimmerBefore)
            binding.ivAfter.loadWithShimmer(item.afterImageUrl, binding.shimmerAfter)

            val openPhotoSelect = { onTryItClick(item) }
            binding.btnTryIt.setOnClickListener { openPhotoSelect() }
            binding.ivAfter.setOnClickListener { openPhotoSelect() }
            binding.afterImageContainer.setOnClickListener { openPhotoSelect() }
        }

        fun clear() {
            binding.ivBefore.dispose()
            binding.ivAfter.dispose()
            binding.shimmerBefore.hideImageShimmer()
            binding.shimmerAfter.hideImageShimmer()
        }
    }

    private class DiffCallback<T : BeforeAfterItem> : DiffUtil.ItemCallback<T>() {
        override fun areItemsTheSame(oldItem: T, newItem: T) = oldItem.id == newItem.id

        override fun areContentsTheSame(oldItem: T, newItem: T) = oldItem == newItem
    }
}

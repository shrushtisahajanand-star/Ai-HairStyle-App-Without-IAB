package com.hairstyle.changer.aitryon.ui.home

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import coil.dispose
import com.hairstyle.changer.aitryon.databinding.ItemHomeHairstyleBinding
import com.hairstyle.changer.aitryon.domain.model.CategoryHairstyle
import com.hairstyle.changer.aitryon.ui.common.ItemLockGate
import com.hairstyle.changer.aitryon.utils.ClickGuard
import com.hairstyle.changer.aitryon.utils.hideImageShimmer
import com.hairstyle.changer.aitryon.utils.loadWithShimmer

class HomeHairstyleAdapter(
    private val lockGate: ItemLockGate? = null,
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
        val item = getItem(position)
        holder.bind(item, lockGate?.isLocked(position, item.id, item.title) == true)
    }

    override fun onViewRecycled(holder: ViewHolder) {
        holder.clear()
        super.onViewRecycled(holder)
    }

    /** Re-bind one item (its lock badge disappears after it was unlocked). */
    private fun refreshItem(id: Int) {
        val index = currentList.indexOfFirst { it.id == id }
        if (index >= 0) notifyItemChanged(index)
    }

    inner class ViewHolder(
        private val binding: ItemHomeHairstyleBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        private var loadedUrl: String? = null

        fun bind(item: CategoryHairstyle, locked: Boolean) {
            binding.ivLock.visibility = if (locked) android.view.View.VISIBLE else android.view.View.GONE
            binding.tvTitle.text = item.title
            if (loadedUrl != item.imageUrl) {
                loadedUrl = item.imageUrl
                binding.ivHairstyle.loadWithShimmer(item.imageUrl, binding.shimmerHairstyle)
            }
            binding.root.setOnClickListener {
                if (!ClickGuard.allow()) return@setOnClickListener
                val gate = lockGate
                if (gate == null) {
                    onItemClick(item)
                } else {
                    gate.run(bindingAdapterPosition, item.id, item.title) {
                        refreshItem(item.id)
                        onItemClick(item)
                    }
                }
            }
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

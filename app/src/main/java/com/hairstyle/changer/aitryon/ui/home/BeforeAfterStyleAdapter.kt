package com.hairstyle.changer.aitryon.ui.home

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import coil.dispose
import com.hairstyle.changer.aitryon.databinding.ItemBeardStyleBinding
import com.hairstyle.changer.aitryon.domain.model.BeforeAfterItem
import com.hairstyle.changer.aitryon.ui.common.ItemLockGate
import com.hairstyle.changer.aitryon.utils.ClickGuard
import com.hairstyle.changer.aitryon.utils.hideImageShimmer
import com.hairstyle.changer.aitryon.utils.loadWithShimmer

class BeforeAfterStyleAdapter<T : BeforeAfterItem>(
    private val lockGate: ItemLockGate? = null,
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
        private val binding: ItemBeardStyleBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: T, locked: Boolean) {
            binding.ivLock.visibility = if (locked) android.view.View.VISIBLE else android.view.View.GONE
            binding.tvTitle.text = item.title
            binding.tvDescription.text = item.description
            binding.ivBefore.loadWithShimmer(item.beforeImageUrl, binding.shimmerBefore)
            binding.ivAfter.loadWithShimmer(item.afterImageUrl, binding.shimmerAfter)

            val openPhotoSelect: () -> Unit = openPhotoSelect@{
                if (!ClickGuard.allow()) return@openPhotoSelect
                val gate = lockGate
                if (gate == null) {
                    onTryItClick(item)
                } else {
                    gate.run(bindingAdapterPosition, item.id, item.title) {
                        refreshItem(item.id)
                        onTryItClick(item)
                    }
                }
            }
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

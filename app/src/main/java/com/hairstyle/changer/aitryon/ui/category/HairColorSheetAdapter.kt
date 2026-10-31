package com.hairstyle.changer.aitryon.ui.category

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.hairstyle.changer.aitryon.databinding.ItemHairColorSheetBinding
import com.hairstyle.changer.aitryon.domain.model.CategoryHairstyle
import com.hairstyle.changer.aitryon.ui.common.ItemLockGate
import com.hairstyle.changer.aitryon.utils.hideImageShimmer
import com.hairstyle.changer.aitryon.utils.loadWithShimmer

class HairColorSheetAdapter(
    private val lockGate: ItemLockGate? = null,
    private val onItemClick: (CategoryHairstyle) -> Unit
) : ListAdapter<CategoryHairstyle, HairColorSheetAdapter.ViewHolder>(DiffCallback()) {

    private var selectedId: Int? = null

    fun setSelectedId(id: Int?) {
        val previous = selectedId
        selectedId = id
        currentList.forEachIndexed { index, item ->
            if (item.id == previous || item.id == id) notifyItemChanged(index)
        }
    }

    fun getSelected(): CategoryHairstyle? =
        currentList.firstOrNull { it.id == selectedId }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemHairColorSheetBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = getItem(position)
        holder.bind(item, item.id == selectedId, lockGate?.isLocked(position, item.id, item.title) == true)
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
        private val binding: ItemHairColorSheetBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: CategoryHairstyle, selected: Boolean, locked: Boolean) {
            binding.ivLock.visibility = if (locked) android.view.View.VISIBLE else android.view.View.GONE
            binding.tvColorName.text = item.title
            binding.ivColor.loadWithShimmer(item.imageUrl, binding.shimmerColor)
            binding.cardColor.strokeWidth = if (selected) {
                (1.5f * binding.root.resources.displayMetrics.density).toInt().coerceAtLeast(1)
            } else {
                0
            }
            binding.root.setOnClickListener {
                val proceed = {
                    setSelectedId(item.id)
                    onItemClick(item)
                }
                val gate = lockGate
                if (gate == null) {
                    proceed()
                } else {
                    gate.run(bindingAdapterPosition, item.id, item.title) {
                        refreshItem(item.id)
                        proceed()
                    }
                }
            }
        }

        fun clear() {
            binding.shimmerColor.hideImageShimmer()
        }
    }

    private class DiffCallback : DiffUtil.ItemCallback<CategoryHairstyle>() {
        override fun areItemsTheSame(old: CategoryHairstyle, new: CategoryHairstyle) =
            old.id == new.id

        override fun areContentsTheSame(old: CategoryHairstyle, new: CategoryHairstyle) =
            old == new
    }
}

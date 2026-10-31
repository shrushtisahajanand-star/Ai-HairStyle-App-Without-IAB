package com.hairstyle.changer.aitryon.ui.category

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import coil.dispose
import com.hairstyle.changer.aitryon.databinding.ItemCategoryHairstyleBinding
import com.hairstyle.changer.aitryon.domain.model.CategoryHairstyle
import com.hairstyle.changer.aitryon.ui.common.ItemLockGate
import com.hairstyle.changer.aitryon.utils.hideImageShimmer
import com.hairstyle.changer.aitryon.utils.loadWithShimmer

class CategoryHairstyleAdapter(
    private val lockGate: ItemLockGate? = null,
    private val onItemClick: (CategoryHairstyle) -> Unit
) : ListAdapter<CategoryHairstyle, CategoryHairstyleAdapter.ViewHolder>(DiffCallback()) {

    private var selectedId: Int? = null

    fun setSelectedId(id: Int?) {
        val previous = selectedId
        selectedId = id
        currentList.forEachIndexed { index, item ->
            if (item.id == previous || item.id == id) {
                notifyItemChanged(index, PAYLOAD_SELECTION)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemCategoryHairstyleBinding.inflate(
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

    override fun onBindViewHolder(holder: ViewHolder, position: Int, payloads: MutableList<Any>) {
        if (payloads.contains(PAYLOAD_SELECTION)) {
            holder.bindSelection(getItem(position).id == selectedId)
        } else {
            super.onBindViewHolder(holder, position, payloads)
        }
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
        private val binding: ItemCategoryHairstyleBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        private val borderInsetPx =
            (2 * binding.root.resources.displayMetrics.density).toInt()
        private var loadedUrl: String? = null

        fun bind(item: CategoryHairstyle, selected: Boolean, locked: Boolean) {
            binding.ivLock.visibility = if (locked) View.VISIBLE else View.GONE
            if (loadedUrl != item.imageUrl) {
                loadedUrl = item.imageUrl
                binding.ivHairstyle.loadWithShimmer(item.imageUrl, binding.shimmerHairstyle)
            }
            bindSelection(selected)
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

        fun bindSelection(selected: Boolean) {
            binding.vGradientBorder.visibility = if (selected) View.VISIBLE else View.GONE
            (binding.cardHairstyle.layoutParams as FrameLayout.LayoutParams).setMargins(
                if (selected) borderInsetPx else 0,
                if (selected) borderInsetPx else 0,
                if (selected) borderInsetPx else 0,
                if (selected) borderInsetPx else 0
            )
            binding.ivSelected.visibility = if (selected) View.VISIBLE else View.GONE
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

    companion object {
        private const val PAYLOAD_SELECTION = "selection"
    }
}

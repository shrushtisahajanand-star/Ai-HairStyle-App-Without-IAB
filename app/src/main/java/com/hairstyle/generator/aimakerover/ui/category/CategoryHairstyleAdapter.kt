package com.hairstyle.generator.aimakerover.ui.category

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import coil.dispose
import com.hairstyle.generator.aimakeover.databinding.ItemCategoryHairstyleBinding
import com.hairstyle.generator.aimakerover.domain.model.CategoryHairstyle
import com.hairstyle.generator.aimakerover.utils.hideImageShimmer
import com.hairstyle.generator.aimakerover.utils.loadWithShimmer

class CategoryHairstyleAdapter(
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
        holder.bind(getItem(position), getItem(position).id == selectedId)
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

    inner class ViewHolder(
        private val binding: ItemCategoryHairstyleBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        private val borderInsetPx =
            (2 * binding.root.resources.displayMetrics.density).toInt()
        private var loadedUrl: String? = null

        fun bind(item: CategoryHairstyle, selected: Boolean) {
            if (loadedUrl != item.imageUrl) {
                loadedUrl = item.imageUrl
                binding.ivHairstyle.loadWithShimmer(item.imageUrl, binding.shimmerHairstyle)
            }
            bindSelection(selected)
            binding.root.setOnClickListener {
                setSelectedId(item.id)
                onItemClick(item)
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

package com.hairstyle.changer.aitryon.ui.category

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.hairstyle.changer.aitryon.databinding.ItemHairColorSheetBinding
import com.hairstyle.changer.aitryon.domain.model.CategoryHairstyle
import com.hairstyle.changer.aitryon.utils.hideImageShimmer
import com.hairstyle.changer.aitryon.utils.loadWithShimmer

class HairColorSheetAdapter(
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
        holder.bind(getItem(position), getItem(position).id == selectedId)
    }

    override fun onViewRecycled(holder: ViewHolder) {
        holder.clear()
        super.onViewRecycled(holder)
    }

    inner class ViewHolder(
        private val binding: ItemHairColorSheetBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: CategoryHairstyle, selected: Boolean) {
            binding.tvColorName.text = item.title
            binding.ivColor.loadWithShimmer(item.imageUrl, binding.shimmerColor)
            binding.cardColor.strokeWidth = if (selected) {
                (1.5f * binding.root.resources.displayMetrics.density).toInt().coerceAtLeast(1)
            } else {
                0
            }
            binding.root.setOnClickListener {
                setSelectedId(item.id)
                onItemClick(item)
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

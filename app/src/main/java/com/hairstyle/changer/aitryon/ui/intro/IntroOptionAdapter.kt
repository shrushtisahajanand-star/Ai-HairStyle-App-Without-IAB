package com.hairstyle.changer.aitryon.ui.intro

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.hairstyle.changer.aitryon.databinding.ItemIntroOptionBinding

class IntroOptionAdapter(
    private val onItemClick: (IntroOption) -> Unit
) : ListAdapter<IntroOption, IntroOptionAdapter.IntroOptionViewHolder>(DiffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): IntroOptionViewHolder {
        val binding = ItemIntroOptionBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return IntroOptionViewHolder(binding, onItemClick)
    }

    override fun onBindViewHolder(holder: IntroOptionViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class IntroOptionViewHolder(
        private val binding: ItemIntroOptionBinding,
        private val onItemClick: (IntroOption) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: IntroOption) {
            binding.ivImage.setImageResource(item.imageRes)
            binding.tvTitle.setText(item.titleRes)
            binding.tvSubtitle.setText(item.subtitleRes)
            binding.root.setOnClickListener { onItemClick(item) }
        }
    }

    private object DiffCallback : DiffUtil.ItemCallback<IntroOption>() {
        override fun areItemsTheSame(oldItem: IntroOption, newItem: IntroOption): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: IntroOption, newItem: IntroOption): Boolean {
            return oldItem == newItem
        }
    }
}

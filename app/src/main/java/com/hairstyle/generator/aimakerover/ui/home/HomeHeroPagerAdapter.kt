package com.hairstyle.generator.aimakerover.ui.home

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import coil.dispose
import coil.load
import com.hairstyle.generator.aimakeover.R
import com.hairstyle.generator.aimakeover.databinding.ItemHomeHeroImageBinding
import com.hairstyle.generator.aimakerover.domain.model.HomeHeroSlide

class HomeHeroPagerAdapter(
    private val onHeroClick: (() -> Unit)? = null
) : ListAdapter<HomeHeroSlide, HomeHeroPagerAdapter.ViewHolder>(DiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemHomeHeroImageBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(binding, onHeroClick)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    override fun onViewRecycled(holder: ViewHolder) {
        holder.clear()
        super.onViewRecycled(holder)
    }

    class ViewHolder(
        private val binding: ItemHomeHeroImageBinding,
        private val onHeroClick: (() -> Unit)?
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: HomeHeroSlide) {
            binding.root.setOnClickListener { onHeroClick?.invoke() }
            if (item.imageUrl.isBlank()) {
                binding.ivHeroImage.dispose()
                binding.ivHeroImage.setImageResource(R.drawable.home_image)
            } else {
                binding.ivHeroImage.load(item.imageUrl) {
                    crossfade(true)
                }
            }
        }

        fun clear() {
            binding.ivHeroImage.dispose()
            binding.root.setOnClickListener(null)
        }
    }

    private class DiffCallback : DiffUtil.ItemCallback<HomeHeroSlide>() {
        override fun areItemsTheSame(oldItem: HomeHeroSlide, newItem: HomeHeroSlide) =
            oldItem.id == newItem.id

        override fun areContentsTheSame(oldItem: HomeHeroSlide, newItem: HomeHeroSlide) =
            oldItem == newItem
    }
}

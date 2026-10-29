package com.hairstyle.changer.aitryon.ui.intro

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.hairstyle.changer.aitryon.databinding.ItemIntroReviewCardBinding

class IntroReviewAdapter : RecyclerView.Adapter<IntroReviewAdapter.ReviewViewHolder>() {

    private val items = mutableListOf<IntroReviewItem>()

    val realItemCount: Int
        get() = if (items.size <= 1) items.size else items.size / 2

    fun submitList(reviews: List<IntroReviewItem>) {
        items.clear()
        if (reviews.isEmpty()) {
            notifyDataSetChanged()
            return
        }
        items.addAll(reviews)
        if (reviews.size > 1) {
            items.addAll(reviews)
        }
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ReviewViewHolder {
        val binding = ItemIntroReviewCardBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ReviewViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ReviewViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int = items.size

    class ReviewViewHolder(
        private val binding: ItemIntroReviewCardBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: IntroReviewItem) {
            binding.tvReviewTitle.setText(item.titleRes)
            binding.tvReviewBody.setText(item.bodyRes)
            binding.ivFlag.setImageResource(item.flagRes)
        }
    }
}

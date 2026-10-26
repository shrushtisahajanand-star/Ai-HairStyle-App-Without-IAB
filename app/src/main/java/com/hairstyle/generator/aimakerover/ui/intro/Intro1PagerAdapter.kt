package com.hairstyle.generator.aimakerover.ui.intro

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.hairstyle.generator.aimakeover.R

class Intro1PagerAdapter : RecyclerView.Adapter<Intro1PagerAdapter.PageViewHolder>() {

    private val pages = mutableListOf<Intro1Page>()

    var onRatingPageBound: ((RecyclerView, Intro1Page.RatingPage) -> Unit)? = null

    fun submitPages(list: List<Intro1Page>) {
        pages.clear()
        pages.addAll(list)
        notifyDataSetChanged()
    }

    override fun getItemCount(): Int = pages.size

    override fun getItemViewType(position: Int): Int = when (pages[position]) {
        is Intro1Page.VideoPage -> VIEW_TYPE_VIDEO
        is Intro1Page.RatingPage -> VIEW_TYPE_RATING
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PageViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val view = when (viewType) {
            VIEW_TYPE_VIDEO -> inflater.inflate(R.layout.item_intro1_video_page, parent, false)
            else -> inflater.inflate(R.layout.item_intro1_rating_page, parent, false)
        }
        return PageViewHolder(view)
    }

    override fun onBindViewHolder(holder: PageViewHolder, position: Int) {
        when (val page = pages[position]) {
            is Intro1Page.RatingPage -> {
                holder.itemView.findViewById<RecyclerView>(R.id.rvReviews)?.let { recyclerView ->
                    onRatingPageBound?.invoke(recyclerView, page)
                }
            }
            is Intro1Page.VideoPage -> Unit
        }
    }

    class PageViewHolder(view: View) : RecyclerView.ViewHolder(view)

    companion object {
        private const val VIEW_TYPE_VIDEO = 0
        private const val VIEW_TYPE_RATING = 1
    }
}

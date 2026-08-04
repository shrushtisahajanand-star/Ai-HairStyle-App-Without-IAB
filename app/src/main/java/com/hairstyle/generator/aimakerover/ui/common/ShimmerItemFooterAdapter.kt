package com.hairstyle.generator.aimakerover.ui.common

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.annotation.LayoutRes
import androidx.recyclerview.widget.RecyclerView
import com.facebook.shimmer.ShimmerFrameLayout
import com.hairstyle.generator.aimakerover.utils.applyAppShimmer
import com.hairstyle.generator.aimakerover.utils.shouldShowListShimmer

class ShimmerItemFooterAdapter(
    @LayoutRes private val itemLayoutRes: Int,
    private val shimmerItemCount: Int = 2,
    initiallyVisible: Boolean = false
) : RecyclerView.Adapter<ShimmerItemFooterAdapter.ViewHolder>() {

    private var showShimmerItems: Boolean = initiallyVisible
        set(value) {
            if (field == value) return
            field = value
            notifyDataSetChanged()
        }

    fun bindLoadingMore(loadingMore: Boolean, hasItems: Boolean, hasMore: Boolean = true) {
        showShimmerItems = loadingMore && hasItems && hasMore
    }

    fun bindPlaceholder(isLoading: Boolean, hasItems: Boolean, hasError: Boolean) {
        showShimmerItems = shouldShowListShimmer(isLoading, hasItems, hasError)
    }

    override fun getItemCount(): Int = if (showShimmerItems) shimmerItemCount else 0

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(itemLayoutRes, parent, false)
        return ViewHolder(view as ShimmerFrameLayout)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind()
    }

    override fun onViewAttachedToWindow(holder: ViewHolder) {
        super.onViewAttachedToWindow(holder)
        holder.bind()
    }

    override fun onViewDetachedFromWindow(holder: ViewHolder) {
        holder.stop()
        super.onViewDetachedFromWindow(holder)
    }

    class ViewHolder(
        private val shimmerLayout: ShimmerFrameLayout
    ) : RecyclerView.ViewHolder(shimmerLayout) {

        fun bind() {
            shimmerLayout.applyAppShimmer()
            shimmerLayout.post {
                shimmerLayout.startShimmer()
            }
        }

        fun stop() {
            shimmerLayout.stopShimmer()
        }
    }
}

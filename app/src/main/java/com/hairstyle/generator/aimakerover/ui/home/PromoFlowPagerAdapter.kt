package com.hairstyle.generator.aimakerover.ui.home

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.hairstyle.generator.aimakeover.databinding.PagePromoPremiumPaywallBinding
import com.hairstyle.generator.aimakeover.databinding.PageSecondVisitPromoBinding

class PromoFlowPagerAdapter : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    var promoBinding: PageSecondVisitPromoBinding? = null
        private set
    var premiumBinding: PagePromoPremiumPaywallBinding? = null
        private set

    override fun getItemCount(): Int = PAGE_COUNT

    override fun getItemViewType(position: Int): Int = position

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return when (viewType) {
            PAGE_PROMO -> {
                val binding = PageSecondVisitPromoBinding.inflate(inflater, parent, false)
                promoBinding = binding
                PromoViewHolder(binding)
            }

            else -> {
                val binding = PagePromoPremiumPaywallBinding.inflate(inflater, parent, false)
                premiumBinding = binding
                PremiumViewHolder(binding)
            }
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) = Unit

    private class PromoViewHolder(
        binding: PageSecondVisitPromoBinding
    ) : RecyclerView.ViewHolder(binding.root)

    private class PremiumViewHolder(
        binding: PagePromoPremiumPaywallBinding
    ) : RecyclerView.ViewHolder(binding.root)

    companion object {
        const val PAGE_PROMO = 0
        const val PAGE_PREMIUM = 1
        const val PAGE_COUNT = 2
    }
}

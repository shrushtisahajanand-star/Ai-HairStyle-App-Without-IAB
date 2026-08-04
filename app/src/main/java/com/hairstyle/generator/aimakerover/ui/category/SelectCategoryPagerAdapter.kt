package com.hairstyle.generator.aimakerover.ui.category

import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter

class SelectCategoryPagerAdapter(
    fragment: Fragment
) : FragmentStateAdapter(fragment) {

    override fun getItemCount(): Int = TAB_COUNT

    override fun createFragment(position: Int): Fragment {
        return SelectCategoryTabFragment.newInstance(position)
    }

    companion object {
        const val TAB_COUNT = 3
    }
}

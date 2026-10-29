package com.hairstyle.changer.aitryon.ui.category

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter

class SelectCategoryPagerAdapter(
    activity: FragmentActivity
) : FragmentStateAdapter(activity) {

    override fun getItemCount(): Int = TAB_COUNT

    override fun createFragment(position: Int): Fragment {
        return SelectCategoryTabFragment.newInstance(position)
    }

    companion object {
        const val TAB_COUNT = 3
    }
}

package com.hairstyle.changer.aitryon.ui.category

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.tabs.TabLayoutMediator
import com.hairstyle.changer.aitryon.R
import com.hairstyle.changer.aitryon.databinding.FragmentSelectCategoryBinding
import com.hairstyle.changer.aitryon.Ads.InterstitialNavHelper
import com.hairstyle.changer.aitryon.Ads.ScreenAdsHelper
import com.hairstyle.changer.aitryon.ui.BaseActivity
import com.hairstyle.changer.aitryon.ui.common.RateAppDialog
import com.hairstyle.changer.aitryon.utils.applyStatusBarPadding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class SelectCategoryActivity : BaseActivity() {

    override val useLightStatusBarIcons: Boolean = false

    private var _binding: FragmentSelectCategoryBinding? = null
    private val binding get() = _binding!!

    private val viewModel: SelectCategoryViewModel by viewModels()
    private var tabMediator: TabLayoutMediator? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        _binding = FragmentSelectCategoryBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setupWindow(binding.root)

        binding.root.applyStatusBarPadding()
        binding.toolbar.setNavigationOnClickListener {
            InterstitialNavHelper.navigateUpWithBackInter(this)
        }
        InterstitialNavHelper.installSystemBackInter(this)

        ScreenAdsHelper.load(this, binding.root, ScreenAdsHelper.selectImage())
        RateAppDialog.showIfNeededForAppOpen(this)

        val initialTab = intent.getIntExtra(ARG_TAB, 0)
        setupViewPager(initialTab)

        binding.btnGenerateHairstyle.setOnClickListener {
            val activeTab = viewModel.activeTab.value
            val selected = viewModel.selectedForTab(activeTab).value ?: return@setOnClickListener
            val photoAlreadySelected =
                intent.getBooleanExtra(ARG_PHOTO_ALREADY_SELECTED, false)
            HairColorBottomSheet.newInstance(
                previewImageUrl = selected.imageUrl,
                hairstyleTitle = selected.title,
                hairstyleApiValue = selected.apiValue,
                source = if (photoAlreadySelected) {
                    HairColorBottomSheet.Source.PHOTO_FIRST
                } else {
                    HairColorBottomSheet.Source.SELECT_CATEGORY
                }
            ).let { HairColorBottomSheet.showOnce(it, supportFragmentManager) }
        }

        observeSelection()
    }

    private fun setupViewPager(initialTab: Int) {
        val pagerAdapter = SelectCategoryPagerAdapter(this)
        binding.categoryViewPager.adapter = pagerAdapter
        binding.categoryViewPager.offscreenPageLimit = SelectCategoryPagerAdapter.TAB_COUNT
        binding.categoryViewPager.setCurrentItem(initialTab, false)

        val tabTitles = listOf(
            R.string.fun_tab_men,
            R.string.fun_tab_women,
            R.string.fun_tab_baby
        )
        tabMediator = TabLayoutMediator(binding.categoryTabLayout, binding.categoryViewPager) { tab, position ->
            tab.setText(tabTitles[position])
        }.also { it.attach() }

        binding.categoryViewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                viewModel.setActiveTab(position)
            }
        })

        viewModel.setActiveTab(initialTab)
    }

    private fun observeSelection() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.activeTab.collect { activeTab ->
                        val selected = viewModel.selectedForTab(activeTab).value
                        binding.btnGenerateHairstyle.isEnabled = selected != null
                    }
                }
                launch {
                    viewModel.selectedForTab(0).collect { updateGenerateButton() }
                }
                launch {
                    viewModel.selectedForTab(1).collect { updateGenerateButton() }
                }
                launch {
                    viewModel.selectedForTab(2).collect { updateGenerateButton() }
                }
            }
        }
    }

    private fun updateGenerateButton() {
        val activeTab = viewModel.activeTab.value
        binding.btnGenerateHairstyle.isEnabled =
            viewModel.selectedForTab(activeTab).value != null
    }

    override fun onDestroy() {
        tabMediator?.detach()
        tabMediator = null
        _binding = null
        super.onDestroy()
    }

    companion object {
        const val ARG_TAB = "initial_tab"
        const val ARG_PHOTO_ALREADY_SELECTED = "photo_already_selected"

        fun createIntent(
            context: Context,
            tab: Int = 0,
            photoAlreadySelected: Boolean = false
        ): Intent = Intent(context, SelectCategoryActivity::class.java).apply {
            putExtra(ARG_TAB, tab)
            putExtra(ARG_PHOTO_ALREADY_SELECTED, photoAlreadySelected)
        }
    }
}

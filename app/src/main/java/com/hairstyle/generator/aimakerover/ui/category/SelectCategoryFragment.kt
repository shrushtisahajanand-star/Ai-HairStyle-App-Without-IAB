package com.hairstyle.generator.aimakerover.ui.category

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.tabs.TabLayoutMediator
import com.hairstyle.generator.aimakeover.R
import com.hairstyle.generator.aimakeover.databinding.FragmentSelectCategoryBinding
import com.hairstyle.generator.aimakerover.Advertisement.InterstitialNavHelper
import com.hairstyle.generator.aimakerover.Advertisement.ScreenAdsHelper
import com.hairstyle.generator.aimakerover.ui.common.RateAppDialog
import com.hairstyle.generator.aimakerover.utils.applyStatusBarPadding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class SelectCategoryFragment : Fragment() {

    private var _binding: FragmentSelectCategoryBinding? = null
    private val binding get() = _binding!!

    private val viewModel: SelectCategoryViewModel by viewModels()
    private var tabMediator: TabLayoutMediator? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSelectCategoryBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.root.applyStatusBarPadding()
        binding.toolbar.setNavigationOnClickListener {
            InterstitialNavHelper.navigateUpWithBackInter(this)
        }
        InterstitialNavHelper.installSystemBackInter(this)

        ScreenAdsHelper.load(this, binding.root, ScreenAdsHelper.selectImage())
        RateAppDialog.showIfNeededForAppOpen(this)

        val initialTab = arguments?.getInt(ARG_TAB, 0) ?: 0
        setupViewPager(initialTab)

        binding.btnGenerateHairstyle.setOnClickListener {
            val activeTab = viewModel.activeTab.value
            val selected = viewModel.selectedForTab(activeTab).value ?: return@setOnClickListener
            val photoAlreadySelected =
                arguments?.getBoolean(ARG_PHOTO_ALREADY_SELECTED, false) == true
            HairColorBottomSheet.newInstance(
                previewImageUrl = selected.imageUrl,
                hairstyleTitle = selected.title,
                hairstyleApiValue = selected.apiValue,
                source = if (photoAlreadySelected) {
                    HairColorBottomSheet.Source.PHOTO_FIRST
                } else {
                    HairColorBottomSheet.Source.SELECT_CATEGORY
                }
            ).show(childFragmentManager, HairColorBottomSheet.TAG)
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
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
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

    override fun onDestroyView() {
        tabMediator?.detach()
        tabMediator = null
        super.onDestroyView()
        _binding = null
    }

    companion object {
        const val ARG_TAB = "initial_tab"
        const val ARG_PHOTO_ALREADY_SELECTED = "photo_already_selected"
    }
}

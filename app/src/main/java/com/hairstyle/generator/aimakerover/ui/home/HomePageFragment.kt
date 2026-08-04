package com.hairstyle.generator.aimakerover.ui.home

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.DecelerateInterpolator
import androidx.activity.OnBackPressedCallback
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.hairstyle.generator.aimakeover.R
import com.hairstyle.generator.aimakeover.databinding.FragmentHomePageBinding
import com.hairstyle.generator.aimakerover.Advertisement.ADSMainClass
import com.hairstyle.generator.aimakerover.Advertisement.InterstitialNavHelper
import com.hairstyle.generator.aimakerover.Advertisement.ScreenAdsHelper
import com.hairstyle.generator.aimakerover.Advertisement.ScreenInterAds
import com.hairstyle.generator.aimakerover.billing.BillingManager
import com.hairstyle.generator.aimakerover.ui.common.RateAppDialog
import com.hairstyle.generator.aimakerover.ui.home.beard.BeardFragment
import com.hairstyle.generator.aimakerover.ui.home.`fun`.FunFragment
import com.hairstyle.generator.aimakerover.utils.NavGradientHelper.setNavIcon
import com.hairstyle.generator.aimakerover.utils.NavGradientHelper.setNavLabel
import com.hairstyle.generator.aimakerover.utils.PremiumPlanManager
import com.hairstyle.generator.aimakerover.utils.PromoSheetContent
import com.hairstyle.generator.aimakerover.utils.applyStatusBarPadding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class HomePageFragment : Fragment() {

    private var _binding: FragmentHomePageBinding? = null
    private val binding get() = _binding!!

    @Inject lateinit var billingManager: BillingManager

    private enum class Tab { HOME, FUN, BEARD }

    private var currentTab = Tab.HOME
    private var homeHeaderHidden = false

    private val headerLayoutListener =
        View.OnLayoutChangeListener { _, _, top, _, bottom, _, oldTop, _, oldBottom ->
            if (bottom - top != oldBottom - oldTop) syncContentInsets()
        }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomePageBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.homeHeader.applyStatusBarPadding()
        binding.homeHeader.addOnLayoutChangeListener(headerLayoutListener)
        setupBottomNavigation()
        setupBackPress()
        savedInstanceState?.getString(KEY_CURRENT_TAB)?.let { name ->
            currentTab = runCatching { Tab.valueOf(name) }.getOrElse { Tab.HOME }
        }
        updateTabUi()
        selectTab(currentTab)
        binding.btnSettings.setOnClickListener {
            InterstitialNavHelper.showOnClick(this, ScreenInterAds.settingIcon()) {
                findNavController().navigate(R.id.action_homePage_to_setting)
            }
        }
        binding.btnPro.setOnClickListener {
            findNavController().navigate(R.id.action_homePage_to_premium)
        }
        updateProButtonVisibility()
        observePremiumEntitlement()
        ScreenAdsHelper.load(this, binding.root, ScreenAdsHelper.home())
        InterstitialNavHelper.preload(activity)
    }

    private fun observePremiumEntitlement() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                billingManager.entitlementRevision.collect {
                    updateProButtonVisibility()
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // Re-check Play subscriptions (expired Internal-test subs drop off here).
        billingManager.startConnection()
        updateProButtonVisibility()
        // Fullscreen inter/app-open leave the window with collapsed system-bar insets.
        _binding?.let { ViewCompat.requestApplyInsets(it.root) }
        if (currentTab != Tab.HOME) {
            showHomeHeaderImmediate()
        }
        syncContentInsets()
    }

    private fun updateProButtonVisibility() {
        binding.btnPro.isVisible = !PremiumPlanManager.isPremiumActive()
    }

    /**
     * Home tab only: scroll-driven header hide/show via translation + alpha
     * (no layout height changes → no blink).
     */
    fun setHomeHeaderCollapsed(collapsed: Boolean) {
        if (_binding == null || currentTab != Tab.HOME) return
        if (homeHeaderHidden == collapsed) return
        animateHomeHeader(show = !collapsed)
    }

    private fun showHomeHeaderImmediate() {
        if (_binding == null) return
        binding.homeHeader.animate().cancel()
        homeHeaderHidden = false
        binding.homeHeader.translationY = 0f
        binding.homeHeader.alpha = 1f
        binding.homeHeader.visibility = View.VISIBLE
    }

    private fun animateHomeHeader(show: Boolean) {
        if (_binding == null) return
        val header = binding.homeHeader
        header.animate().cancel()

        if (header.height == 0) {
            header.post { animateHomeHeader(show) }
            return
        }

        homeHeaderHidden = !show
        header.visibility = View.VISIBLE
        val targetY = if (show) 0f else -header.height.toFloat()
        val targetAlpha = if (show) 1f else 0f

        header.animate()
            .translationY(targetY)
            .alpha(targetAlpha)
            .setDuration(HEADER_ANIM_MS)
            .setInterpolator(DecelerateInterpolator())
            .withLayer()
            .start()
    }

    internal fun showSecondVisitPromo(promo: PromoSheetContent) {
        if (!isAdded) return
        if (PremiumPlanManager.isPremiumActive()) return
        if (childFragmentManager.findFragmentByTag(SecondVisitPromoBottomSheet.TAG) != null) return

        SecondVisitPromoBottomSheet.newInstance(
            imageUrl = promo.imageUrl,
            badgeRes = promo.badgeRes,
            title = promo.title,
            subtitle = promo.subtitle,
            startOnPremium = false
        ).show(childFragmentManager, SecondVisitPromoBottomSheet.TAG)
    }

    /** After promo sheet closes — no rate here; rate is shown before promo. */
    internal fun onSecondVisitPromoDismissed() = Unit

    /** Fun / Beard / Home tab became visible — show pending rate prompt on first eligible screen. */
    internal fun maybeShowRateAppPrompt() {
        if (!isAdded) return
        val host = childFragmentManager.findFragmentByTag(tagFor(currentTab)) ?: return
        RateAppDialog.showIfNeededForAppOpen(host)
    }

    internal fun openBeardTab() {
        selectTab(Tab.BEARD)
    }

    internal fun openFunTab() {
        selectTab(Tab.FUN)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putString(KEY_CURRENT_TAB, currentTab.name)
        super.onSaveInstanceState(outState)
    }

    private fun setupBottomNavigation() {
        binding.tabFun.setOnClickListener { onBottomNavClick(Tab.FUN) }
        binding.btnHome.setOnClickListener { onBottomNavClick(Tab.HOME) }
        binding.tabBeard.setOnClickListener { onBottomNavClick(Tab.BEARD) }
    }

    /** System / gesture back — no interstitial ads. */
    private fun setupBackPress() {
        requireActivity().onBackPressedDispatcher.addCallback(
            viewLifecycleOwner,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    when (currentTab) {
                        Tab.FUN, Tab.BEARD -> selectTab(Tab.HOME)
                        Tab.HOME -> handleHomeBackExit()
                    }
                }
            }
        )
    }

    /**
     * Firebase `exit_dialog_show` → [ADSMainClass.getExitAds].
     * true: show Exit dialog; false: finish app directly.
     */
    private fun handleHomeBackExit() {
        if (ADSMainClass.getExitAds()) {
            ExitAppDialog.show(this) {
                requireActivity().finishAffinity()
            }
        } else {
            requireActivity().finishAffinity()
        }
    }

    private fun onBottomNavClick(tab: Tab) {
        if (currentTab == tab && childFragmentManager.findFragmentByTag(tagFor(tab))?.isVisible == true) {
            return
        }
        InterstitialNavHelper.showOnBottomNav(this) {
            // After fullscreen ad, wait one frame so header can lay out before insets.
            if (!isAdded || _binding == null) return@showOnBottomNav
            view?.post {
                if (!isAdded || _binding == null) return@post
                selectTab(tab)
            }
        }
    }

    private fun selectTab(tab: Tab) {
        if (_binding == null || !isAdded) return
        if (currentTab == tab && childFragmentManager.findFragmentByTag(tagFor(tab))?.isVisible == true) {
            // Still re-apply insets (e.g. returning from inter with same tab race).
            if (tab != Tab.HOME) {
                showHomeHeaderImmediate()
                syncContentInsets()
            }
            return
        }

        currentTab = tab
        updateTabUi()
        updateHeaderTitle(tab)
        // Fun/Beard: restore header first, then measure height for top inset.
        if (tab != Tab.HOME) {
            showHomeHeaderImmediate()
        }
        updateHeaderBackground(tab)
        syncContentInsets()

        val targetTag = tagFor(tab)
        val transaction = childFragmentManager.beginTransaction()
        // Only toggle tab fragments — don't hide dialogs/bottom sheets in child FM.
        listOf(TAG_HOME, TAG_FUN, TAG_BEARD).forEach { tag ->
            childFragmentManager.findFragmentByTag(tag)?.let { fragment ->
                if (fragment.isAdded) transaction.hide(fragment)
            }
        }

        var fragment = childFragmentManager.findFragmentByTag(targetTag)
        if (fragment == null) {
            fragment = when (tab) {
                Tab.HOME -> HomeFragment()
                Tab.FUN -> FunFragment()
                Tab.BEARD -> BeardFragment()
            }
            transaction.add(R.id.homeTabContainer, fragment, targetTag)
        } else {
            transaction.show(fragment)
        }
        // Interstitial dismiss often lands after onSaveInstanceState.
        if (childFragmentManager.isStateSaved) {
            transaction.commitAllowingStateLoss()
            childFragmentManager.executePendingTransactions()
        } else {
            transaction.commitNow()
        }
    }

    private fun tagFor(tab: Tab): String = when (tab) {
        Tab.HOME -> TAG_HOME
        Tab.FUN -> TAG_FUN
        Tab.BEARD -> TAG_BEARD
    }

    private fun updateHeaderBackground(tab: Tab) {
        if (_binding == null) return
        binding.homeHeader.setBackgroundColor(
            if (tab == Tab.HOME) {
                Color.TRANSPARENT
            } else {
                ContextCompat.getColor(requireContext(), R.color.background)
            }
        )
    }

    /**
     * Fun/Beard content must sit below [homeHeader]. The header height changes whenever the
     * status bar inset is re-applied (fullscreen inter/app-open ads drop it to 0 on Android 15+),
     * so the top padding is re-synced on every header layout instead of only once.
     */
    private fun syncContentInsets() {
        if (_binding == null) return
        val bottomPadding = resources.getDimensionPixelSize(R.dimen.bottom_nav_content_padding)
        val container = binding.homeTabContainer

        val topPadding = if (currentTab == Tab.HOME) {
            0
        } else {
            binding.homeHeader.height.takeIf { it > 0 } ?: return
        }

        if (container.paddingTop != topPadding || container.paddingBottom != bottomPadding) {
            container.setPadding(0, topPadding, 0, bottomPadding)
        }
    }

    private fun updateHeaderTitle(tab: Tab) {
        binding.tvPageTitle.setText(
            when (tab) {
                Tab.HOME -> R.string.nav_home
                Tab.FUN -> R.string.fun_title
                Tab.BEARD -> R.string.nav_beard
            }
        )
    }

    private fun updateTabUi() {
        binding.ivFun.setNavIcon(currentTab == Tab.FUN, "fun")
        binding.tvFun.setNavLabel(currentTab == Tab.FUN)

        binding.ivBeard.setNavIcon(currentTab == Tab.BEARD, "beard")
        binding.tvBeard.setNavLabel(currentTab == Tab.BEARD)
    }

    override fun onDestroyView() {
        binding.homeHeader.animate().cancel()
        binding.homeHeader.removeOnLayoutChangeListener(headerLayoutListener)
        super.onDestroyView()
        _binding = null
    }

    companion object {
        private const val KEY_CURRENT_TAB = "current_tab"
        private const val TAG_HOME = "tab_home"
        private const val TAG_FUN = "tab_fun"
        private const val TAG_BEARD = "tab_beard"
        private const val HEADER_ANIM_MS = 220L
    }
}

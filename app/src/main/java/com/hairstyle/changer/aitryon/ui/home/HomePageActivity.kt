package com.hairstyle.changer.aitryon.ui.home

import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.view.animation.DecelerateInterpolator
import androidx.activity.OnBackPressedCallback
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.isVisible
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.hairstyle.changer.aitryon.R
import com.hairstyle.changer.aitryon.databinding.FragmentHomePageBinding
import com.hairstyle.changer.aitryon.Ads.ADSMainClass
import com.hairstyle.changer.aitryon.Ads.ADSNativeDisplay
import com.hairstyle.changer.aitryon.Ads.InterstitialNavHelper
import com.hairstyle.changer.aitryon.Ads.ScreenAdsHelper
import com.hairstyle.changer.aitryon.Ads.ScreenInterAds
import com.hairstyle.changer.aitryon.repository.VideosRepository
import com.hairstyle.changer.aitryon.ui.AppNavigator
import com.hairstyle.changer.aitryon.ui.BaseActivity
import com.hairstyle.changer.aitryon.ui.common.RateAppDialog
import com.hairstyle.changer.aitryon.ui.home.beard.BeardFragment
import com.hairstyle.changer.aitryon.ui.home.`fun`.FunFragment
import com.hairstyle.changer.aitryon.utils.NavGradientHelper.setNavIcon
import com.hairstyle.changer.aitryon.utils.NavGradientHelper.setNavLabel
import com.hairstyle.changer.aitryon.utils.applyStatusBarPadding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class HomePageActivity : BaseActivity() {

    override val useLightStatusBarIcons: Boolean = false

    private var _binding: FragmentHomePageBinding? = null
    private val binding get() = _binding!!

    @Inject
    lateinit var videosRepository: VideosRepository

    private enum class Tab { HOME, FUN, BEARD }

    private var currentTab = Tab.HOME
    private var homeHeaderHidden = false
    private val homeNativeReloadHandler = Handler(Looper.getMainLooper())
    private val homeNativeReloadRunnable = Runnable { reloadHomeBottomNativeIfVisible() }

    private val headerLayoutListener =
        View.OnLayoutChangeListener { _, _, top, _, bottom, _, oldTop, _, oldBottom ->
            if (bottom - top != oldBottom - oldTop) syncContentInsets()
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        _binding = FragmentHomePageBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setupWindow(binding.root)

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
                AppNavigator.openSettings(this)
            }
        }
        applyHomeNativeAdsBackground()
        ScreenAdsHelper.load(this, binding.root, ScreenAdsHelper.home())
        applyHomeNativeAdsBackground()
        InterstitialNavHelper.preload(this)
        videosRepository.preloadHomeVideos()
    }

    override fun onResume() {
        super.onResume()
        // Fullscreen inter/app-open leave the window with collapsed system-bar insets.
        _binding?.let { ViewCompat.requestApplyInsets(it.root) }
        if (currentTab != Tab.HOME) {
            showHomeHeaderImmediate()
        }
        syncContentInsets()
        applyHomeNativeAdsBackground()
        scheduleHomeBottomNativeReload()
    }

    override fun onPause() {
        stopHomeBottomNativeReload()
        super.onPause()
    }

    private fun scheduleHomeBottomNativeReload() {
        stopHomeBottomNativeReload()
        if (!ADSMainClass.getNativeAdsReloading()) return
        val seconds = ADSMainClass.getNativeAdsReloadSeconds()
        if (seconds <= 0) return
        if (normalizeHomeAdsType() != "native") return
        homeNativeReloadHandler.postDelayed(homeNativeReloadRunnable, seconds * 1000L)
    }

    private fun stopHomeBottomNativeReload() {
        homeNativeReloadHandler.removeCallbacks(homeNativeReloadRunnable)
    }

    private fun reloadHomeBottomNativeIfVisible() {
        if (!isScreenAlive() || _binding == null) return
        if (!lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) return
        // Load (shimmer) → Show when ready → then start next N-second timer
        ScreenAdsHelper.reloadHomeBottomNative(this, binding.root) {
            if (!isScreenAlive()) return@reloadHomeBottomNative
            if (!lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) return@reloadHomeBottomNative
            scheduleHomeBottomNativeReload()
        }
    }

    private fun normalizeHomeAdsType(): String {
        return when (ADSMainClass.getHomeScreenAdsType().trim().lowercase()) {
            "banner" -> "banner"
            else -> "native"
        }
    }

    /** Home only: #4E4C4C on bar + CardView + Native body + shimmer (when Native is active). */
    private fun applyHomeNativeAdsBackground() {
        if (_binding == null) return
        val adsVisible = binding.root.findViewById<View>(R.id.screenAdContainer)
            ?.isVisible == true
        val useNativeBg = adsVisible && normalizeHomeAdsType() == "native"
        if (useNativeBg) {
            val bg = ContextCompat.getColor(this, R.color.home_native_ads_background)
            val shimmer = ContextCompat.getColor(this, R.color.home_native_ads_shimmer)
            ADSNativeDisplay.setHomeNativeBackgroundOverride(bg, shimmer)
            binding.homeBottomAdsBar.setBackgroundColor(bg)
            binding.root.findViewById<View>(R.id.screenAdContainer)?.setBackgroundColor(bg)
            binding.root.findViewById<androidx.cardview.widget.CardView>(R.id.cardNativeAd)?.apply {
                setCardBackgroundColor(bg)
                // Avoid a darker “floating” card on the same strip.
                radius = 0f
            }
            binding.root.findViewById<com.facebook.shimmer.ShimmerFrameLayout>(R.id.shimmerScreenAd)
                ?.setBackgroundColor(bg)
        } else {
            ADSNativeDisplay.setHomeNativeBackgroundOverride(null, null)
            val fallback = ContextCompat.getColor(this, R.color.nav_bar_background)
            binding.homeBottomAdsBar.setBackgroundColor(fallback)
            binding.root.findViewById<View>(R.id.screenAdContainer)?.setBackgroundColor(Color.TRANSPARENT)
            binding.root.findViewById<androidx.cardview.widget.CardView>(R.id.cardNativeAd)?.apply {
                setCardBackgroundColor(fallback)
                radius = resources.getDimension(com.intuit.sdp.R.dimen._12sdp)
            }
        }
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

    /** Fun / Beard / Home tab became visible — show pending rate prompt on first eligible screen. */
    internal fun maybeShowRateAppPrompt() {
        if (!isScreenAlive()) return
        RateAppDialog.showIfNeededForAppOpen(this)
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
        onBackPressedDispatcher.addCallback(
            this,
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
                finishAffinity()
            }
        } else {
            finishAffinity()
        }
    }

    private fun onBottomNavClick(tab: Tab) {
        if (currentTab == tab && supportFragmentManager.findFragmentByTag(tagFor(tab))?.isVisible == true) {
            return
        }
        InterstitialNavHelper.showOnBottomNav(this) {
            // After fullscreen ad, wait one frame so header can lay out before insets.
            if (!isScreenAlive() || _binding == null) return@showOnBottomNav
            binding.root.post {
                if (!isScreenAlive() || _binding == null) return@post
                selectTab(tab)
            }
        }
    }

    private fun selectTab(tab: Tab) {
        if (_binding == null || !isScreenAlive()) return
        if (currentTab == tab && supportFragmentManager.findFragmentByTag(tagFor(tab))?.isVisible == true) {
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
        val transaction = supportFragmentManager.beginTransaction()
        // Only toggle tab fragments — don't hide dialogs/bottom sheets in FM.
        listOf(TAG_HOME, TAG_FUN, TAG_BEARD).forEach { tag ->
            supportFragmentManager.findFragmentByTag(tag)?.let { fragment ->
                if (fragment.isAdded) transaction.hide(fragment)
            }
        }

        var fragment = supportFragmentManager.findFragmentByTag(targetTag)
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
        if (supportFragmentManager.isStateSaved) {
            transaction.commitAllowingStateLoss()
            supportFragmentManager.executePendingTransactions()
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
                ContextCompat.getColor(this, R.color.background)
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

    override fun onDestroy() {
        stopHomeBottomNativeReload()
        ADSNativeDisplay.setHomeNativeBackgroundOverride(null, null)
        _binding?.homeHeader?.animate()?.cancel()
        _binding?.homeHeader?.removeOnLayoutChangeListener(headerLayoutListener)
        _binding = null
        super.onDestroy()
    }

    companion object {
        private const val KEY_CURRENT_TAB = "current_tab"
        private const val TAG_HOME = "tab_home"
        private const val TAG_FUN = "tab_fun"
        private const val TAG_BEARD = "tab_beard"
        private const val HEADER_ANIM_MS = 220L
    }
}

package com.hairstyle.changer.aitryon.ui.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.ConcatAdapter
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import com.hairstyle.changer.aitryon.R
import com.hairstyle.changer.aitryon.databinding.FragmentHomeBinding
import com.hairstyle.changer.aitryon.domain.model.CategoryHairstyle
import com.hairstyle.changer.aitryon.domain.model.HomeHeroSlide
import com.hairstyle.changer.aitryon.Ads.InterstitialNavHelper
import com.hairstyle.changer.aitryon.Ads.ScreenInterAds
import com.hairstyle.changer.aitryon.remoteconfig.InAppUpdateConfig
import com.hairstyle.changer.aitryon.remoteconfig.RemoteConfigManager
import com.hairstyle.changer.aitryon.ui.AppNavigator
import com.hairstyle.changer.aitryon.ui.category.HairColorBottomSheet
import com.facebook.shimmer.ShimmerFrameLayout
import com.hairstyle.changer.aitryon.ui.common.RateAppDialog
import com.hairstyle.changer.aitryon.ui.common.ShimmerItemFooterAdapter
import com.hairstyle.changer.aitryon.utils.HomeCategoryVideoHelper
import com.hairstyle.changer.aitryon.utils.InAppUpdateHelper
import com.hairstyle.changer.aitryon.utils.VideoTitleKeys
import com.hairstyle.changer.aitryon.utils.bindListLoading
import com.hairstyle.changer.aitryon.utils.showListShimmer
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlin.math.abs
import okhttp3.OkHttpClient
import javax.inject.Inject

@AndroidEntryPoint
class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    private val viewModel: HomeViewModel by viewModels()
    private lateinit var menAdapter: HomeHairstyleAdapter
    private lateinit var womenAdapter: HomeHairstyleAdapter
    private lateinit var childrenAdapter: HomeHairstyleAdapter
    private lateinit var menShimmerFooter: ShimmerItemFooterAdapter
    private lateinit var womenShimmerFooter: ShimmerItemFooterAdapter
    private lateinit var childrenShimmerFooter: ShimmerItemFooterAdapter

    private lateinit var heroAdapter: HomeHeroPagerAdapter
    private var heroAutoScroller: HomeHeroAutoScroller? = null
    private var heroPageCallback: ViewPager2.OnPageChangeCallback? = null
    private var heroSlides: List<HomeHeroSlide> = emptyList()

    @Inject lateinit var okHttpClient: OkHttpClient
    @Inject lateinit var remoteConfigManager: RemoteConfigManager

    private var cachedUpdateConfig: InAppUpdateConfig = InAppUpdateConfig.DISABLED

    private val inAppUpdateLauncher = registerForActivityResult(
        ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        InAppUpdateHelper.onUpdateFlowResult(
            resultCode = result.resultCode,
            config = cachedUpdateConfig,
            onFinished = {
                RateAppDialog.showIfNeededForAppOpen(this)
            }
        )
    }

    private var categoryVideoHandles: List<HomeCategoryVideoHelper.Handle> = emptyList()
    private val categoryVideoKeys = listOf(
        VideoTitleKeys.HOME_VIDEO_1,
        VideoTitleKeys.HOME_VIDEO_2,
        VideoTitleKeys.HOME_VIDEO_3
    )

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        menAdapter = HomeHairstyleAdapter(::onHairstyleSelected)
        womenAdapter = HomeHairstyleAdapter(::onHairstyleSelected)
        childrenAdapter = HomeHairstyleAdapter(::onHairstyleSelected)
        menShimmerFooter = ShimmerItemFooterAdapter(R.layout.item_shimmer_home, shimmerItemCount = 2)
        womenShimmerFooter = ShimmerItemFooterAdapter(R.layout.item_shimmer_home, shimmerItemCount = 2)
        childrenShimmerFooter = ShimmerItemFooterAdapter(R.layout.item_shimmer_home, shimmerItemCount = 2)

        // Show row shimmer immediately until first data arrives
        showInitialRowShimmer(binding.shimmerMenHairstyles, binding.rvMenHairstyles)
        showInitialRowShimmer(binding.shimmerWomenHairstyles, binding.rvWomenHairstyles)
        showInitialRowShimmer(binding.shimmerChildrenHairstyles, binding.rvChildrenHairstyles)

        setupHeroCarousel()
        setupCategoryVideos()

        setupHairstyleRow(
            recyclerView = binding.rvMenHairstyles,
            concatAdapter = ConcatAdapter(menAdapter, menShimmerFooter),
            loadMore = viewModel::loadMoreMenHairstyles
        )
        setupHairstyleRow(
            recyclerView = binding.rvWomenHairstyles,
            concatAdapter = ConcatAdapter(womenAdapter, womenShimmerFooter),
            loadMore = viewModel::loadMoreWomenHairstyles
        )
        setupHairstyleRow(
            recyclerView = binding.rvChildrenHairstyles,
            concatAdapter = ConcatAdapter(childrenAdapter, childrenShimmerFooter),
            loadMore = viewModel::loadMoreChildrenHairstyles
        )

        binding.btnUploadPhoto.setOnClickListener {
            // Upload Your Photo: pick photo → Select Category → hair color → generate.
            InterstitialNavHelper.showOnClick(this, ScreenInterAds.uploadPhoto()) {
                val host = activity ?: return@showOnClick
                AppNavigator.openPhotoSelect(host, photoFirstFlow = true)
            }
        }

        binding.btnSeeAllMen.setOnClickListener { openSelectCategory(tab = 0, interEnabled = ScreenInterAds.seeAll()) }
        binding.btnSeeAllWomen.setOnClickListener { openSelectCategory(tab = 1, interEnabled = ScreenInterAds.seeAll()) }
        binding.btnSeeAllChildren.setOnClickListener { openSelectCategory(tab = 2, interEnabled = ScreenInterAds.seeAll()) }

        observeHairstyles(
            viewModel.menHairstyles,
            menAdapter,
            menShimmerFooter,
            binding.shimmerMenHairstyles,
            binding.rvMenHairstyles
        )
        observeHairstyles(
            viewModel.womenHairstyles,
            womenAdapter,
            womenShimmerFooter,
            binding.shimmerWomenHairstyles,
            binding.rvWomenHairstyles
        )
        observeHairstyles(
            viewModel.childrenHairstyles,
            childrenAdapter,
            childrenShimmerFooter,
            binding.shimmerChildrenHairstyles,
            binding.rvChildrenHairstyles
        )

        setupHomeHeaderScrollBehavior()
    }

    /**
     * Scroll down → hide title/Pro/Settings. Scroll up (or top) → show with smooth animation.
     * Only affects Home tab header on [HomePageActivity]; no layout reflow (no blink).
     */
    private fun setupHomeHeaderScrollBehavior() {
        var accumulatedDy = 0
        binding.root.setOnScrollChangeListener { _, _, scrollY, _, oldScrollY ->
            val homePage = activity as? HomePageActivity ?: return@setOnScrollChangeListener
            val dy = scrollY - oldScrollY

            if (scrollY <= 0) {
                accumulatedDy = 0
                homePage.setHomeHeaderCollapsed(false)
                return@setOnScrollChangeListener
            }

            accumulatedDy += dy
            when {
                accumulatedDy > HEADER_SCROLL_THRESHOLD -> {
                    accumulatedDy = 0
                    homePage.setHomeHeaderCollapsed(true)
                }
                accumulatedDy < -HEADER_SCROLL_THRESHOLD -> {
                    accumulatedDy = 0
                    homePage.setHomeHeaderCollapsed(false)
                }
            }
        }
    }

    private fun showInitialRowShimmer(shimmer: ShimmerFrameLayout, content: View) {
        content.isVisible = false
        shimmer.showListShimmer()
    }

    override fun onResume() {
        super.onResume()
        viewModel.loadHeroSlides()
        refreshVisibleImages()
        if (!isHidden) {
            playCategoryVideos()
            maybeShowUpdate()
        }
    }

    override fun onHiddenChanged(hidden: Boolean) {
        super.onHiddenChanged(hidden)
        if (!hidden) {
            viewModel.loadHeroSlides()
            refreshVisibleImages()
            playCategoryVideos()
            maybeShowUpdate()
            // Returning to Home: sync header with current scroll (no blink).
            if (_binding != null) {
                (activity as? HomePageActivity)?.setHomeHeaderCollapsed(
                    binding.root.scrollY > HEADER_SCROLL_THRESHOLD
                )
            }
        } else {
            pauseCategoryVideos()
        }
    }

    override fun onPause() {
        pauseCategoryVideos()
        super.onPause()
    }

    private fun refreshVisibleImages() {
        if (_binding == null) return
        if (::menAdapter.isInitialized) {
            listOf(menAdapter, womenAdapter, childrenAdapter).forEach { adapter ->
                if (adapter.itemCount > 0) {
                    adapter.notifyItemRangeChanged(0, adapter.itemCount)
                }
            }
        }
        if (::heroAdapter.isInitialized && heroSlides.isNotEmpty()) {
            val slides = heroSlides.toList()
            heroAdapter.submitList(emptyList()) {
                heroAdapter.submitList(slides)
            }
        }
    }

    private fun setupCategoryVideos() {
        val cards = listOf(
            binding.categoryVideoBeard to R.string.nav_beard,
            binding.categoryVideoHairstyle to R.string.home_category_hairstyle,
            binding.categoryVideoFun to R.string.fun_title
        )

        categoryVideoHandles = cards.mapIndexed { index, (card, titleRes) ->
            card.tvCategoryTitle.setText(titleRes)
            HomeCategoryVideoHelper.Handle(
                webpView = card.ivCategoryVideo,
                shimmerLayout = card.shimmerCategoryVideo
            ).also { handle ->
                val videoKey = categoryVideoKeys[index]
                viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
                    handle.preload(requireContext().applicationContext, okHttpClient, videoKey)
                }
            }
        }

        binding.categoryVideoBeard.root.setOnClickListener {
            (activity as? HomePageActivity)?.openBeardTab()
        }
        binding.categoryVideoHairstyle.root.setOnClickListener {
            openSelectCategory(tab = 0, interEnabled = ScreenInterAds.tryIt())
        }
        binding.categoryVideoFun.root.setOnClickListener {
            (activity as? HomePageActivity)?.openFunTab()
        }
    }

    private fun playCategoryVideos() {
        categoryVideoHandles.forEachIndexed { index, handle ->
            handle.play(requireContext(), categoryVideoKeys[index])
        }
    }

    private fun pauseCategoryVideos() {
        categoryVideoHandles.forEach { it.pause() }
    }

    private fun releaseCategoryVideos() {
        categoryVideoHandles.forEach { it.release() }
        categoryVideoHandles = emptyList()
    }

    private fun setupHeroCarousel() {
        binding.btnHeroTryIt.setOnClickListener {
            openSelectCategory(tab = 0, interEnabled = ScreenInterAds.tryIt())
        }

        heroAdapter = HomeHeroPagerAdapter {
            openSelectCategory(tab = 0, interEnabled = ScreenInterAds.tryIt())
        }
        binding.heroViewPager.adapter = heroAdapter
        binding.heroViewPager.offscreenPageLimit = 1
        enableSmoothHeroSwipe()

        heroPageCallback = object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                updateHeroTitle(position)
                binding.heroDotsIndicator.setSelectedIndex(position)
            }
        }
        binding.heroViewPager.registerOnPageChangeCallback(heroPageCallback!!)
        heroAutoScroller = HomeHeroAutoScroller(binding.heroViewPager, viewLifecycleOwner)

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.heroSlides.collect { state ->
                    heroSlides = state.slides.ifEmpty { fallbackHeroSlides() }
                    heroAdapter.submitList(heroSlides) {
                        binding.heroDotsIndicator.setDotCount(heroSlides.size)
                        binding.heroViewPager.setCurrentItem(0, false)
                        updateHeroTitle(0)
                        binding.heroDotsIndicator.setSelectedIndex(0, animate = false)
                        heroAutoScroller?.start(heroSlides.size)
                    }
                }
            }
        }
    }

    private fun enableSmoothHeroSwipe() {
        (binding.heroViewPager.getChildAt(0) as? RecyclerView)?.apply {
            overScrollMode = View.OVER_SCROLL_NEVER
        }

        var downX = 0f
        var downY = 0f
        binding.heroViewPager.setOnTouchListener { view, event ->
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    downX = event.x
                    downY = event.y
                    view.parent.requestDisallowInterceptTouchEvent(true)
                }
                MotionEvent.ACTION_MOVE -> {
                    val dx = abs(event.x - downX)
                    val dy = abs(event.y - downY)
                    view.parent.requestDisallowInterceptTouchEvent(dx > dy)
                }
            }
            false
        }
    }

    private fun fallbackHeroSlides(): List<HomeHeroSlide> {
        return listOf(
            HomeHeroSlide(id = 0, imageUrl = "", title = getString(R.string.nav_home))
        )
    }

    private fun heroDisplayTitle(index: Int, slide: HomeHeroSlide): String {
        val titles = resources.getStringArray(R.array.home_hero_titles)
        return titles.getOrNull(index) ?: slide.title.ifBlank { getString(R.string.nav_home) }
    }

    private fun updateHeroTitle(position: Int) {
        if (heroSlides.isEmpty()) return
        val safeIndex = position.coerceIn(0, heroSlides.lastIndex)
        binding.tvHeroTitle.text = heroDisplayTitle(safeIndex, heroSlides[safeIndex])
    }

    private fun setupHairstyleRow(
        recyclerView: RecyclerView,
        concatAdapter: ConcatAdapter,
        loadMore: () -> Unit
    ) {
        val layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
        recyclerView.layoutManager = layoutManager
        recyclerView.adapter = concatAdapter
        recyclerView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                if (dx <= 0) return
                val lastVisible = layoutManager.findLastVisibleItemPosition()
                val total = concatAdapter.itemCount
                if (total > 0 && lastVisible >= total - 3) {
                    loadMore()
                }
            }
        })
    }

    private fun observeHairstyles(
        stateFlow: StateFlow<HomeHairstyleUiState>,
        adapter: HomeHairstyleAdapter,
        shimmerFooter: ShimmerItemFooterAdapter,
        shimmerView: ShimmerFrameLayout,
        contentView: RecyclerView
    ) {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                stateFlow.collect { state ->
                    val hasItems = state.items.isNotEmpty()
                    val hasError = state.error != null
                    shimmerView.bindListLoading(
                        isLoading = state.isLoading,
                        hasItems = hasItems,
                        hasError = hasError,
                        contentView = contentView
                    )
                    shimmerFooter.bindLoadingMore(
                        loadingMore = state.isLoadingMore,
                        hasItems = hasItems,
                        hasMore = state.hasMore
                    )
                    adapter.submitList(state.items)
                }
            }
        }
    }

    /**
     * In-app update (Firebase-gated), then the app-open rate prompt once the update UI settles.
     */
    private fun maybeShowUpdate() {
        if (_binding == null || isHidden || !isAdded) return
        cachedUpdateConfig = remoteConfigManager.getInAppUpdateConfig()
        InAppUpdateHelper.checkAndMaybeStart(
            fragment = this,
            updateLauncher = inAppUpdateLauncher,
            config = cachedUpdateConfig,
            onFinished = {
                RateAppDialog.showIfNeededForAppOpen(this)
            }
        )
    }

    private fun onHairstyleSelected(item: CategoryHairstyle) {
        HairColorBottomSheet.newInstance(
            previewImageUrl = item.imageUrl,
            hairstyleTitle = item.title,
            hairstyleApiValue = item.apiValue,
            source = HairColorBottomSheet.Source.HOME
        ).show(parentFragmentManager, HairColorBottomSheet.TAG)
    }

    private fun openSelectCategory(tab: Int, interEnabled: Boolean) {
        InterstitialNavHelper.showOnClick(this, interEnabled) {
            val host = activity ?: return@showOnClick
            AppNavigator.openSelectCategory(host, tab = tab)
        }
    }

    override fun onDestroyView() {
        releaseCategoryVideos()
        heroAutoScroller?.stop()
        heroPageCallback?.let { binding.heroViewPager.unregisterOnPageChangeCallback(it) }
        heroPageCallback = null
        heroAutoScroller = null
        super.onDestroyView()
        _binding = null
    }

    companion object {
        private const val HEADER_SCROLL_THRESHOLD = 12
    }
}

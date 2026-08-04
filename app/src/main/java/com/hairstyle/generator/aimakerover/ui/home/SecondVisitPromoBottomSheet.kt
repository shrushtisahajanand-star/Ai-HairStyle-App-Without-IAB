package com.hairstyle.generator.aimakerover.ui.home

import android.app.Dialog
import android.content.DialogInterface
import android.graphics.Outline
import android.os.Bundle
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewOutlineProvider
import androidx.core.os.bundleOf
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.viewpager2.widget.ViewPager2
import coil.load
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.hairstyle.generator.aimakeover.R
import com.hairstyle.generator.aimakeover.databinding.BottomSheetSecondVisitPromoBinding
import com.hairstyle.generator.aimakerover.data.remoteconfig.RemoteConfigManager
import com.hairstyle.generator.aimakerover.ui.premium.PremiumViewModel
import com.hairstyle.generator.aimakerover.utils.NavGradientHelper.applyBrandTextGradient
import com.hairstyle.generator.aimakerover.utils.PromoPremiumMediaHelper
import com.hairstyle.generator.aimakerover.utils.VideoTitleKeys
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import javax.inject.Inject

@AndroidEntryPoint
class SecondVisitPromoBottomSheet : BottomSheetDialogFragment() {

    private var _binding: BottomSheetSecondVisitPromoBinding? = null
    private val binding get() = _binding!!

    private val viewModel: PremiumViewModel by viewModels()

    @Inject lateinit var okHttpClient: OkHttpClient
    @Inject lateinit var remoteConfigManager: RemoteConfigManager

    private lateinit var pagerAdapter: PromoFlowPagerAdapter
    private var premiumController: PromoPremiumPaywallController? = null
    private var bottomSheetBehavior: BottomSheetBehavior<View>? = null

    private var premiumPageShown = false

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = BottomSheetSecondVisitPromoBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        premiumPageShown = savedInstanceState?.getBoolean(KEY_PREMIUM_SHOWN) == true ||
            arguments?.getBoolean(ARG_START_ON_PREMIUM) == true

        pagerAdapter = PromoFlowPagerAdapter()
        binding.promoPager.apply {
            adapter = pagerAdapter
            isUserInputEnabled = false
            offscreenPageLimit = 1
            registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
                override fun onPageSelected(position: Int) {
                    if (position == PromoFlowPagerAdapter.PAGE_PREMIUM) {
                        onPremiumPageVisible()
                    }
                }
            })
        }

        binding.promoPager.post {
            ensurePagesReady {
                setupPromoPage()
                setupPremiumPage()
                if (premiumPageShown) {
                    binding.promoPager.setCurrentItem(PromoFlowPagerAdapter.PAGE_PREMIUM, false)
                    updateSheetHeight(PREMIUM_SHEET_HEIGHT_FRACTION)
                    onPremiumPageVisible()
                }
            }
        }

        preloadPremiumMedia()
    }

    private fun ensurePagesReady(onReady: () -> Unit) {
        if (pagerAdapter.promoBinding != null && pagerAdapter.premiumBinding != null) {
            onReady()
            return
        }
        binding.promoPager.post { ensurePagesReady(onReady) }
    }

    private fun setupPromoPage() {
        val promoBinding = pagerAdapter.promoBinding ?: return
        val imageUrl = arguments?.getString(ARG_IMAGE_URL).orEmpty()
        val badgeRes = arguments?.getInt(ARG_BADGE_RES) ?: R.string.promo_sheet_badge_female
        val title = arguments?.getString(ARG_TITLE).orEmpty()
        val subtitle = arguments?.getString(ARG_SUBTITLE).orEmpty()

        promoBinding.root.post { applyTopRoundedClip(promoBinding.root) }

        promoBinding.ivPromoImage.load(imageUrl) {
            crossfade(true)
        }
        promoBinding.tvBadge.text = getString(R.string.promo_sheet_sparkle_prefix, getString(badgeRes))
        promoBinding.tvBadge.applyBrandTextGradient()
        promoBinding.tvTitle.text = title
        promoBinding.tvSubtitle.text = subtitle

        promoBinding.btnTryNow.setOnClickListener { showPremiumPage() }
        promoBinding.btnClose.setOnClickListener { showPremiumPage() }
    }

    private fun setupPremiumPage() {
        val premiumBinding = pagerAdapter.premiumBinding ?: return
        premiumController = PromoPremiumPaywallController(
            binding = premiumBinding,
            fragment = this,
            viewModel = viewModel,
            remoteConfigManager = remoteConfigManager,
            okHttpClient = okHttpClient,
            onClose = { dismissSheet() },
            onPurchaseSuccess = { dismissSheet() }
        ).also { controller ->
            controller.bindIfNeeded()
            premiumBinding.root.post { applyTopRoundedClip(premiumBinding.root) }
        }
    }

    private fun showPremiumPage() {
        if (premiumPageShown) return
        premiumPageShown = true
        binding.promoPager.setCurrentItem(PromoFlowPagerAdapter.PAGE_PREMIUM, true)
        updateSheetHeight(PREMIUM_SHEET_HEIGHT_FRACTION)
    }

    private fun onPremiumPageVisible() {
        premiumController?.startVideoIfNeeded()
        bottomSheetBehavior?.apply {
            isDraggable = false
            isHideable = true
        }
    }

    private fun dismissSheet() {
        dismissAllowingStateLoss()
    }

    private fun preloadPremiumMedia() {
        lifecycleScope.launch(Dispatchers.IO) {
            PromoPremiumMediaHelper.preload(
                requireContext().applicationContext,
                okHttpClient,
                VideoTitleKeys.PREMIUM_INTRO
            )
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putBoolean(KEY_PREMIUM_SHOWN, premiumPageShown)
        super.onSaveInstanceState(outState)
    }

    override fun onDismiss(dialog: DialogInterface) {
        super.onDismiss(dialog)
        (parentFragment as? HomePageFragment)?.onSecondVisitPromoDismissed()
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return object : BottomSheetDialog(requireContext(), theme) {
            @Deprecated("Deprecated in Java")
            override fun onBackPressed() {
                if (premiumPageShown) {
                    dismissSheet()
                }
            }
        }.apply {
            setCanceledOnTouchOutside(false)
            setCancelable(false)
            setOnKeyListener { _, keyCode, event ->
                keyCode == KeyEvent.KEYCODE_BACK &&
                    event.action == KeyEvent.ACTION_UP &&
                    !premiumPageShown
            }
            setOnShowListener { dialogInterface ->
                val bottomSheet = (dialogInterface as BottomSheetDialog)
                    .findViewById<View>(com.google.android.material.R.id.design_bottom_sheet)
                    ?: return@setOnShowListener

                val fraction = if (premiumPageShown) {
                    PREMIUM_SHEET_HEIGHT_FRACTION
                } else {
                    PROMO_SHEET_HEIGHT_FRACTION
                }
                applySheetSize(bottomSheet, fraction)
            }
        }
    }

    private fun applySheetSize(bottomSheet: View, heightFraction: Float) {
        val sheetHeight = (resources.displayMetrics.heightPixels * heightFraction).toInt()
        bottomSheet.layoutParams.height = sheetHeight
        bottomSheet.setBackgroundResource(R.drawable.bg_photo_guide_bottom_sheet)
        applyTopRoundedClip(bottomSheet)

        bottomSheetBehavior = BottomSheetBehavior.from(bottomSheet).apply {
            peekHeight = sheetHeight
            maxHeight = sheetHeight
            state = BottomSheetBehavior.STATE_EXPANDED
            skipCollapsed = true
            isDraggable = false
            isHideable = false
        }
    }

    private fun updateSheetHeight(heightFraction: Float) {
        val bottomSheet = dialog?.findViewById<View>(
            com.google.android.material.R.id.design_bottom_sheet
        ) ?: return
        applySheetSize(bottomSheet, heightFraction)
    }

    private fun applyTopRoundedClip(view: View) {
        val radius = resources.getDimension(com.intuit.sdp.R.dimen._24sdp)
        view.outlineProvider = object : ViewOutlineProvider() {
            override fun getOutline(outlineView: View, outline: Outline) {
                outline.setRoundRect(
                    0,
                    0,
                    outlineView.width,
                    outlineView.height + radius.toInt(),
                    radius
                )
            }
        }
        view.clipToOutline = true
    }

    override fun onPause() {
        premiumController?.pauseVideo()
        super.onPause()
    }

    override fun onDestroyView() {
        premiumController?.releaseVideo()
        premiumController = null
        bottomSheetBehavior = null
        super.onDestroyView()
        _binding = null
    }

    companion object {
        private const val PROMO_SHEET_HEIGHT_FRACTION = 0.9f
        private const val PREMIUM_SHEET_HEIGHT_FRACTION = 0.9f
        private const val KEY_PREMIUM_SHOWN = "premium_page_shown"

        const val TAG = "SecondVisitPromoBottomSheet"
        const val RESULT_KEY = "second_visit_promo_result"

        private const val ARG_IMAGE_URL = "image_url"
        private const val ARG_BADGE_RES = "badge_res"
        private const val ARG_TITLE = "title"
        private const val ARG_SUBTITLE = "subtitle"
        private const val ARG_START_ON_PREMIUM = "start_on_premium"

        fun newInstance(
            imageUrl: String,
            badgeRes: Int,
            title: String,
            subtitle: String,
            startOnPremium: Boolean = false
        ): SecondVisitPromoBottomSheet {
            return SecondVisitPromoBottomSheet().apply {
                arguments = bundleOf(
                    ARG_IMAGE_URL to imageUrl,
                    ARG_BADGE_RES to badgeRes,
                    ARG_TITLE to title,
                    ARG_SUBTITLE to subtitle,
                    ARG_START_ON_PREMIUM to startOnPremium
                )
            }
        }
    }
}

package com.hairstyle.generator.aimakerover.ui

import android.content.Context
import android.graphics.Rect
import android.os.Bundle
import android.view.MotionEvent
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import androidx.appcompat.app.AppCompatActivity
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.NavHostFragment
import com.hairstyle.generator.aimakeover.R
import com.hairstyle.generator.aimakeover.databinding.ActivityMainBinding
import com.hairstyle.generator.aimakerover.billing.BillingManager
import com.hairstyle.generator.aimakerover.ui.common.NoInternetDialogController
import com.hairstyle.generator.aimakerover.utils.LocaleManager
import com.hairstyle.generator.aimakerover.utils.NetworkMonitor
import com.hairstyle.generator.aimakerover.utils.PrefManager
import com.hairstyle.generator.aimakerover.utils.setStatusBarLightIcons
import com.hairstyle.generator.aimakerover.utils.setupEdgeToEdge
import com.hairstyle.generator.aimakerover.utils.stableNavigationBarBottom
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    @Inject
    lateinit var billingManager: BillingManager

    @Inject
    lateinit var networkMonitor: NetworkMonitor

    private lateinit var binding: ActivityMainBinding
    private var currentDestinationId = R.id.splashFragment
    private var noInternetDialog: NoInternetDialogController? = null

    override fun attachBaseContext(newBase: Context) {
        PrefManager.init(newBase)
        super.attachBaseContext(LocaleManager.wrap(newBase))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupEdgeToEdge(binding.navHostFragment)
        setupNavHostInsetsForDestination()
        setupStatusBarForDestination()
        billingManager.startConnection()

        noInternetDialog = NoInternetDialogController(this, networkMonitor)
        networkMonitor.start()
        observeNetwork()
    }

    private fun observeNetwork() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                // Immediate check when entering STARTED (covers Home already open)
                networkMonitor.refresh()
                networkMonitor.isOnline.collect { online ->
                    noInternetDialog?.onConnectivityChanged(online)
                }
            }
        }
    }

    /** Tap anywhere outside the focused input → dismiss the keyboard. */
    override fun dispatchTouchEvent(ev: MotionEvent): Boolean {
        if (ev.action == MotionEvent.ACTION_DOWN) {
            val focused = currentFocus
            if (focused is EditText) {
                val bounds = Rect()
                focused.getGlobalVisibleRect(bounds)
                if (!bounds.contains(ev.rawX.toInt(), ev.rawY.toInt())) {
                    focused.clearFocus()
                    getSystemService(InputMethodManager::class.java)
                        ?.hideSoftInputFromWindow(focused.windowToken, 0)
                }
            }
        }
        return super.dispatchTouchEvent(ev)
    }

    override fun onStart() {
        super.onStart()
        networkMonitor.start()
        networkMonitor.refresh()
        noInternetDialog?.onConnectivityChanged(networkMonitor.isOnline.value)
    }

    override fun onDestroy() {
        noInternetDialog?.release()
        noInternetDialog = null
        if (isFinishing) {
            networkMonitor.stop()
        }
        super.onDestroy()
    }

    private fun setupNavHostInsetsForDestination() {
        ViewCompat.setOnApplyWindowInsetsListener(binding.navHostFragment) { view, insets ->
            // decorFitsSystemWindows=false disables adjustResize, so the IME inset has to be
            // applied by hand — otherwise the keyboard covers the content instead of shrinking it.
            val imeBottom = insets.getInsets(WindowInsetsCompat.Type.ime()).bottom
            val bottomPadding = when {
                currentDestinationId == R.id.splashFragment -> 0
                else -> maxOf(imeBottom, insets.stableNavigationBarBottom())
            }
            view.updatePadding(bottom = bottomPadding)
            insets
        }
        ViewCompat.requestApplyInsets(binding.navHostFragment)
    }

    private fun setupStatusBarForDestination() {
        val navHostFragment =
            supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        val navController = navHostFragment.navController

        navController.addOnDestinationChangedListener { _, destination, _ ->
            currentDestinationId = destination.id
            ViewCompat.requestApplyInsets(binding.navHostFragment)

            val useLightStatusBarIcons = when (destination.id) {
                R.id.homePageFragment,
                R.id.photoSelectFragment,
                R.id.galleryPhotoSelectionFragment,
                R.id.progressFragment,
                R.id.settingFragment,
                R.id.activeSubscriptionFragment,
                R.id.premiumFragment,
                R.id.userCreationFragment,
                R.id.userCreationPreviewFragment,
                R.id.feedbackFragment,
                R.id.resultFragment,
                R.id.selectCategoryFragment,
                R.id.languageFragment,
                R.id.intro2Fragment,
                R.id.intro3Fragment,
                R.id.splashFragment -> false

                else -> true
            }
            setStatusBarLightIcons(useLightStatusBarIcons)
        }
    }
}

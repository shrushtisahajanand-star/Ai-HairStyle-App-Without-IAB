package com.hairstyle.changer.aitryon.ui

import android.content.Context
import android.graphics.Rect
import android.os.Bundle
import android.view.MotionEvent
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import androidx.appcompat.app.AppCompatActivity
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelLazy
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.hairstyle.changer.aitryon.ui.common.NoInternetDialogController
import com.hairstyle.changer.aitryon.utils.DeviceIdProvider
import com.hairstyle.changer.aitryon.utils.LocaleManager
import com.hairstyle.changer.aitryon.utils.NetworkMonitor
import com.hairstyle.changer.aitryon.utils.PrefManager
import com.hairstyle.changer.aitryon.utils.contentBottomInsetOrFallback
import com.hairstyle.changer.aitryon.utils.setStatusBarLightIcons
import com.hairstyle.changer.aitryon.utils.setupEdgeToEdge
import kotlinx.coroutines.launch
import javax.inject.Inject

abstract class BaseActivity : AppCompatActivity() {

    @Inject
    lateinit var networkMonitor: NetworkMonitor

    private var noInternetDialog: NoInternetDialogController? = null

    protected open val useLightStatusBarIcons: Boolean = true
    protected open val isSplashScreen: Boolean = false

    override fun attachBaseContext(newBase: Context) {
        PrefManager.init(newBase)
        super.attachBaseContext(LocaleManager.wrap(newBase))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        if (isSplashScreen) {
            installSplashScreen()
        }
        val deviceId = DeviceIdProvider.getDeviceId(this)
//        Log.d(TAG, "device_id    = $deviceId")

        super.onCreate(savedInstanceState)
        noInternetDialog = NoInternetDialogController(this, networkMonitor)
        networkMonitor.start()
        observeNetwork()
    }

    protected fun setupWindow(contentView: View) {
        setupEdgeToEdge(contentView)
        ViewCompat.setOnApplyWindowInsetsListener(contentView) { view, insets ->
            val sys = insets.getInsets(
                WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.displayCutout()
            )
            val bottomPadding = if (isSplashScreen) 0 else insets.contentBottomInsetOrFallback(view)
            view.updatePadding(left = sys.left, right = sys.right, bottom = bottomPadding)
            insets
        }
        ViewCompat.requestApplyInsets(contentView)
        setStatusBarLightIcons(useLightStatusBarIcons)
    }

    private fun observeNetwork() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                networkMonitor.refresh()
                networkMonitor.isOnline.collect { online ->
                    noInternetDialog?.onConnectivityChanged(online)
                }
            }
        }
    }

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

    override fun onResume() {
        super.onResume()
        ViewCompat.requestApplyInsets(window.decorView)
    }

    override fun onDestroy() {
        noInternetDialog?.release()
        noInternetDialog = null
        super.onDestroy()
    }

    protected fun isScreenAlive(): Boolean = !isFinishing && !isDestroyed

    /**
     * Activity [androidx.activity.viewModels] has no ownerProducer.
     * Use this to keep a ViewModel on a custom [ViewModelStoreOwner] with Hilt's factory.
     */
    protected inline fun <reified VM : ViewModel> sharedViewModels(
        noinline ownerProducer: () -> ViewModelStoreOwner
    ): Lazy<VM> = ViewModelLazy(
        VM::class,
        { ownerProducer().viewModelStore },
        { defaultViewModelProviderFactory },
        { defaultViewModelCreationExtras }
    )
}

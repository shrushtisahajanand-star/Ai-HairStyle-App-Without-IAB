package com.hairstyle.changer.aitryon.ui.viewmodel

import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner

/**
 * Process-scoped owner so PhotoSelect / Gallery / Progress / Result share one
 * [GenerationViewModel], matching the previous Activity-scoped `activityViewModels()`.
 */
object GenerationViewModelOwner : ViewModelStoreOwner {
    override val viewModelStore: ViewModelStore = ViewModelStore()

    fun clear() {
        viewModelStore.clear()
    }
}

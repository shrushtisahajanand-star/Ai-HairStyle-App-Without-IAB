package com.hairstyle.changer.aitryon.utils

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.os.Handler
import android.os.Looper
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NetworkMonitor @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val connectivityManager =
        context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

    private val mainHandler = Handler(Looper.getMainLooper())

    private val _isOnline = MutableStateFlow(checkOnline())
    val isOnline: StateFlow<Boolean> = _isOnline.asStateFlow()

    private var started = false

    private val callback = object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network) {
            // Capabilities may not be validated yet â€” re-check shortly
            postRefresh(delayMs = 0L)
            postRefresh(delayMs = 400L)
        }

        override fun onLost(network: Network) {
            // activeNetwork can lag; force re-check now and after a beat
            postRefresh(delayMs = 0L)
            postRefresh(delayMs = 300L)
        }

        override fun onUnavailable() {
            postOnline(false)
        }

        override fun onCapabilitiesChanged(
            network: Network,
            networkCapabilities: NetworkCapabilities
        ) {
            val online = hasUsableInternet(networkCapabilities)
            postOnline(online)
        }
    }

    fun start() {
        if (started) {
            refresh()
            return
        }
        started = true
        refresh()
        runCatching {
            // Default network = what the app actually uses (Home / Splash / API)
            connectivityManager.registerDefaultNetworkCallback(callback)
            // Log.d(TAG, "DefaultNetworkCallback registered")
        }.onFailure {
            // Log.e(TAG, "Failed to register network callback", it)
            started = false
        }
    }

    fun stop() {
        if (!started) return
        started = false
        runCatching {
            connectivityManager.unregisterNetworkCallback(callback)
        }
    }

    fun refresh() {
        postOnline(checkOnline())
    }

    fun isNetworkAvailable(): Boolean = checkOnline()

    suspend fun awaitOnline() {
        start()
        if (checkOnline()) {
            postOnline(true)
            return
        }
        postOnline(false)
        isOnline.first { it }
    }

    private fun checkOnline(): Boolean {
        val network = connectivityManager.activeNetwork ?: return false
        val caps = connectivityManager.getNetworkCapabilities(network) ?: return false
        return hasUsableInternet(caps)
    }

    private fun hasUsableInternet(caps: NetworkCapabilities): Boolean {
        return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
            caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
    }

    private fun postRefresh(delayMs: Long) {
        mainHandler.postDelayed({
            val online = checkOnline()
            // Log.d(TAG, "refresh online=$online")
            _isOnline.value = online
        }, delayMs)
    }

    private fun postOnline(online: Boolean) {
        mainHandler.post {
            if (_isOnline.value != online) {
                // Log.d(TAG, "online -> $online")
            }
            _isOnline.value = online
        }
    }

    companion object {
        private const val TAG = "NetworkMonitor"
    }
}

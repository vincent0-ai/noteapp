package com.example.echowithin.data.network

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Lightweight wrapper around Android's ConnectivityManager that exposes a
 * Compose-friendly StateFlow so any screen can react to online/offline
 * transitions (and so the global UI can show a "You're offline — changes
 * will sync when you reconnect" banner).
 *
 * Usage:
 *   val online by NetworkMonitor.rememberIsOnline()
 *   if (online) { ... } else { OfflineBanner() }
 */
object NetworkMonitor {
    private val _isOnline = MutableStateFlow(true)
    val isOnline: StateFlow<Boolean> = _isOnline.asStateFlow()

    private var registered = false

    /**
     * Registers the network callback for the lifetime of the process.
     * Idempotent: safe to call from multiple screens / composables.
     * Reads the current connectivity synchronously on registration so the
     * initial state is correct (the callback can take ~50ms to fire).
     */
    @Synchronized
    fun ensureRegistered(context: Context) {
        if (registered) return
        registered = true
        val cm = context.applicationContext
            .getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

        // Seed with the current state — the system delivers callbacks
        // asynchronously and we don't want a 200ms flash of "online" when
        // the device is in airplane mode.
        _isOnline.value = cm.activeNetwork
            ?.let { cm.getNetworkCapabilities(it) }
            ?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true

        val request = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()

        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                val caps = cm.getNetworkCapabilities(network)
                _isOnline.value = caps?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true
            }

            override fun onLost(network: Network) {
                _isOnline.value = cm.activeNetwork
                    ?.let { cm.getNetworkCapabilities(it) }
                    ?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true
            }

            override fun onCapabilitiesChanged(network: Network, caps: NetworkCapabilities) {
                _isOnline.value = caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            }
        }

        try {
            cm.registerNetworkCallback(request, callback)
        } catch (_: Exception) {
            // Some emulators / restricted profiles throw SecurityException;
            // falling back to the seed value is safe.
        }
    }
}

/**
 * Compose-friendly accessor. Subscribes to the StateFlow for as long as
 * the calling composable is in the composition.
 */
@Composable
fun rememberIsOnline(): State<Boolean> {
    val context = LocalContext.current
    LaunchedEffect(Unit) { NetworkMonitor.ensureRegistered(context) }
    return NetworkMonitor.isOnline.collectAsState()
}

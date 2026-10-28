package com.danielcrvo.sintonia

import android.app.Application
import coil3.ImageLoader
import coil3.PlatformContext
import coil3.SingletonImageLoader
import coil3.disk.DiskCache
import coil3.memory.MemoryCache
import coil3.network.okhttp.OkHttpNetworkFetcherFactory
import com.danielcrvo.sintonia.core.data.importing.GuideRefreshWorker
import com.danielcrvo.sintonia.core.data.importing.ListRefresher
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import okhttp3.Dispatcher
import okhttp3.OkHttpClient
import okio.Path.Companion.toOkioPath
import javax.inject.Inject

@HiltAndroidApp
class SintoniaApp : Application(), SingletonImageLoader.Factory {
    @Inject lateinit var refresher: dagger.Lazy<ListRefresher>

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    /** IPTV logos share one host; the default per-host limit of 5 would queue them. */
    override fun newImageLoader(context: PlatformContext): ImageLoader {
        val client = OkHttpClient
            .Builder()
            .dispatcher(Dispatcher().apply { maxRequestsPerHost = ImageRequestsPerHost })
            .build()
        return ImageLoader
            .Builder(context)
            .components { add(OkHttpNetworkFetcherFactory(callFactory = { client })) }
            .memoryCache { MemoryCache.Builder().maxSizePercent(context, MemoryCachePercent).build() }
            .diskCache {
                DiskCache
                    .Builder()
                    .directory(cacheDir.resolve("images").toOkioPath())
                    .maxSizeBytes(DiskCacheBytes)
                    .build()
            }.build()
    }

    private companion object {
        const val ImageRequestsPerHost = 16
        const val MemoryCachePercent = 0.15
        const val DiskCacheBytes = 192L * 1024 * 1024
        const val RefreshDelayMillis = 8_000L
    }

    override fun onCreate() {
        super.onCreate()
        // Off the first-frame path so the refresh does not compete with scrolling.
        appScope.launch {
            // Scheduling is best-effort: the app works without WorkManager.
            runCatching { GuideRefreshWorker.schedule(this@SintoniaApp) }
            delay(RefreshDelayMillis)
            runCatching { refresher.get().refreshIfStale() }
        }
    }
}

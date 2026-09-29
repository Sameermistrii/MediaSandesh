package com.mediasandesh.news

import android.app.Application
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.disk.DiskCache
import coil.memory.MemoryCache
import com.mediasandesh.news.data.Library
import com.mediasandesh.news.data.Repository

class NewsApp : Application(), ImageLoaderFactory {
    override fun onCreate() {
        super.onCreate()
        if (applicationInfo.flags and android.content.pm.ApplicationInfo.FLAG_DEBUGGABLE != 0) {
            android.webkit.WebView.setWebContentsDebuggingEnabled(true)
        }
        AppPrefs.load(this)
        Library.init(this)
        Repository.init(this)
    }

    override fun newImageLoader(): ImageLoader = ImageLoader.Builder(this)
        .memoryCache { MemoryCache.Builder(this).maxSizePercent(0.2).build() }
        .diskCache {
            DiskCache.Builder()
                .directory(cacheDir.resolve("news_images"))
                .maxSizeBytes(80L * 1024 * 1024)
                .build()
        }
        .respectCacheHeaders(false)
        .build()
}

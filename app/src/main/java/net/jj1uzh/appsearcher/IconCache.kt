package net.jj1uzh.appsearcher

import androidx.compose.ui.graphics.ImageBitmap

object IconCache {
    private val cache = android.util.LruCache<String, ImageBitmap>(100)

    fun get(packageName: String): ImageBitmap? {
        return cache.get(packageName)
    }

    fun put(packageName: String, bitmap: ImageBitmap) {
        cache.put(packageName, bitmap)
    }
}

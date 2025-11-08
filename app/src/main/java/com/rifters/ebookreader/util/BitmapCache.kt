package com.rifters.ebookreader.util

import android.graphics.Bitmap
import android.util.LruCache

/**
 * LRU cache for storing bitmaps with automatic memory management.
 * Cache size is based on available memory.
 */
class BitmapCache private constructor() {
    
    private val memoryCache: LruCache<String, Bitmap>
    
    init {
        // Get max available VM memory, exceeding this amount will throw an OutOfMemory exception.
        // Use 1/8th of the available memory for this memory cache.
        val maxMemory = (Runtime.getRuntime().maxMemory() / 1024).toInt()
        val cacheSize = maxMemory / 8
        
        memoryCache = object : LruCache<String, Bitmap>(cacheSize) {
            override fun sizeOf(key: String, bitmap: Bitmap): Int {
                // The cache size will be measured in kilobytes rather than number of items.
                return bitmap.byteCount / 1024
            }
            
            override fun entryRemoved(
                evicted: Boolean,
                key: String,
                oldValue: Bitmap,
                newValue: Bitmap?
            ) {
                // Recycle bitmap when it's removed from cache
                if (evicted && !oldValue.isRecycled) {
                    oldValue.recycle()
                }
            }
        }
    }
    
    companion object {
        @Volatile
        private var instance: BitmapCache? = null
        
        fun getInstance(): BitmapCache {
            return instance ?: synchronized(this) {
                instance ?: BitmapCache().also { instance = it }
            }
        }
    }
    
    fun put(key: String, bitmap: Bitmap) {
        if (getBitmap(key) == null) {
            memoryCache.put(key, bitmap)
        }
    }
    
    fun getBitmap(key: String): Bitmap? {
        return memoryCache.get(key)
    }
    
    fun clear() {
        memoryCache.evictAll()
    }
    
    fun remove(key: String) {
        memoryCache.remove(key)
    }
    
    fun size(): Int {
        return memoryCache.size()
    }
}

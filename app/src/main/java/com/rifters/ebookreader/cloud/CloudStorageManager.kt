package com.rifters.ebookreader.cloud

import android.content.Context
import android.util.Log

/**
 * Manager for cloud storage providers
 * Handles multiple cloud providers and provider selection
 */
class CloudStorageManager(private val context: Context) {
    
    private val TAG = "CloudStorageManager"
    private val providers = mutableMapOf<String, CloudStorageProvider>()
    private var activeProvider: CloudStorageProvider? = null
    
    /**
     * Register a cloud storage provider
     */
    fun registerProvider(provider: CloudStorageProvider) {
        providers[provider.providerName] = provider
        Log.d(TAG, "Registered provider: ${provider.providerName}")
    }
    
    /**
     * Set the active provider
     */
    fun setActiveProvider(providerName: String) {
        activeProvider = providers[providerName]
        if (activeProvider == null) {
            Log.w(TAG, "Provider not found: $providerName")
        } else {
            Log.d(TAG, "Active provider set to: $providerName")
        }
    }
    
    /**
     * Get the currently active provider
     */
    fun getActiveProvider(): CloudStorageProvider? = activeProvider
    
    /**
     * Get all registered providers
     */
    fun getAllProviders(): List<CloudStorageProvider> = providers.values.toList()
    
    /**
     * Get a specific provider by name
     */
    fun getProvider(providerName: String): CloudStorageProvider? = providers[providerName]
    
    /**
     * Remove a provider
     */
    fun unregisterProvider(providerName: String) {
        providers.remove(providerName)
        if (activeProvider?.providerName == providerName) {
            activeProvider = null
        }
        Log.d(TAG, "Unregistered provider: $providerName")
    }
    
    /**
     * Check if any provider is configured and authenticated
     */
    suspend fun hasAuthenticatedProvider(): Boolean {
        return providers.values.any { it.isAuthenticated() }
    }
    
    companion object {
        @Volatile
        private var INSTANCE: CloudStorageManager? = null
        
        fun getInstance(context: Context): CloudStorageManager {
            return INSTANCE ?: synchronized(this) {
                val instance = CloudStorageManager(context.applicationContext)
                INSTANCE = instance
                instance
            }
        }
    }
}

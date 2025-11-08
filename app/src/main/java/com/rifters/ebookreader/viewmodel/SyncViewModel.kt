package com.rifters.ebookreader.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.rifters.ebookreader.database.BookDatabase
import com.rifters.ebookreader.sync.FirebaseSyncService
import com.rifters.ebookreader.sync.SyncRepository
import com.rifters.ebookreader.sync.SyncResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class SyncViewModel(application: Application) : AndroidViewModel(application) {
    
    private val database = BookDatabase.getDatabase(application)
    private val syncService = FirebaseSyncService()
    private val syncRepository = SyncRepository(
        cloudSyncService = syncService,
        bookDao = database.bookDao(),
        bookmarkDao = database.bookmarkDao(),
        syncDao = database.syncDao()
    )
    
    private val _syncStatus = MutableLiveData<SyncStatus>()
    val syncStatus: LiveData<SyncStatus> = _syncStatus
    
    private val _pendingSyncCount = MutableLiveData<Int>()
    val pendingSyncCount: LiveData<Int> = _pendingSyncCount
    
    private val _isAuthenticated = MutableLiveData<Boolean>()
    val isAuthenticated: LiveData<Boolean> = _isAuthenticated
    
    init {
        viewModelScope.launch(Dispatchers.IO) {
            checkAuthentication()
            updatePendingSyncCount()
        }
    }
    
    /**
     * Check if user is authenticated with cloud service
     */
    private suspend fun checkAuthentication() {
        val isAuth = syncService.isAuthenticated()
        _isAuthenticated.postValue(isAuth)
    }
    
    /**
     * Initialize and authenticate with cloud service
     */
    fun initializeSync() = viewModelScope.launch(Dispatchers.IO) {
        _syncStatus.postValue(SyncStatus.InProgress("Initializing..."))
        
        val initResult = syncService.initialize()
        if (!initResult) {
            _syncStatus.postValue(SyncStatus.Error("Failed to initialize sync service"))
            return@launch
        }
        
        if (!syncService.isAuthenticated()) {
            val authResult = syncService.signInAnonymously()
            if (authResult.isFailure) {
                _syncStatus.postValue(
                    SyncStatus.Error("Authentication failed: ${authResult.exceptionOrNull()?.message}")
                )
                return@launch
            }
        }
        
        _isAuthenticated.postValue(true)
        _syncStatus.postValue(SyncStatus.Success("Sync initialized successfully"))
    }
    
    /**
     * Sync all pending changes to cloud
     */
    fun syncToCloud() = viewModelScope.launch(Dispatchers.IO) {
        _syncStatus.postValue(SyncStatus.InProgress("Syncing to cloud..."))
        
        val result = syncRepository.syncAll()
        
        if (result.isSuccess) {
            val syncResult = result.getOrNull()
            if (syncResult != null) {
                if (syncResult.isFullSuccess) {
                    _syncStatus.postValue(
                        SyncStatus.Success("Synced ${syncResult.successCount} items successfully")
                    )
                } else {
                    _syncStatus.postValue(
                        SyncStatus.PartialSuccess(
                            "Synced ${syncResult.successCount} items, ${syncResult.failureCount} failed",
                            syncResult
                        )
                    )
                }
            } else {
                _syncStatus.postValue(SyncStatus.Success("Sync completed"))
            }
        } else {
            _syncStatus.postValue(
                SyncStatus.Error("Sync failed: ${result.exceptionOrNull()?.message}")
            )
        }
        
        updatePendingSyncCount()
    }
    
    /**
     * Pull updates from cloud
     */
    fun pullFromCloud() = viewModelScope.launch(Dispatchers.IO) {
        _syncStatus.postValue(SyncStatus.InProgress("Pulling from cloud..."))
        
        val result = syncRepository.pullFromCloud()
        
        if (result.isSuccess) {
            val syncResult = result.getOrNull()
            if (syncResult != null) {
                if (syncResult.isFullSuccess) {
                    _syncStatus.postValue(
                        SyncStatus.Success("Downloaded ${syncResult.successCount} updates")
                    )
                } else {
                    _syncStatus.postValue(
                        SyncStatus.PartialSuccess(
                            "Downloaded ${syncResult.successCount} updates, ${syncResult.failureCount} failed",
                            syncResult
                        )
                    )
                }
            } else {
                _syncStatus.postValue(SyncStatus.Success("Pull completed"))
            }
        } else {
            _syncStatus.postValue(
                SyncStatus.Error("Pull failed: ${result.exceptionOrNull()?.message}")
            )
        }
    }
    
    /**
     * Perform full sync (push then pull)
     */
    fun fullSync() = viewModelScope.launch(Dispatchers.IO) {
        _syncStatus.postValue(SyncStatus.InProgress("Performing full sync..."))
        
        // First push local changes
        val pushResult = syncRepository.syncAll()
        if (pushResult.isFailure) {
            _syncStatus.postValue(
                SyncStatus.Error("Sync failed: ${pushResult.exceptionOrNull()?.message}")
            )
            return@launch
        }
        
        // Then pull cloud changes
        val pullResult = syncRepository.pullFromCloud()
        if (pullResult.isFailure) {
            _syncStatus.postValue(
                SyncStatus.Error("Pull failed: ${pullResult.exceptionOrNull()?.message}")
            )
            return@launch
        }
        
        _syncStatus.postValue(SyncStatus.Success("Full sync completed"))
        updatePendingSyncCount()
    }
    
    /**
     * Mark a book for sync
     */
    fun markBookForSync(bookId: Long) = viewModelScope.launch(Dispatchers.IO) {
        syncRepository.markBookForSync(bookId)
        updatePendingSyncCount()
    }
    
    /**
     * Mark a bookmark for sync
     */
    fun markBookmarkForSync(bookmarkId: Long) = viewModelScope.launch(Dispatchers.IO) {
        syncRepository.markBookmarkForSync(bookmarkId)
        updatePendingSyncCount()
    }
    
    /**
     * Update pending sync count
     */
    private suspend fun updatePendingSyncCount() {
        val count = syncRepository.getPendingSyncCount()
        _pendingSyncCount.postValue(count)
    }
    
    /**
     * Clear sync status
     */
    fun clearSyncStatus() {
        _syncStatus.postValue(SyncStatus.Idle)
    }
    
    sealed class SyncStatus {
        object Idle : SyncStatus()
        data class InProgress(val message: String) : SyncStatus()
        data class Success(val message: String) : SyncStatus()
        data class PartialSuccess(val message: String, val result: SyncResult) : SyncStatus()
        data class Error(val message: String) : SyncStatus()
    }
}

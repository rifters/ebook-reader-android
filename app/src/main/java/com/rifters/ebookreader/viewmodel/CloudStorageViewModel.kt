package com.rifters.ebookreader.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.rifters.ebookreader.cloud.*
import kotlinx.coroutines.launch
import java.io.File

class CloudStorageViewModel(application: Application) : AndroidViewModel(application) {
    
    private val cloudManager = CloudStorageManager.getInstance(application)
    
    private val _files = MutableLiveData<CloudResult<List<CloudFile>>>()
    val files: LiveData<CloudResult<List<CloudFile>>> = _files
    
    private val _downloadResult = MutableLiveData<CloudResult<String>>()
    val downloadResult: LiveData<CloudResult<String>> = _downloadResult
    
    private val _uploadResult = MutableLiveData<CloudResult<CloudFile>>()
    val uploadResult: LiveData<CloudResult<CloudFile>> = _uploadResult
    
    private val _authResult = MutableLiveData<CloudResult<Boolean>>()
    val authResult: LiveData<CloudResult<Boolean>> = _authResult
    
    private val _quota = MutableLiveData<CloudResult<StorageQuota>>()
    val quota: LiveData<CloudResult<StorageQuota>> = _quota
    
    /**
     * Get the cloud storage manager
     */
    fun getCloudManager() = cloudManager
    
    /**
     * Authenticate with active provider
     */
    fun authenticate() {
        val provider = cloudManager.getActiveProvider()
        if (provider == null) {
            _authResult.value = CloudResult.Error("No active provider")
            return
        }
        
        _authResult.value = CloudResult.Loading
        viewModelScope.launch {
            try {
                val success = provider.authenticate()
                _authResult.value = CloudResult.Success(success)
            } catch (e: Exception) {
                _authResult.value = CloudResult.Error("Authentication failed", e)
            }
        }
    }
    
    /**
     * List files in the current directory
     */
    fun listFiles(path: String? = null) {
        val provider = cloudManager.getActiveProvider()
        if (provider == null) {
            _files.value = CloudResult.Error("No active provider")
            return
        }
        
        _files.value = CloudResult.Loading
        viewModelScope.launch {
            try {
                val fileList = provider.listFiles(path)
                _files.value = CloudResult.Success(fileList)
            } catch (e: Exception) {
                _files.value = CloudResult.Error("Failed to list files", e)
            }
        }
    }
    
    /**
     * Download a file from cloud storage
     */
    fun downloadFile(cloudFile: CloudFile, destinationPath: String) {
        val provider = cloudManager.getActiveProvider()
        if (provider == null) {
            _downloadResult.value = CloudResult.Error("No active provider")
            return
        }
        
        _downloadResult.value = CloudResult.Loading
        viewModelScope.launch {
            try {
                val inputStream = provider.downloadFile(cloudFile)
                if (inputStream != null) {
                    val outputFile = File(destinationPath)
                    outputFile.parentFile?.mkdirs()
                    
                    outputFile.outputStream().use { output ->
                        inputStream.use { input ->
                            input.copyTo(output)
                        }
                    }
                    
                    _downloadResult.value = CloudResult.Success(destinationPath)
                } else {
                    _downloadResult.value = CloudResult.Error("Failed to download file")
                }
            } catch (e: Exception) {
                _downloadResult.value = CloudResult.Error("Download failed", e)
            }
        }
    }
    
    /**
     * Upload a file to cloud storage
     */
    fun uploadFile(localPath: String, remotePath: String) {
        val provider = cloudManager.getActiveProvider()
        if (provider == null) {
            _uploadResult.value = CloudResult.Error("No active provider")
            return
        }
        
        _uploadResult.value = CloudResult.Loading
        viewModelScope.launch {
            try {
                val cloudFile = provider.uploadFile(localPath, remotePath)
                if (cloudFile != null) {
                    _uploadResult.value = CloudResult.Success(cloudFile)
                } else {
                    _uploadResult.value = CloudResult.Error("Upload failed")
                }
            } catch (e: Exception) {
                _uploadResult.value = CloudResult.Error("Upload failed", e)
            }
        }
    }
    
    /**
     * Get storage quota
     */
    fun getQuota() {
        val provider = cloudManager.getActiveProvider()
        if (provider == null) {
            _quota.value = CloudResult.Error("No active provider")
            return
        }
        
        _quota.value = CloudResult.Loading
        viewModelScope.launch {
            try {
                val quotaInfo = provider.getQuota()
                if (quotaInfo != null) {
                    _quota.value = CloudResult.Success(quotaInfo)
                } else {
                    _quota.value = CloudResult.Error("Quota information not available")
                }
            } catch (e: Exception) {
                _quota.value = CloudResult.Error("Failed to get quota", e)
            }
        }
    }
    
    /**
     * Sign out from active provider
     */
    fun signOut() {
        viewModelScope.launch {
            cloudManager.getActiveProvider()?.signOut()
        }
    }
}

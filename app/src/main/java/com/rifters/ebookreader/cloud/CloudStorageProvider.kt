package com.rifters.ebookreader.cloud

import java.io.InputStream

/**
 * Base interface for cloud storage providers
 * Supports listing files, downloading, and uploading
 */
interface CloudStorageProvider {
    
    /**
     * Provider name (e.g., "Google Drive", "OneDrive", "WebDAV")
     */
    val providerName: String
    
    /**
     * Check if user is authenticated with this provider
     */
    suspend fun isAuthenticated(): Boolean
    
    /**
     * Authenticate with the provider
     * @return true if authentication successful
     */
    suspend fun authenticate(): Boolean
    
    /**
     * Sign out from the provider
     */
    suspend fun signOut()
    
    /**
     * List files in a directory
     * @param path Directory path (null or empty for root)
     * @return List of files and folders
     */
    suspend fun listFiles(path: String? = null): List<CloudFile>
    
    /**
     * Download a file
     * @param file Cloud file to download
     * @return InputStream of file content
     */
    suspend fun downloadFile(file: CloudFile): InputStream?
    
    /**
     * Upload a file
     * @param localPath Local file path
     * @param remotePath Remote destination path
     * @return CloudFile representing uploaded file, or null if failed
     */
    suspend fun uploadFile(localPath: String, remotePath: String): CloudFile?
    
    /**
     * Delete a file
     * @param file Cloud file to delete
     * @return true if deletion successful
     */
    suspend fun deleteFile(file: CloudFile): Boolean
    
    /**
     * Create a folder
     * @param path Folder path
     * @return CloudFile representing the folder, or null if failed
     */
    suspend fun createFolder(path: String): CloudFile?
    
    /**
     * Get storage quota information
     */
    suspend fun getQuota(): StorageQuota?
}

/**
 * Represents a file or folder in cloud storage
 */
data class CloudFile(
    val id: String,
    val name: String,
    val path: String,
    val isDirectory: Boolean,
    val size: Long = 0,
    val mimeType: String? = null,
    val modifiedTime: Long = 0,
    val provider: String
)

/**
 * Storage quota information
 */
data class StorageQuota(
    val totalBytes: Long,
    val usedBytes: Long,
    val freeBytes: Long
) {
    val usagePercentage: Float
        get() = if (totalBytes > 0) (usedBytes.toFloat() / totalBytes) * 100f else 0f
}

/**
 * Result of a cloud operation
 */
sealed class CloudResult<out T> {
    data class Success<T>(val data: T) : CloudResult<T>()
    data class Error(val message: String, val exception: Exception? = null) : CloudResult<Nothing>()
    object Loading : CloudResult<Nothing>()
}

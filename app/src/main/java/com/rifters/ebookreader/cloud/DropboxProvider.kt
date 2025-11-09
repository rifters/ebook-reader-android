package com.rifters.ebookreader.cloud

import android.util.Log
import com.dropbox.core.DbxRequestConfig
import com.dropbox.core.v2.DbxClientV2
import com.dropbox.core.v2.files.FileMetadata
import com.dropbox.core.v2.files.FolderMetadata
import com.dropbox.core.v2.files.WriteMode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.InputStream

/**
 * Dropbox cloud storage provider
 * Uses Dropbox Core SDK
 */
class DropboxProvider(
    private val accessToken: String
) : CloudStorageProvider {
    
    override val providerName: String = "Dropbox"
    
    private val TAG = "DropboxProvider"
    private var client: DbxClientV2? = null
    
    override suspend fun isAuthenticated(): Boolean {
        return client != null
    }
    
    override suspend fun authenticate(): Boolean = withContext(Dispatchers.IO) {
        try {
            val config = DbxRequestConfig.newBuilder("EBookReader").build()
            client = DbxClientV2(config, accessToken)
            
            // Test connection
            client?.users()?.currentAccount
            true
        } catch (e: Exception) {
            Log.e(TAG, "Authentication failed", e)
            false
        }
    }
    
    override suspend fun signOut() {
        client = null
    }
    
    override suspend fun listFiles(path: String?): List<CloudFile> = withContext(Dispatchers.IO) {
        try {
            val dbxClient = client ?: return@withContext emptyList()
            val targetPath = if (path.isNullOrEmpty()) "" else path
            
            val result = dbxClient.files().listFolder(targetPath)
            
            result.entries.map { entry ->
                when (entry) {
                    is FileMetadata -> CloudFile(
                        id = entry.id,
                        name = entry.name,
                        path = entry.pathLower ?: entry.pathDisplay,
                        isDirectory = false,
                        size = entry.size,
                        mimeType = null,
                        modifiedTime = entry.serverModified?.time ?: 0,
                        provider = providerName
                    )
                    is FolderMetadata -> CloudFile(
                        id = entry.id,
                        name = entry.name,
                        path = entry.pathLower ?: entry.pathDisplay,
                        isDirectory = true,
                        size = 0,
                        mimeType = null,
                        modifiedTime = 0,
                        provider = providerName
                    )
                    else -> null
                }
            }.filterNotNull()
        } catch (e: Exception) {
            Log.e(TAG, "Error listing files", e)
            emptyList()
        }
    }
    
    override suspend fun downloadFile(file: CloudFile): InputStream? = withContext(Dispatchers.IO) {
        try {
            val dbxClient = client ?: return@withContext null
            dbxClient.files().download(file.path).inputStream
        } catch (e: Exception) {
            Log.e(TAG, "Error downloading file", e)
            null
        }
    }
    
    override suspend fun uploadFile(localPath: String, remotePath: String): CloudFile? = withContext(Dispatchers.IO) {
        try {
            val dbxClient = client ?: return@withContext null
            val localFile = File(localPath)
            
            localFile.inputStream().use { inputStream ->
                val metadata = dbxClient.files()
                    .uploadBuilder(remotePath)
                    .withMode(WriteMode.OVERWRITE)
                    .uploadAndFinish(inputStream)
                
                CloudFile(
                    id = metadata.id,
                    name = metadata.name,
                    path = metadata.pathLower ?: metadata.pathDisplay,
                    isDirectory = false,
                    size = metadata.size,
                    mimeType = null,
                    modifiedTime = metadata.serverModified?.time ?: System.currentTimeMillis(),
                    provider = providerName
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error uploading file", e)
            null
        }
    }
    
    override suspend fun deleteFile(file: CloudFile): Boolean = withContext(Dispatchers.IO) {
        try {
            val dbxClient = client ?: return@withContext false
            dbxClient.files().deleteV2(file.path)
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error deleting file", e)
            false
        }
    }
    
    override suspend fun createFolder(path: String): CloudFile? = withContext(Dispatchers.IO) {
        try {
            val dbxClient = client ?: return@withContext null
            val metadata = dbxClient.files().createFolderV2(path).metadata
            
            CloudFile(
                id = metadata.id,
                name = metadata.name,
                path = metadata.pathLower ?: metadata.pathDisplay,
                isDirectory = true,
                size = 0,
                mimeType = null,
                modifiedTime = System.currentTimeMillis(),
                provider = providerName
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error creating folder", e)
            null
        }
    }
    
    override suspend fun getQuota(): StorageQuota? = withContext(Dispatchers.IO) {
        try {
            val dbxClient = client ?: return@withContext null
            val spaceUsage = dbxClient.users().spaceUsage
            
            val allocated = spaceUsage.allocation.individualValue?.allocated ?: 0
            val used = spaceUsage.used
            
            StorageQuota(
                totalBytes = allocated,
                usedBytes = used,
                freeBytes = allocated - used
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error getting quota", e)
            null
        }
    }
}

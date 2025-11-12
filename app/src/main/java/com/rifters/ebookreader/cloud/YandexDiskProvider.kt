package com.rifters.ebookreader.cloud

import android.util.Log
import com.yandex.disk.rest.Credentials
import com.yandex.disk.rest.RestClient
import com.yandex.disk.rest.ResourcesArgs
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.InputStream

/**
 * Yandex Disk provider using Yandex Disk REST API SDK.
 * Requires OAuth token from Yandex OAuth service.
 */
class YandexDiskProvider(private val accessToken: String? = null) : CloudStorageProvider {

    override val providerName: String = "YandexDisk"
    private val TAG = "YandexDiskProvider"
    private var restClient: RestClient? = null

    override suspend fun isAuthenticated(): Boolean {
        return accessToken != null && accessToken.isNotEmpty()
    }

    override suspend fun authenticate(): Boolean = withContext(Dispatchers.IO) {
        try {
            if (accessToken == null) return@withContext false
            
            val credentials = Credentials("", accessToken)
            restClient = RestClient(credentials)
            
            // Test connection
            restClient?.getDiskInfo()
            true
        } catch (e: Exception) {
            Log.e(TAG, "Yandex Disk authentication failed", e)
            false
        }
    }

    override suspend fun signOut() {
        restClient = null
    }

    override suspend fun listFiles(path: String?): List<CloudFile> = withContext(Dispatchers.IO) {
        try {
            val client = restClient ?: return@withContext emptyList()
            val targetPath = path ?: "disk:/"
            
            val resourceArgs = ResourcesArgs.Builder()
                .setPath(targetPath)
                .setLimit(200)
                .build()

            val resource = client.getResources(resourceArgs)
            val items = resource.resourceList?.items ?: emptyList()
            
            items.map { item ->
                CloudFile(
                    id = item.path.path,
                    name = item.name,
                    path = item.path.path,
                    isDirectory = item.isDir,
                    size = item.size ?: 0,
                    mimeType = item.mimeType,
                    modifiedTime = item.modified?.time ?: 0,
                    provider = providerName
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error listing files", e)
            emptyList()
        }
    }

    override suspend fun downloadFile(file: CloudFile): InputStream? = withContext(Dispatchers.IO) {
        try {
            val client = restClient ?: return@withContext null
            
            Log.w(TAG, "Download not implemented for YandexDisk")
            null
        } catch (e: Exception) {
            Log.e(TAG, "Error downloading file", e)
            null
        }
    }

    override suspend fun uploadFile(localPath: String, remotePath: String): CloudFile? = withContext(Dispatchers.IO) {
        try {
            val client = restClient ?: return@withContext null
            val localFile = File(localPath)
            
            // Get upload link
            val link = client.getUploadLink(remotePath, true)
            
            // Upload file (would use OkHttp)
            // client.uploadFile(link, true, localFile, null)
            
            // Return CloudFile representation
            null
        } catch (e: Exception) {
            Log.e(TAG, "Error uploading file", e)
            null
        }
    }

    override suspend fun deleteFile(file: CloudFile): Boolean = withContext(Dispatchers.IO) {
        try {
            val client = restClient ?: return@withContext false
            client.delete(file.path, true)
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error deleting file", e)
            false
        }
    }

    override suspend fun createFolder(path: String): CloudFile? = withContext(Dispatchers.IO) {
        try {
            val client = restClient ?: return@withContext null
            client.makeFolder(path)
            
            CloudFile(
                id = path,
                name = path.substringAfterLast('/'),
                path = path,
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
            val client = restClient ?: return@withContext null
            val diskInfo = client.getDiskInfo()
            
            StorageQuota(
                totalBytes = diskInfo.totalSpace,
                usedBytes = diskInfo.usedSpace,
                freeBytes = diskInfo.totalSpace - diskInfo.usedSpace
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error getting quota", e)
            null
        }
    }
}

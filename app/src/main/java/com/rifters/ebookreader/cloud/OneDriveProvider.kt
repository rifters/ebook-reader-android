package com.rifters.ebookreader.cloud

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL

/**
 * OneDrive cloud storage provider
 * Uses Microsoft Graph API
 * Note: Requires OAuth 2.0 authentication flow which should be implemented separately
 */
class OneDriveProvider(
    private val accessToken: String
) : CloudStorageProvider {
    
    override val providerName: String = "OneDrive"
    
    private val TAG = "OneDriveProvider"
    private val GRAPH_API_ENDPOINT = "https://graph.microsoft.com/v1.0"
    
    override suspend fun isAuthenticated(): Boolean {
        return accessToken.isNotEmpty()
    }
    
    override suspend fun authenticate(): Boolean = withContext(Dispatchers.IO) {
        try {
            // Test token by getting user info
            val url = URL("$GRAPH_API_ENDPOINT/me/drive")
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.setRequestProperty("Authorization", "Bearer $accessToken")
            
            val responseCode = connection.responseCode
            connection.disconnect()
            
            responseCode in 200..299
        } catch (e: Exception) {
            Log.e(TAG, "Authentication failed", e)
            false
        }
    }
    
    override suspend fun signOut() {
        // OAuth token should be revoked in the OAuth flow
    }
    
    override suspend fun listFiles(path: String?): List<CloudFile> = withContext(Dispatchers.IO) {
        try {
            val endpoint = if (path.isNullOrEmpty()) {
                "$GRAPH_API_ENDPOINT/me/drive/root/children"
            } else {
                "$GRAPH_API_ENDPOINT/me/drive/items/$path/children"
            }
            
            val url = URL(endpoint)
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.setRequestProperty("Authorization", "Bearer $accessToken")
            
            val responseCode = connection.responseCode
            if (responseCode !in 200..299) {
                connection.disconnect()
                return@withContext emptyList()
            }
            
            val response = connection.inputStream.bufferedReader().use { it.readText() }
            connection.disconnect()
            
            parseFilesResponse(response)
        } catch (e: Exception) {
            Log.e(TAG, "Error listing files", e)
            emptyList()
        }
    }
    
    private fun parseFilesResponse(json: String): List<CloudFile> {
        return try {
            val jsonObject = JSONObject(json)
            val valuesArray = jsonObject.getJSONArray("value")
            
            (0 until valuesArray.length()).mapNotNull { i ->
                val item = valuesArray.getJSONObject(i)
                CloudFile(
                    id = item.getString("id"),
                    name = item.getString("name"),
                    path = item.getString("id"),
                    isDirectory = item.has("folder"),
                    size = item.optLong("size", 0),
                    mimeType = item.optString("mimeType", null),
                    modifiedTime = parseDateTime(item.optString("lastModifiedDateTime", "")),
                    provider = providerName
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing files response", e)
            emptyList()
        }
    }
    
    private fun parseDateTime(dateTime: String): Long {
        return try {
            if (dateTime.isEmpty()) return 0
            // Simplified date parsing - in production, use DateTimeFormatter
            System.currentTimeMillis()
        } catch (e: Exception) {
            0
        }
    }
    
    override suspend fun downloadFile(file: CloudFile): InputStream? = withContext(Dispatchers.IO) {
        try {
            // Get download URL
            val url = URL("$GRAPH_API_ENDPOINT/me/drive/items/${file.id}/content")
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.setRequestProperty("Authorization", "Bearer $accessToken")
            connection.instanceFollowRedirects = true
            
            val responseCode = connection.responseCode
            if (responseCode in 200..299) {
                connection.inputStream
            } else {
                connection.disconnect()
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error downloading file", e)
            null
        }
    }
    
    override suspend fun uploadFile(localPath: String, remotePath: String): CloudFile? = withContext(Dispatchers.IO) {
        try {
            val localFile = java.io.File(localPath)
            val url = URL("$GRAPH_API_ENDPOINT/me/drive/root:/$remotePath:/content")
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "PUT"
            connection.setRequestProperty("Authorization", "Bearer $accessToken")
            connection.setRequestProperty("Content-Type", "application/octet-stream")
            connection.doOutput = true
            
            localFile.inputStream().use { input ->
                connection.outputStream.use { output ->
                    input.copyTo(output)
                }
            }
            
            val responseCode = connection.responseCode
            if (responseCode in 200..299) {
                val response = connection.inputStream.bufferedReader().use { it.readText() }
                connection.disconnect()
                
                val json = JSONObject(response)
                CloudFile(
                    id = json.getString("id"),
                    name = json.getString("name"),
                    path = json.getString("id"),
                    isDirectory = false,
                    size = json.getLong("size"),
                    mimeType = json.optString("mimeType", null),
                    modifiedTime = System.currentTimeMillis(),
                    provider = providerName
                )
            } else {
                connection.disconnect()
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error uploading file", e)
            null
        }
    }
    
    override suspend fun deleteFile(file: CloudFile): Boolean = withContext(Dispatchers.IO) {
        try {
            val url = URL("$GRAPH_API_ENDPOINT/me/drive/items/${file.id}")
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "DELETE"
            connection.setRequestProperty("Authorization", "Bearer $accessToken")
            
            val responseCode = connection.responseCode
            connection.disconnect()
            
            responseCode in 200..299
        } catch (e: Exception) {
            Log.e(TAG, "Error deleting file", e)
            false
        }
    }
    
    override suspend fun createFolder(path: String): CloudFile? = withContext(Dispatchers.IO) {
        try {
            val url = URL("$GRAPH_API_ENDPOINT/me/drive/root/children")
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "POST"
            connection.setRequestProperty("Authorization", "Bearer $accessToken")
            connection.setRequestProperty("Content-Type", "application/json")
            connection.doOutput = true
            
            val folderName = path.substringAfterLast('/')
            val requestBody = """
                {
                    "name": "$folderName",
                    "folder": {},
                    "@microsoft.graph.conflictBehavior": "rename"
                }
            """.trimIndent()
            
            connection.outputStream.use { it.write(requestBody.toByteArray()) }
            
            val responseCode = connection.responseCode
            if (responseCode in 200..299) {
                val response = connection.inputStream.bufferedReader().use { it.readText() }
                connection.disconnect()
                
                val json = JSONObject(response)
                CloudFile(
                    id = json.getString("id"),
                    name = json.getString("name"),
                    path = json.getString("id"),
                    isDirectory = true,
                    size = 0,
                    mimeType = null,
                    modifiedTime = System.currentTimeMillis(),
                    provider = providerName
                )
            } else {
                connection.disconnect()
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error creating folder", e)
            null
        }
    }
    
    override suspend fun getQuota(): StorageQuota? = withContext(Dispatchers.IO) {
        try {
            val url = URL("$GRAPH_API_ENDPOINT/me/drive")
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.setRequestProperty("Authorization", "Bearer $accessToken")
            
            val responseCode = connection.responseCode
            if (responseCode in 200..299) {
                val response = connection.inputStream.bufferedReader().use { it.readText() }
                connection.disconnect()
                
                val json = JSONObject(response)
                val quota = json.getJSONObject("quota")
                val total = quota.getLong("total")
                val used = quota.getLong("used")
                
                StorageQuota(
                    totalBytes = total,
                    usedBytes = used,
                    freeBytes = total - used
                )
            } else {
                connection.disconnect()
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error getting quota", e)
            null
        }
    }
}

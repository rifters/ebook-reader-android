package com.rifters.ebookreader.cloud

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL
import java.util.Base64

/**
 * WebDAV cloud storage provider
 * Supports any WebDAV-compatible server (Nextcloud, ownCloud, etc.)
 */
class WebDavProvider(
    private val serverUrl: String,
    private val username: String,
    private val password: String
) : CloudStorageProvider {
    
    override val providerName: String = "WebDAV"
    
    private val TAG = "WebDavProvider"
    private var isAuth = false
    
    override suspend fun isAuthenticated(): Boolean = isAuth
    
    override suspend fun authenticate(): Boolean = withContext(Dispatchers.IO) {
        try {
            // Test connection with PROPFIND request
            val url = URL(serverUrl)
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "PROPFIND"
            connection.setRequestProperty("Authorization", getAuthHeader())
            connection.setRequestProperty("Depth", "0")
            connection.connectTimeout = 10000
            connection.readTimeout = 10000
            
            val responseCode = connection.responseCode
            connection.disconnect()
            
            isAuth = responseCode in 200..299
            isAuth
        } catch (e: Exception) {
            Log.e(TAG, "Authentication failed", e)
            false
        }
    }
    
    override suspend fun signOut() {
        isAuth = false
    }
    
    override suspend fun listFiles(path: String?): List<CloudFile> = withContext(Dispatchers.IO) {
        try {
            val targetPath = if (path.isNullOrEmpty()) serverUrl else "$serverUrl/$path"
            val url = URL(targetPath)
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "PROPFIND"
            connection.setRequestProperty("Authorization", getAuthHeader())
            connection.setRequestProperty("Depth", "1")
            connection.setRequestProperty("Content-Type", "application/xml; charset=utf-8")
            
            val responseCode = connection.responseCode
            if (responseCode !in 200..299) {
                connection.disconnect()
                return@withContext emptyList()
            }
            
            val response = connection.inputStream.bufferedReader().use { it.readText() }
            connection.disconnect()
            
            parseWebDavResponse(response, path ?: "")
        } catch (e: Exception) {
            Log.e(TAG, "Error listing files", e)
            emptyList()
        }
    }
    
    override suspend fun downloadFile(file: CloudFile): InputStream? = withContext(Dispatchers.IO) {
        try {
            val url = URL("$serverUrl/${file.path}")
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.setRequestProperty("Authorization", getAuthHeader())
            
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
            val file = java.io.File(localPath)
            val url = URL("$serverUrl/$remotePath")
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "PUT"
            connection.setRequestProperty("Authorization", getAuthHeader())
            connection.setRequestProperty("Content-Type", "application/octet-stream")
            connection.doOutput = true
            
            file.inputStream().use { input ->
                connection.outputStream.use { output ->
                    input.copyTo(output)
                }
            }
            
            val responseCode = connection.responseCode
            connection.disconnect()
            
            if (responseCode in 200..299) {
                CloudFile(
                    id = remotePath,
                    name = file.name,
                    path = remotePath,
                    isDirectory = false,
                    size = file.length(),
                    mimeType = null,
                    modifiedTime = System.currentTimeMillis(),
                    provider = providerName
                )
            } else {
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error uploading file", e)
            null
        }
    }
    
    override suspend fun deleteFile(file: CloudFile): Boolean = withContext(Dispatchers.IO) {
        try {
            val url = URL("$serverUrl/${file.path}")
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "DELETE"
            connection.setRequestProperty("Authorization", getAuthHeader())
            
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
            val url = URL("$serverUrl/$path")
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "MKCOL"
            connection.setRequestProperty("Authorization", getAuthHeader())
            
            val responseCode = connection.responseCode
            connection.disconnect()
            
            if (responseCode in 200..299) {
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
            } else {
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error creating folder", e)
            null
        }
    }
    
    override suspend fun getQuota(): StorageQuota? {
        // WebDAV doesn't have a standard quota API
        return null
    }
    
    private fun getAuthHeader(): String {
        val credentials = "$username:$password"
        val encodedCredentials = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            Base64.getEncoder().encodeToString(credentials.toByteArray())
        } else {
            android.util.Base64.encodeToString(credentials.toByteArray(), android.util.Base64.NO_WRAP)
        }
        return "Basic $encodedCredentials"
    }
    
    private fun parseWebDavResponse(xml: String, basePath: String): List<CloudFile> {
        val files = mutableListOf<CloudFile>()
        
        try {
            // Simple XML parsing for WebDAV responses
            // Look for <d:response> elements
            val responsePattern = Regex("<d:response>(.*?)</d:response>", RegexOption.DOT_MATCHES_ALL)
            val hrefPattern = Regex("<d:href>(.*?)</d:href>")
            val collectionPattern = Regex("<d:collection\\s*/?>")
            val contentLengthPattern = Regex("<d:getcontentlength>(\\d+)</d:getcontentlength>")
            val lastModifiedPattern = Regex("<d:getlastmodified>(.*?)</d:getlastmodified>")
            
            responsePattern.findAll(xml).forEach { matchResult ->
                val responseXml = matchResult.groupValues[1]
                
                val hrefMatch = hrefPattern.find(responseXml)
                if (hrefMatch != null) {
                    val href = hrefMatch.groupValues[1]
                    val path = href.removePrefix(serverUrl).removePrefix("/")
                    
                    // Skip the current directory itself
                    if (path.isEmpty() || path == basePath) return@forEach
                    
                    val isDirectory = collectionPattern.containsMatchIn(responseXml)
                    val size = contentLengthPattern.find(responseXml)?.groupValues?.get(1)?.toLongOrNull() ?: 0
                    val name = path.substringAfterLast('/')
                    
                    files.add(
                        CloudFile(
                            id = path,
                            name = name,
                            path = path,
                            isDirectory = isDirectory,
                            size = size,
                            mimeType = null,
                            modifiedTime = 0,
                            provider = providerName
                        )
                    )
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing WebDAV response", e)
        }
        
        return files
    }
}

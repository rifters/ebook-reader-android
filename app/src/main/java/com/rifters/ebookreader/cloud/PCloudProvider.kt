package com.rifters.ebookreader.cloud

import java.io.InputStream

/**
 * pCloud provider skeleton.
 */
class PCloudProvider(private val accessToken: String? = null) : CloudStorageProvider {

    override val providerName: String = "pCloud"

    override suspend fun isAuthenticated(): Boolean = accessToken != null

    override suspend fun authenticate(): Boolean {
        // TODO: implement OAuth
        return false
    }

    override suspend fun signOut() {}

    override suspend fun listFiles(path: String?): List<CloudFile> = emptyList()

    override suspend fun downloadFile(file: CloudFile): InputStream? = null

    override suspend fun uploadFile(localPath: String, remotePath: String): CloudFile? = null

    override suspend fun deleteFile(file: CloudFile): Boolean = false

    override suspend fun createFolder(path: String): CloudFile? = null

    override suspend fun getQuota(): StorageQuota? = null
}

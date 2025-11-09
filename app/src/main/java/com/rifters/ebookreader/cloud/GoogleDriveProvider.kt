package com.rifters.ebookreader.cloud

import android.content.Context
import android.util.Log
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.Scope
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential
import com.google.api.client.http.javanet.NetHttpTransport
import com.google.api.client.json.gson.GsonFactory
import com.google.api.services.drive.Drive
import com.google.api.services.drive.DriveScopes
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.InputStream

/**
 * Google Drive cloud storage provider
 * Uses Google Drive API v3
 */
class GoogleDriveProvider(
    private val context: Context
) : CloudStorageProvider {
    
    override val providerName: String = "Google Drive"
    
    private val TAG = "GoogleDriveProvider"
    private var driveService: Drive? = null
    private var account: GoogleSignInAccount? = null
    
    override suspend fun isAuthenticated(): Boolean {
        account = GoogleSignIn.getLastSignedInAccount(context)
        return account != null && driveService != null
    }
    
    override suspend fun authenticate(): Boolean = withContext(Dispatchers.IO) {
        try {
            account = GoogleSignIn.getLastSignedInAccount(context)
            if (account == null) {
                Log.e(TAG, "No signed in account")
                return@withContext false
            }
            
            val credential = GoogleAccountCredential.usingOAuth2(
                context,
                listOf(DriveScopes.DRIVE_FILE, DriveScopes.DRIVE_READONLY)
            )
            credential.selectedAccount = account!!.account
            
            driveService = Drive.Builder(
                NetHttpTransport(),
                GsonFactory.getDefaultInstance(),
                credential
            )
                .setApplicationName("EBook Reader")
                .build()
            
            true
        } catch (e: Exception) {
            Log.e(TAG, "Authentication failed", e)
            false
        }
    }
    
    override suspend fun signOut() {
        driveService = null
        account = null
    }
    
    override suspend fun listFiles(path: String?): List<CloudFile> = withContext(Dispatchers.IO) {
        try {
            val service = driveService ?: return@withContext emptyList()
            
            // Build query - list files in folder or root
            val query = if (path.isNullOrEmpty()) {
                "'root' in parents and trashed=false"
            } else {
                "'$path' in parents and trashed=false"
            }
            
            val result = service.files().list()
                .setQ(query)
                .setFields("files(id, name, mimeType, size, modifiedTime)")
                .setPageSize(100)
                .execute()
            
            result.files.map { file ->
                CloudFile(
                    id = file.id,
                    name = file.name,
                    path = file.id, // Google Drive uses IDs as paths
                    isDirectory = file.mimeType == "application/vnd.google-apps.folder",
                    size = file.getSize() ?: 0,
                    mimeType = file.mimeType,
                    modifiedTime = file.modifiedTime?.value ?: 0,
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
            val service = driveService ?: return@withContext null
            
            val outputStream = ByteArrayOutputStream()
            service.files().get(file.id)
                .executeMediaAndDownloadTo(outputStream)
            
            outputStream.toByteArray().inputStream()
        } catch (e: Exception) {
            Log.e(TAG, "Error downloading file", e)
            null
        }
    }
    
    override suspend fun uploadFile(localPath: String, remotePath: String): CloudFile? = withContext(Dispatchers.IO) {
        try {
            val service = driveService ?: return@withContext null
            val localFile = java.io.File(localPath)
            
            val fileMetadata = com.google.api.services.drive.model.File()
            fileMetadata.name = localFile.name
            
            val mediaContent = com.google.api.client.http.FileContent(
                "application/octet-stream",
                localFile
            )
            
            val uploadedFile = service.files().create(fileMetadata, mediaContent)
                .setFields("id, name, mimeType, size, modifiedTime")
                .execute()
            
            CloudFile(
                id = uploadedFile.id,
                name = uploadedFile.name,
                path = uploadedFile.id,
                isDirectory = false,
                size = uploadedFile.getSize() ?: 0,
                mimeType = uploadedFile.mimeType,
                modifiedTime = uploadedFile.modifiedTime?.value ?: System.currentTimeMillis(),
                provider = providerName
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error uploading file", e)
            null
        }
    }
    
    override suspend fun deleteFile(file: CloudFile): Boolean = withContext(Dispatchers.IO) {
        try {
            val service = driveService ?: return@withContext false
            service.files().delete(file.id).execute()
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error deleting file", e)
            false
        }
    }
    
    override suspend fun createFolder(path: String): CloudFile? = withContext(Dispatchers.IO) {
        try {
            val service = driveService ?: return@withContext null
            
            val folderMetadata = com.google.api.services.drive.model.File()
            folderMetadata.name = path
            folderMetadata.mimeType = "application/vnd.google-apps.folder"
            
            val folder = service.files().create(folderMetadata)
                .setFields("id, name, mimeType, modifiedTime")
                .execute()
            
            CloudFile(
                id = folder.id,
                name = folder.name,
                path = folder.id,
                isDirectory = true,
                size = 0,
                mimeType = folder.mimeType,
                modifiedTime = folder.modifiedTime?.value ?: System.currentTimeMillis(),
                provider = providerName
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error creating folder", e)
            null
        }
    }
    
    override suspend fun getQuota(): StorageQuota? = withContext(Dispatchers.IO) {
        try {
            val service = driveService ?: return@withContext null
            
            val about = service.about().get()
                .setFields("storageQuota")
                .execute()
            
            val quota = about.storageQuota
            StorageQuota(
                totalBytes = quota.limit ?: 0,
                usedBytes = quota.usage ?: 0,
                freeBytes = (quota.limit ?: 0) - (quota.usage ?: 0)
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error getting quota", e)
            null
        }
    }
    
    companion object {
        /**
         * Get GoogleSignInOptions for Drive access
         */
        fun getSignInOptions(): GoogleSignInOptions {
            return GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestEmail()
                .requestScopes(
                    Scope(DriveScopes.DRIVE_FILE),
                    Scope(DriveScopes.DRIVE_READONLY)
                )
                .build()
        }
    }
}

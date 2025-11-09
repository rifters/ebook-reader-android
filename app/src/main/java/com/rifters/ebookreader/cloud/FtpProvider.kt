package com.rifters.ebookreader.cloud

import android.util.Log
import com.jcraft.jsch.ChannelSftp
import com.jcraft.jsch.JSch
import com.jcraft.jsch.Session
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.apache.commons.net.ftp.FTP
import org.apache.commons.net.ftp.FTPClient
import org.apache.commons.net.ftp.FTPFile
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.InputStream

/**
 * FTP/SFTP cloud storage provider
 * Supports both FTP and SFTP protocols
 */
class FtpProvider(
    private val host: String,
    private val port: Int,
    private val username: String,
    private val password: String,
    private val useSftp: Boolean = false
) : CloudStorageProvider {
    
    override val providerName: String = if (useSftp) "SFTP" else "FTP"
    
    private val TAG = "FtpProvider"
    
    // FTP
    private var ftpClient: FTPClient? = null
    
    // SFTP
    private var sftpSession: Session? = null
    private var sftpChannel: ChannelSftp? = null
    
    override suspend fun isAuthenticated(): Boolean {
        return if (useSftp) {
            sftpSession?.isConnected == true && sftpChannel?.isConnected == true
        } else {
            ftpClient?.isConnected == true
        }
    }
    
    override suspend fun authenticate(): Boolean = withContext(Dispatchers.IO) {
        try {
            if (useSftp) {
                authenticateSftp()
            } else {
                authenticateFtp()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Authentication failed", e)
            false
        }
    }
    
    private fun authenticateFtp(): Boolean {
        ftpClient = FTPClient()
        ftpClient?.connect(host, port)
        
        val loggedIn = ftpClient?.login(username, password) ?: false
        if (loggedIn) {
            ftpClient?.enterLocalPassiveMode()
            ftpClient?.setFileType(FTP.BINARY_FILE_TYPE)
        }
        
        return loggedIn
    }
    
    private fun authenticateSftp(): Boolean {
        val jsch = JSch()
        sftpSession = jsch.getSession(username, host, port)
        sftpSession?.setPassword(password)
        
        val config = java.util.Properties()
        config["StrictHostKeyChecking"] = "no"
        sftpSession?.setConfig(config)
        
        sftpSession?.connect()
        
        val channel = sftpSession?.openChannel("sftp")
        channel?.connect()
        sftpChannel = channel as? ChannelSftp
        
        return sftpChannel != null
    }
    
    override suspend fun signOut() {
        if (useSftp) {
            sftpChannel?.disconnect()
            sftpSession?.disconnect()
            sftpChannel = null
            sftpSession = null
        } else {
            ftpClient?.disconnect()
            ftpClient = null
        }
    }
    
    override suspend fun listFiles(path: String?): List<CloudFile> = withContext(Dispatchers.IO) {
        try {
            if (useSftp) {
                listFilesSftp(path)
            } else {
                listFilesFtp(path)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error listing files", e)
            emptyList()
        }
    }
    
    private fun listFilesFtp(path: String?): List<CloudFile> {
        val targetPath = path ?: "/"
        val files = ftpClient?.listFiles(targetPath) ?: return emptyList()
        
        return files.map { file ->
            CloudFile(
                id = "$targetPath/${file.name}",
                name = file.name,
                path = "$targetPath/${file.name}",
                isDirectory = file.isDirectory,
                size = file.size,
                mimeType = null,
                modifiedTime = file.timestamp?.timeInMillis ?: 0,
                provider = providerName
            )
        }
    }
    
    private fun listFilesSftp(path: String?): List<CloudFile> {
        val targetPath = path ?: "/"
        val files = sftpChannel?.ls(targetPath) ?: return emptyList()
        
        return files.mapNotNull { entry ->
            val lsEntry = entry as? com.jcraft.jsch.ChannelSftp.LsEntry
            if (lsEntry != null && lsEntry.filename != "." && lsEntry.filename != "..") {
                CloudFile(
                    id = "$targetPath/${lsEntry.filename}",
                    name = lsEntry.filename,
                    path = "$targetPath/${lsEntry.filename}",
                    isDirectory = lsEntry.attrs.isDir,
                    size = lsEntry.attrs.size,
                    mimeType = null,
                    modifiedTime = lsEntry.attrs.mTime * 1000L,
                    provider = providerName
                )
            } else {
                null
            }
        }
    }
    
    override suspend fun downloadFile(file: CloudFile): InputStream? = withContext(Dispatchers.IO) {
        try {
            if (useSftp) {
                downloadFileSftp(file)
            } else {
                downloadFileFtp(file)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error downloading file", e)
            null
        }
    }
    
    private fun downloadFileFtp(file: CloudFile): InputStream? {
        val outputStream = ByteArrayOutputStream()
        val success = ftpClient?.retrieveFile(file.path, outputStream) ?: false
        
        return if (success) {
            ByteArrayInputStream(outputStream.toByteArray())
        } else {
            null
        }
    }
    
    private fun downloadFileSftp(file: CloudFile): InputStream? {
        val outputStream = ByteArrayOutputStream()
        sftpChannel?.get(file.path, outputStream)
        return ByteArrayInputStream(outputStream.toByteArray())
    }
    
    override suspend fun uploadFile(localPath: String, remotePath: String): CloudFile? = withContext(Dispatchers.IO) {
        try {
            val localFile = java.io.File(localPath)
            
            val success = if (useSftp) {
                uploadFileSftp(localFile, remotePath)
            } else {
                uploadFileFtp(localFile, remotePath)
            }
            
            if (success) {
                CloudFile(
                    id = remotePath,
                    name = localFile.name,
                    path = remotePath,
                    isDirectory = false,
                    size = localFile.length(),
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
    
    private fun uploadFileFtp(localFile: java.io.File, remotePath: String): Boolean {
        return localFile.inputStream().use { inputStream ->
            ftpClient?.storeFile(remotePath, inputStream) ?: false
        }
    }
    
    private fun uploadFileSftp(localFile: java.io.File, remotePath: String): Boolean {
        localFile.inputStream().use { inputStream ->
            sftpChannel?.put(inputStream, remotePath)
        }
        return true
    }
    
    override suspend fun deleteFile(file: CloudFile): Boolean = withContext(Dispatchers.IO) {
        try {
            if (useSftp) {
                sftpChannel?.rm(file.path)
                true
            } else {
                ftpClient?.deleteFile(file.path) ?: false
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error deleting file", e)
            false
        }
    }
    
    override suspend fun createFolder(path: String): CloudFile? = withContext(Dispatchers.IO) {
        try {
            val success = if (useSftp) {
                sftpChannel?.mkdir(path)
                true
            } else {
                ftpClient?.makeDirectory(path) ?: false
            }
            
            if (success) {
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
        // FTP/SFTP doesn't have a standard quota API
        return null
    }
}

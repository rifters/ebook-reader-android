package com.rifters.ebookreader.sync

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.rifters.ebookreader.cloud.CloudFile
import com.rifters.ebookreader.cloud.CloudStorageManager
import com.rifters.ebookreader.cloud.ConflictResolver
import com.rifters.ebookreader.util.DownloadNotificationHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Worker that downloads a file from a cloud provider and saves it to local storage.
 * Shows progress notifications with cancel support.
 * Input Data keys:
 * - providerName (String)
 * - filePath (String) // remote path
 * - localDestination (String) // absolute local path to save
 * - fileName (String) // display name for notification
 */
class DownloadWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    private val TAG = "DownloadWorker"
    private val notificationHelper = DownloadNotificationHelper(context)

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            val providerName = inputData.getString("providerName") ?: return@withContext Result.failure()
            val remotePath = inputData.getString("filePath") ?: return@withContext Result.failure()
            val destination = inputData.getString("localDestination") ?: return@withContext Result.failure()
            val fileName = inputData.getString("fileName") ?: File(remotePath).name

            // Show initial notification
            notificationHelper.showProgressNotification(id.toString(), fileName, 0)

            val manager = CloudStorageManager.get(applicationContext)
            val provider = manager.getProviderByName(providerName) ?: return@withContext Result.failure()

            // List files to find the file id if necessary (providers may use path directly)
            val cloudFile = CloudFile(
                id = remotePath,
                name = fileName,
                path = remotePath,
                isDirectory = false,
                provider = providerName
            )

            provider.authenticate()

            val input = provider.downloadFile(cloudFile) ?: run {
                notificationHelper.showErrorNotification(id.toString(), fileName, "Download failed")
                return@withContext Result.retry()
            }

            // Resolve conflict
            val destFile = File(destination)
            val resolved = ConflictResolver.resolve(destFile, ConflictResolver.Strategy.RENAME) ?: run {
                notificationHelper.showErrorNotification(id.toString(), fileName, "Cannot resolve file name")
                return@withContext Result.failure()
            }

            // Download with progress tracking
            val totalSize = cloudFile.size
            var downloaded = 0L
            val buffer = ByteArray(8192)

            resolved.outputStream().use { out ->
                input.use { inp ->
                    var bytesRead: Int
                    while (inp.read(buffer).also { bytesRead = it } != -1) {
                        out.write(buffer, 0, bytesRead)
                        downloaded += bytesRead
                        
                        // Update progress (throttle to avoid too many updates)
                        if (totalSize > 0 && downloaded % (totalSize / 20) < buffer.size) {
                            val progress = ((downloaded * 100) / totalSize).toInt()
                            notificationHelper.showProgressNotification(id.toString(), fileName, progress)
                            
                            // Update work progress
                            setProgress(workDataOf("progress" to progress))
                        }
                    }
                }
            }

            // After download, trigger auto-import
            AutoImportService.importFile(applicationContext, resolved.absolutePath)

            // Show success notification
            notificationHelper.showSuccessNotification(id.toString(), fileName)

            Result.success(workDataOf("filePath" to resolved.absolutePath))
        } catch (e: Exception) {
            Log.e(TAG, "Download failed", e)
            val fileName = inputData.getString("fileName") ?: "file"
            notificationHelper.showErrorNotification(id.toString(), fileName, e.message ?: "Unknown error")
            Result.retry()
        }
    }
}

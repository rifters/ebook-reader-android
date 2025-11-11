package com.rifters.ebookreader.sync

import android.content.Context
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.rifters.ebookreader.cloud.CloudFile

/**
 * Manager that enqueues downloads into WorkManager.
 */
object DownloadQueueManager {

    private const val QUEUE_NAME = "cloud_download_queue"

    fun enqueueDownload(context: Context, providerName: String, file: CloudFile, localDestination: String) {
        val data = Data.Builder()
            .putString("providerName", providerName)
            .putString("filePath", file.path)
            .putString("localDestination", localDestination)
            .build()

        val request = OneTimeWorkRequestBuilder<DownloadWorker>()
            .setInputData(data)
            .build()

        WorkManager.getInstance(context).enqueueUniqueWork(
            QUEUE_NAME,
            ExistingWorkPolicy.APPEND_OR_REPLACE,
            request
        )
    }
}

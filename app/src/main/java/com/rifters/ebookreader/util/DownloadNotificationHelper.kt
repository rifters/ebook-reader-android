package com.rifters.ebookreader.util

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.work.WorkManager
import com.rifters.ebookreader.MainActivity
import com.rifters.ebookreader.R

/**
 * Helper for creating and managing download notifications.
 * Shows progress, completion, and error states with cancel action.
 */
class DownloadNotificationHelper(private val context: Context) {

    private val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    
    companion object {
        const val CHANNEL_ID = "cloud_download_channel"
        const val CHANNEL_NAME = "Cloud Downloads"
        const val NOTIFICATION_ID_BASE = 5000
        
        const val ACTION_CANCEL_DOWNLOAD = "com.rifters.ebookreader.ACTION_CANCEL_DOWNLOAD"
    }

    init {
        createNotificationChannel()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Notifications for cloud file downloads"
                setShowBadge(false)
            }
            notificationManager.createNotificationChannel(channel)
        }
    }

    /**
     * Show progress notification for ongoing download
     */
    fun showProgressNotification(
        workId: String,
        fileName: String,
        progress: Int,
        max: Int = 100
    ): NotificationCompat.Builder {
        val notificationId = getNotificationId(workId)
        
        val cancelIntent = Intent(ACTION_CANCEL_DOWNLOAD).apply {
            putExtra("workId", workId)
        }
        val cancelPendingIntent = PendingIntent.getBroadcast(
            context,
            notificationId,
            cancelIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setContentTitle("Downloading")
            .setContentText(fileName)
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setProgress(max, progress, progress == 0)
            .setOngoing(true)
            .setAutoCancel(false)
            .addAction(
                android.R.drawable.ic_delete,
                "Cancel",
                cancelPendingIntent
            )

        notificationManager.notify(notificationId, builder.build())
        return builder
    }

    /**
     * Show success notification
     */
    fun showSuccessNotification(workId: String, fileName: String) {
        val notificationId = getNotificationId(workId)
        
        val intent = Intent(context, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            context,
            notificationId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setContentTitle("Download complete")
            .setContentText(fileName)
            .setSmallIcon(android.R.drawable.stat_sys_download_done)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)

        notificationManager.notify(notificationId, builder.build())
    }

    /**
     * Show error notification
     */
    fun showErrorNotification(workId: String, fileName: String, error: String) {
        val notificationId = getNotificationId(workId)

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setContentTitle("Download failed")
            .setContentText("$fileName: $error")
            .setSmallIcon(android.R.drawable.stat_notify_error)
            .setAutoCancel(true)

        notificationManager.notify(notificationId, builder.build())
    }

    /**
     * Cancel notification
     */
    fun cancelNotification(workId: String) {
        val notificationId = getNotificationId(workId)
        notificationManager.cancel(notificationId)
    }

    /**
     * Cancel download work
     */
    fun cancelDownload(workId: String) {
        WorkManager.getInstance(context).cancelWorkById(java.util.UUID.fromString(workId))
        cancelNotification(workId)
    }

    private fun getNotificationId(workId: String): Int {
        // Generate consistent notification ID from work ID
        return NOTIFICATION_ID_BASE + Math.abs(workId.hashCode() % 1000)
    }
}

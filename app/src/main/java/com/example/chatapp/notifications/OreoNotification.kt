package com.example.chatapp.notifications

import android.annotation.TargetApi
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.ContextWrapper
import android.os.Build

/**
 * To be used for notifications on devices with android version higher than "Oreo"
 */
class OreoNotification(base: Context) : ContextWrapper(base) {
    private var notificationManager: NotificationManager? = null

    companion object {
        private const val CHANNEL_ID = "com.example.chatapp"
        private const val CHANNEL_NAME = "chatapp"
    }

    init {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            createChannel()
        }
    }

    @TargetApi(Build.VERSION_CODES.O)
    private fun createChannel() {
        val channel = NotificationChannel(CHANNEL_ID, CHANNEL_NAME, NotificationManager.IMPORTANCE_DEFAULT)
        channel.enableLights(true)
        channel.enableVibration(true)
        channel.lockscreenVisibility = Notification.VISIBILITY_PRIVATE
        getManager().createNotificationChannel(channel)
    }

    fun getManager(): NotificationManager {
        if (notificationManager == null) {
            notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        }

        return getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    }

    @TargetApi(Build.VERSION_CODES.O)
    fun getOreoNotification(title: String, body: String, pendingIntent: PendingIntent, icon: Int): Notification.Builder {
        return Notification.Builder(applicationContext, CHANNEL_ID)
            .setContentIntent(pendingIntent)
            .setContentTitle(title)
            .setContentText(body)
            .setSmallIcon(icon)
            .setAutoCancel(true)
    }
}
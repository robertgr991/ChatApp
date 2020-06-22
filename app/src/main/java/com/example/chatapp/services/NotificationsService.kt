package com.example.chatapp.services

import android.app.NotificationManager
import android.content.Context
import com.example.chatapp.models.User

class NotificationsService(context: Context) {
    private val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    fun cancelNotificationFromUser(user: User) {
        val id = getNotificationId(user.id)
        notificationManager.cancel(id)
    }

    fun getNotificationId(userId: String): Int = (userId.filter { it.isDigit() }).toInt()
}
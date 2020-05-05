package com.example.chatapp.services

import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.RingtoneManager
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.chatapp.App
import com.example.chatapp.R
import com.example.chatapp.notifications.OreoNotification
import com.example.chatapp.repositories.ChatRepository
import com.example.chatapp.repositories.UserRepository
import com.example.chatapp.ui.chat.ChatLogActivity
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import org.koin.android.ext.android.inject

class MessagingServiceFirebase: FirebaseMessagingService() {
    private val userRepository: UserRepository by inject()
    private val chatRepository: ChatRepository by inject()
    private val notificationsService: NotificationsService by inject()

    override fun onNewToken(token: String) {
        super.onNewToken(token)

        if (App.context.currentUser != null) {
            userRepository.setDeviceToken(token)
        }
    }

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)

        // Don't show the notification if user is logged out
        if (App.context.currentUser != null) {
            sendNotification(message)
        }
    }

    private fun sendNotification(message: RemoteMessage) {
        val userId = message.data["userId"] ?: return
        val toId = message.data["to"] ?: return

        // Check if the received userId is valid
        userRepository.findById(userId) { user ->
            if (user != null && toId == App.context.currentUser!!.id) {
                // Don't show the notification if the current user is in conversation
                // with the user that send the notification
                chatRepository.getCurrentConversation { conversationUserId ->
                    if (conversationUserId != null && conversationUserId == user.id) {
                        return@getCurrentConversation
                    }


                    val title = message.data["title"]!!
                    val body = message.data["body"]!!
                    val requestCode = notificationsService.getNotificationId(userId)
                    val intent = Intent(this, ChatLogActivity::class.java)
                    intent.putExtra("user", user)
                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                    val pendingIntent = PendingIntent.getActivity(this, requestCode, intent, PendingIntent.FLAG_ONE_SHOT)
                    val icon = R.mipmap.ic_launcher
                    var notifyId = 0

                    if (requestCode > 0) {
                        notifyId = requestCode
                    }

                    // For version higher then "Oreo" you have to create a channel
                    // for the notifications
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        val oreoNotification = OreoNotification(this)
                        val builder = oreoNotification.getOreoNotification(title, body, pendingIntent, icon)
                        oreoNotification.getManager().notify(notifyId, builder.build())
                    } else {
                        val defaultSound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
                        val builder = NotificationCompat.Builder(this)
                            .setSmallIcon(icon)
                            .setContentTitle(title)
                            .setPriority(Notification.PRIORITY_DEFAULT)
                            .setContentText(body)
                            .setAutoCancel(true)
                            .setSound(defaultSound)
                            .setContentIntent(pendingIntent)
                        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                        notificationManager.notify(notifyId, builder.build())
                    }
                }
            }
        }
    }
}
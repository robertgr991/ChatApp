package com.example.chatapp.ui

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.core.content.ContextCompat.startActivity
import com.example.chatapp.ui.chat.LatestMessagesActivity
import com.example.chatapp.ui.chat.NewMessageActivity
import com.example.chatapp.ui.user.LoginActivity
import com.example.chatapp.ui.user.RegisterActivity

class ActivitiesManager {
    companion object {
        fun redirectToHomepage(context: Context) {
            val intent = Intent(context, LatestMessagesActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TASK.or(Intent.FLAG_ACTIVITY_NEW_TASK)
            startActivity(context, intent, Bundle())
        }

        fun redirectToLogin(context: Context) {
            val intent = Intent(context, LoginActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TASK.or(Intent.FLAG_ACTIVITY_NEW_TASK)
            startActivity(context, intent, Bundle())
        }

        fun redirectToRegister(context: Context) {
            val intent = Intent(context, RegisterActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TASK.or(Intent.FLAG_ACTIVITY_NEW_TASK)
            startActivity(context, intent, Bundle())
        }

        fun redirectToNewMessage(context: Context) {
            val intent = Intent(context, NewMessageActivity::class.java)
            startActivity(context, intent, Bundle())
        }
    }
}
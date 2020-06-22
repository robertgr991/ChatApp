package com.example.chatapp.ui

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.core.content.ContextCompat.startActivity
import com.example.chatapp.models.User
import com.example.chatapp.ui.chat.ChatLogActivity
import com.example.chatapp.ui.chat.LatestMessagesActivity
import com.example.chatapp.ui.chat.NewMessageActivity
import com.example.chatapp.ui.user.LoginActivity
import com.example.chatapp.ui.user.ProfileActivity
import com.example.chatapp.ui.user.RegisterActivity

/**
 * Used to redirect between activities
 */
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

        fun redirectToChatWithUser(context: Context, user: User) {
            val intent = Intent(context, ChatLogActivity::class.java)
            intent.putExtra("user", user)
            startActivity(context, intent, Bundle())
        }

        fun redirectToProfile(context: Context, userProfile: User) {
            val intent = Intent(context, ProfileActivity::class.java)
            intent.putExtra("user", userProfile)
            startActivity(context, intent, Bundle())
        }
    }
}
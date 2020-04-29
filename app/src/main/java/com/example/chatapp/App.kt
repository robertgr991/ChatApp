package com.example.chatapp

import android.app.Application
import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.core.content.ContextCompat
import androidx.core.content.ContextCompat.startActivity
import com.example.chatapp.models.User
import com.example.chatapp.ui.chat.LatestMessagesActivity
import com.example.chatapp.ui.user.LoginActivity
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.startKoin

class App : Application(){
    var currentUser: User? = null

    companion object {
        lateinit var context: App
    }

    override fun onCreate() {
        super.onCreate()
        // Start Koin
        startKoin{
            androidLogger()
            androidContext(this@App)
            modules(appModule)
        }
        context = this
    }
}
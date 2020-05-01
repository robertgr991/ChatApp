package com.example.chatapp

import android.app.Application
import android.util.Log
import com.example.chatapp.events.eventsModule
import com.example.chatapp.models.User
import com.example.chatapp.repositories.repositoriesModule
import com.example.chatapp.services.servicesModule
import com.example.chatapp.ui.uiModule
import com.example.chatapp.validators.validatorsModule
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.startKoin

class App : Application() {
    var currentUser: User? = null

    companion object {
        lateinit var context: App
    }

    override fun onCreate() {
        super.onCreate()
        Log.d("App", "Application STARTED")
        // Start Koin
        startKoin{
            androidLogger()
            androidContext(this@App)
            modules(
                appModule,
                servicesModule,
                repositoriesModule,
                uiModule,
                validatorsModule,
                eventsModule
            )
        }
        context = this
    }
}
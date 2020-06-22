package com.example.chatapp

import android.app.Application
import android.util.Log
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.OnLifecycleEvent
import androidx.lifecycle.ProcessLifecycleOwner
import com.example.chatapp.events.eventsModule
import com.example.chatapp.models.User
import com.example.chatapp.repositories.repositoriesModule
import com.example.chatapp.services.UserService
import com.example.chatapp.services.servicesModule
import com.example.chatapp.ui.uiModule
import com.example.chatapp.validators.validatorsModule
import org.koin.android.ext.android.inject
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.startKoin


class App : Application(), LifecycleObserver {
    private val userService: UserService by inject()
    var currentUser: User? = null
    var hasSetToken: Boolean = false

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
        ProcessLifecycleOwner.get().lifecycle.addObserver(this);
    }


    @OnLifecycleEvent(Lifecycle.Event.ON_STOP)
    private fun onAppBackgrounded() {
        userService.setStatus("offline")
        Log.d("APPLICATION", "App in background")
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_DESTROY)
    private fun onAppDestroyed() {
        userService.setStatus("offline")
        Log.d("APPLICATION", "App destroyed")
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_START)
    private fun onAppForegrounded() {
        userService.setStatus("online")
        Log.d("APPLICATION", "App in foreground")
    }
}
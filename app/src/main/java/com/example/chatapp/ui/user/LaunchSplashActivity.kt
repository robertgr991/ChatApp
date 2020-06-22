package com.example.chatapp.ui.user

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import com.example.chatapp.R
import com.example.chatapp.services.UserService
import com.example.chatapp.ui.ActivitiesManager
import org.koin.android.ext.android.inject

/**
 * Launcher activity
 *
 * If user is logged, redirect to homepage, otherwise
 * redirect to register
 */
class LaunchSplashActivity : AppCompatActivity() {
    private val userService: UserService by inject()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_launch_splash)

        if (userService.isLoggedIn()) {
            userService.getCurrent {
                if (it == null) {
                    ActivitiesManager.redirectToRegister(this)
                } else {
                    ActivitiesManager.redirectToHomepage(this)
                }
            }
        } else {
            ActivitiesManager.redirectToRegister(this)
        }
    }
}

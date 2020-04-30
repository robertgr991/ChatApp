package com.example.chatapp.ui.chat

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import com.example.chatapp.R
import com.example.chatapp.services.UserService
import com.example.chatapp.ui.ActivitiesManager
import org.koin.android.ext.android.inject
import org.koin.java.KoinJavaComponent.inject

class LatestMessagesActivity : AppCompatActivity() {
    private val userService: UserService by inject()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_latest_messages)
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        when (item?.itemId) {
            R.id.menu_new_message -> {
                ActivitiesManager.redirectToNewMessage(this)
            }
            R.id.menu_sign_out -> {
                userService.signOut()
                ActivitiesManager.redirectToLogin(this)
            }
        }

        return super.onOptionsItemSelected(item)
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.nav_menu, menu)
        return super.onCreateOptionsMenu(menu)
    }
}

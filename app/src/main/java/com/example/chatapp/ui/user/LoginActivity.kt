package com.example.chatapp.ui.user

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.example.chatapp.App
import com.example.chatapp.R
import com.example.chatapp.models.dto.UserLoginDTO
import com.example.chatapp.services.UserService
import com.example.chatapp.ui.ActivitiesManager
import com.example.chatapp.ui.chat.LatestMessagesActivity
import com.example.chatapp.ui.notifiers.ToastNotifier
import kotlinx.android.synthetic.main.activity_login.*
import kotlinx.android.synthetic.main.activity_register.*
import org.koin.android.ext.android.inject

class LoginActivity : AppCompatActivity() {
    private val userService: UserService by inject()
    private val toastNotifier: ToastNotifier by inject()
    private var isLoginDisabled = false

    override fun onCreate(savedInstanceState: Bundle?) {
        if (App.context.currentUser != null) {
            ActivitiesManager.redirectToHomepage(this)
        }

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        login_btn_login.setOnClickListener {
            if (isLoginDisabled) {
                return@setOnClickListener
            }

            val email = login_txt_email.text.toString()
            val password = login_txt_password.text.toString()
            val loginUser = UserLoginDTO(email, password)

            isLoginDisabled = true
            login_btn_login.background = this.getDrawable(R.drawable.rounded_btn_disabled)
            userService.signIn(loginUser) {
                // Successful login
                if (it == null || it == "") {
                    userService.getCurrent() { loggedUser ->
                        if (loggedUser != null) {
                            ActivitiesManager.redirectToHomepage(this)
                        } else {
                            toastNotifier.notify(this, "There was an error, try again later", toastNotifier.lengthLong)
                        }
                    }
                } else {
                    // Show errors
                    toastNotifier.notify(this, it, toastNotifier.lengthLong)
                    isLoginDisabled = false
                    login_btn_login.background = this.getDrawable(R.drawable.rounded_btn_accent)
                }
            }
        }

        login_txt_back.setOnClickListener {
           ActivitiesManager.redirectToRegister(this)
        }
    }
}
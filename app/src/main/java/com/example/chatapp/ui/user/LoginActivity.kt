package com.example.chatapp.ui.user

import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.example.chatapp.R
import com.example.chatapp.models.dto.UserLoginDTO
import com.example.chatapp.services.UserService
import com.example.chatapp.ui.notifiers.ToastNotifier
import kotlinx.android.synthetic.main.activity_login.*
import org.koin.android.ext.android.inject

class LoginActivity : AppCompatActivity() {
    private val userService: UserService by inject()
    private val toastNotifier: ToastNotifier by inject()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        login_btn_login.setOnClickListener {
            val email = login_txt_email.text.toString()
            val password = login_txt_password.text.toString()
            val loginUser = UserLoginDTO(email, password)

            userService.signIn(loginUser) {
                // Successful login
                if (it == null || it == "") {
                    Log.d("login", "success")
                } else {
                    toastNotifier.notify(this, it, toastNotifier.lengthLong)
                }
            }
        }

        login_txt_back.setOnClickListener {
            finish()
        }
    }
}
package com.example.chatapp.ui.user

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.example.chatapp.R
import com.example.chatapp.models.dto.CreateUserDTO
import com.example.chatapp.services.UserService
import com.example.chatapp.ui.notifiers.ToastNotifier
import com.example.chatapp.validators.CreateUserValidator
import kotlinx.android.synthetic.main.activity_register.*
import org.koin.android.ext.android.inject


class RegisterActivity : AppCompatActivity() {
    private val userService: UserService by inject()
    private val toastNotifier: ToastNotifier by inject()
    private val createUserValidator: CreateUserValidator by inject()
    private var selectedPhotoUri: Uri? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)

        val logTag = "RegisterActivity"
        register_btn_select_photo.setOnClickListener {
            val intent = Intent(Intent.ACTION_PICK)
            intent.type = "image/*"
            startActivityForResult(intent, 0)
        }

        register_btn_register.setOnClickListener {
            val username = register_txt_username.text.toString()
            val email = register_txt_email.text.toString()
            val password = register_txt_password.text.toString()
            val user = CreateUserDTO(username, email, password)
            val validation = createUserValidator.validate(user)

            if (!validation.status) {
                toastNotifier.notify(this, validation.messages[0], toastNotifier.lengthLong)
                return@setOnClickListener
            }

            userService.create(user, selectedPhotoUri) {
                // Successful register
                if (it == null || it == "") {
                    Log.d("register", "success")
                } else {
                    toastNotifier.notify(this, it, toastNotifier.lengthLong)
                }
            }
        }

        register_txt_already_registered.setOnClickListener {
            Log.d(logTag, "Already registered")
            val intent = Intent(this, LoginActivity::class.java)
            startActivity(intent)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == 0 && resultCode == Activity.RESULT_OK && data != null) {
            // Selected image
            selectedPhotoUri = data.data
            Log.d("photo", selectedPhotoUri.toString())
            val bitmap = MediaStore.Images.Media.getBitmap(contentResolver, selectedPhotoUri)
            register_imgview_select_photo.setImageBitmap(bitmap)
            register_btn_select_photo.alpha = 0f
//            val bitmapDrawable = BitmapDrawable(this.resources, bitmap)
//            register_btn_select_photo.background = bitmapDrawable
        }
    }
}

package com.example.chatapp.ui.user

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import androidx.appcompat.app.AppCompatActivity
import com.example.chatapp.App
import com.example.chatapp.R
import com.example.chatapp.models.dto.CreateUserDTO
import com.example.chatapp.services.UserService
import com.example.chatapp.ui.ActivitiesManager
import com.example.chatapp.ui.notifiers.ToastNotifier
import com.example.chatapp.ui.utils.ProgressDialog
import com.example.chatapp.utils.Utils
import com.example.chatapp.validators.CreateUserValidator
import kotlinx.android.synthetic.main.activity_register.*
import org.koin.android.ext.android.inject


class RegisterActivity : AppCompatActivity() {
    private val userService: UserService by inject()
    private val toastNotifier: ToastNotifier by inject()
    private val createUserValidator: CreateUserValidator by inject()
    private lateinit var progressDialog: ProgressDialog
    private var selectedPhotoUri: Uri? = null
    private var isRegisterDisabled = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)

        // Set progress dialog
        progressDialog = ProgressDialog(this)

        // Select profile photo
        register_btn_select_photo.setOnClickListener {
            val intent = Intent(Intent.ACTION_PICK)
            intent.type = "image/*"
            startActivityForResult(intent, 0)
        }

        register_take_picture_btn.setOnClickListener{
            var i=Intent(MediaStore.ACTION_IMAGE_CAPTURE)
            startActivityForResult(i,123)
        }

        register_btn_register.setOnClickListener {
            if (isRegisterDisabled) {
                return@setOnClickListener
            }

            val username = register_txt_username.text.toString()
            val email = register_txt_email.text.toString()
            val password = register_txt_password.text.toString()
            val user = CreateUserDTO(username, email, password)
            val validation = createUserValidator.validate(user)

            // Check if input is valid
            if (!validation.status) {
                toastNotifier.notify(this, validation.messages[0], toastNotifier.lengthLong)
                return@setOnClickListener
            }

            // Disable the button until process finishes
            isRegisterDisabled = true
            register_btn_register.background = this.getDrawable(R.drawable.rounded_btn_disabled)
            progressDialog.show()
            userService.create(user, selectedPhotoUri) {
                progressDialog.cancel()
                // Successful register, redirect to homepage
                if (it == null || it == "") {
                    ActivitiesManager.redirectToHomepage(this)
                } else {
                    // Show errors
                    toastNotifier.notify(this, it, toastNotifier.lengthLong)
                    isRegisterDisabled = false
                    register_btn_register.background = this.getDrawable(R.drawable.rounded_btn_accent)
                }
            }
        }

        register_txt_already_registered.setOnClickListener {
            ActivitiesManager.redirectToLogin(this)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == 0 && resultCode == Activity.RESULT_OK && data != null) { // Check if image was selected from gallery
            // Selected photo
            selectedPhotoUri = data.data
            val bitmap = MediaStore.Images.Media.getBitmap(contentResolver, selectedPhotoUri)
            register_imgview_select_photo.setImageBitmap(bitmap)
            register_btn_select_photo.alpha = 0f
        }

        if(requestCode == 123 && data != null) { // Check if image was taken with the camera
            var bmp = data.extras.get("data") as Bitmap
            selectedPhotoUri = Utils.getImageUriFromBitmap(App.context,bmp)
            register_imgview_select_photo.setImageBitmap(bmp)
            register_btn_select_photo.alpha = 0f
        }
    }
}

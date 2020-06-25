package com.example.chatapp.ui.user

import android.Manifest
import android.app.Activity
import android.content.*
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Typeface
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.os.Parcelable
import android.provider.MediaStore
import android.util.Log
import android.view.MenuItem
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.bumptech.glide.Glide
import com.example.chatapp.App
import com.example.chatapp.R
import com.example.chatapp.models.User
import com.example.chatapp.models.dto.UpdateUserDTO
import com.example.chatapp.services.UserService
import com.example.chatapp.ui.ActivitiesManager
import com.example.chatapp.ui.notifiers.ToastNotifier
import com.example.chatapp.ui.utils.AlertDialogBuilder
import com.example.chatapp.ui.utils.ProgressDialog
import com.example.chatapp.utils.Utils
import com.example.chatapp.validators.UpdateUserValidator
import kotlinx.android.parcel.Parcelize
import kotlinx.android.synthetic.main.activity_profile.*
import org.koin.android.ext.android.inject
import java.io.ByteArrayOutputStream
import java.io.File
import kotlin.properties.Delegates

/**
 * Used to save the state between orientation changes
 */
@Parcelize
data class ProfileOrientationState(var hasTouchedImage: Boolean = false, var isSaveEnabled: Boolean = true, var newImageUri: Uri? = null) :
    Parcelable {}

class ProfileActivity : AppCompatActivity() {
    private val updateUserValidator: UpdateUserValidator by inject()
    private val toastNotifier: ToastNotifier by inject()
    private val userService: UserService by inject()
    private lateinit var progressDialog: ProgressDialog
    private lateinit var userProfile: User
    private var newImageUri: Uri? = null
    private var hasTouchedImage: Boolean = false
    private var isSaveEnabled: Boolean = true
    private var isBlocked by Delegates.notNull<Boolean>()

    companion object {
        var newUser: User? = null
        private const val STATE_KEY = "STATE_KEY"
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        // Save changes before orientation change
        outState.putParcelable(STATE_KEY, ProfileOrientationState(hasTouchedImage, isSaveEnabled, newImageUri))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        if (App.context.currentUser == null) {
            ActivitiesManager.redirectToLogin(this)
        }

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profile)

        // Retrieve the modifications made before the orientation change
        if (savedInstanceState != null) {
            val savedState = savedInstanceState.getParcelable<ProfileOrientationState>(STATE_KEY)

            if (savedState != null) {
                newImageUri = savedState.newImageUri
                hasTouchedImage = savedState.hasTouchedImage
                isSaveEnabled = savedState.isSaveEnabled
            }
        }

        // Set progress dialog
        progressDialog = ProgressDialog(this)

        // Add details on action bar
        if (supportActionBar != null) {
            supportActionBar?.setDisplayHomeAsUpEnabled(true)
            supportActionBar?.setDisplayShowHomeEnabled(true)

            userProfile = if (intent.hasExtra("user")) {
                intent.getParcelableExtra("user")
            } else {
                App.context.currentUser!!
            }

            if (newUser != null) {
                userProfile = newUser as User
            }
        }

        loadCommonProfile()

        if (userProfile.id == App.context.currentUser!!.id) {
            loadOwnProfile()
        } else {
            loadUserProfile()
        }
    }

    private fun loadCommonProfile() {
        supportActionBar?.title = "${userProfile.username}'s profile"
        profile_btn_block_unblock.visibility = View.GONE

        if (userProfile.imageName != null) {
            Glide.with(this).load(userProfile.imageName).into(profile_imgview_image)

        } else {
            Glide.with(this).load(R.drawable.default_avatar).into(profile_imgview_image)
        }
    }

    private fun loadUserProfile() {
        // Hide editable
        profile_txt_bio_own.visibility = View.GONE
        profile_txt_email_title.visibility = View.GONE
        profile_txt_email_own.visibility = View.GONE
        profile_divider_2.visibility = View.GONE
        profile_btn_save_changes.visibility = View.GONE
        profile_btn_remove_image.visibility = View.GONE
        profile_divider_2_own.visibility = View.GONE
        profile_take_picture.visibility = View.GONE

        if (userProfile.bio == null) {
            profile_txt_bio.text = getString(R.string.profile_user_no_bio)
            profile_txt_bio.setTypeface(profile_txt_bio.typeface, Typeface.BOLD)
        } else {
            profile_txt_bio.text = userProfile.bio
        }

        val alertBlock = AlertDialogBuilder.positiveNegativeDialog(
            this,
            getString(R.string.profile_confirm_block_title),
            getString(R.string.profile_confirm_block_text),
            "YES",
            DialogInterface.OnClickListener { dialog, _ ->
                userService.blockUser(userProfile)
                isBlocked = true
                profile_btn_block_unblock.text = getString(R.string.profile_unblock_text)
                dialog.dismiss()
            },
            "NO",
            DialogInterface.OnClickListener { dialog, _ ->
                // Just dismiss the dialog
                dialog.dismiss()
            }
        )
        val alertUnblock = AlertDialogBuilder.positiveNegativeDialog(
            this,
            getString(R.string.profile_confirm_block_title),
            getString(R.string.profile_confirm_unblock_text),
            "YES",
            DialogInterface.OnClickListener { dialog, _ ->
                userService.unBlockUser(userProfile)
                isBlocked = false
                profile_btn_block_unblock.text = getString(R.string.profile_block_text)
                dialog.dismiss()
            },
            "NO",
            DialogInterface.OnClickListener { dialog, _ ->
                // Just dismiss the dialog
                dialog.dismiss()
            }
        )

        userService.isUserBlocked(userProfile) { result ->
            isBlocked = result
            profile_btn_block_unblock.text = if (result) getString(R.string.profile_unblock_text) else getString(R.string.profile_block_text)
            profile_btn_block_unblock.setOnClickListener {
                if (isBlocked) {
                    alertUnblock.show()
                } else {
                    alertBlock.show()
                }
            }
        }
    }

    private fun loadOwnProfile() {
        //Hide
        profile_divider_2.visibility = View.GONE
        profile_txt_bio.visibility = View.GONE

        if (userProfile.imageName == null) {
            profile_btn_remove_image.visibility = View.GONE
        }

        profile_txt_bio_own.setText(userProfile.bio ?: "")
        profile_txt_email_own.text = userProfile.email

        profile_imgview_image.setOnClickListener {
            val intent = Intent(Intent.ACTION_PICK)
            intent.type = "image/*"
            startActivityForResult(intent, 0)
        }

        profile_take_picture.setOnClickListener {
            ActivityCompat.requestPermissions(this,
                arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE),
                122)
        }

        profile_btn_remove_image.setOnClickListener {
            profile_btn_remove_image.visibility = View.GONE
            hasTouchedImage = true
            newImageUri = null
            Glide.with(this).load(R.drawable.default_avatar).into(profile_imgview_image)
        }

        profile_btn_save_changes.setOnClickListener {
            if (!isSaveEnabled()) {
                return@setOnClickListener
            }

            disableSave()
            val newBio = profile_txt_bio_own.text.toString()
            val updateUser = UpdateUserDTO(hasTouchedImage, newBio, newImageUri)
            val validate = updateUserValidator.validate(updateUser)

            if (!validate.status) {
                toastNotifier.notify(this, validate.messages[0], toastNotifier.lengthLong)
                return@setOnClickListener
            }

            progressDialog.show()
            userService.update(updateUser) {
                progressDialog.cancel()
                enableSave()

                if (it == null || it.isEmpty()) {
                    newUser = App.context.currentUser!!
                } else {
                    // Show the error
                    toastNotifier.notify(this, it, toastNotifier.lengthLong)
                }
            }
        }

        // Check if there are changes made before an orientation change
        if (hasTouchedImage) {
            if (newImageUri == null) {
                profile_btn_remove_image.visibility = View.GONE
                Glide.with(this).load(R.drawable.default_avatar).into(profile_imgview_image)
            } else {
                Log.d("ABC", newImageUri.toString())
                val bitmap = MediaStore.Images.Media.getBitmap(contentResolver, newImageUri)
                profile_imgview_image.setImageBitmap(bitmap)

                if (profile_btn_remove_image.visibility == View.GONE) {
                    profile_btn_remove_image.visibility = View.VISIBLE
                }
            }
        }
    }

    private fun isSaveEnabled(): Boolean {
        return isSaveEnabled
    }

    private fun enableSave() {
        isSaveEnabled = true
        profile_btn_save_changes.background = getDrawable(R.drawable.rounded_btn_accent)
    }

    private fun disableSave() {
        isSaveEnabled = true
        profile_btn_save_changes.background = getDrawable(R.drawable.rounded_btn_disabled)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == 0 && resultCode == Activity.RESULT_OK && data != null) {
            // Selected image
            hasTouchedImage = true
            newImageUri = data.data
            val bitmap = MediaStore.Images.Media.getBitmap(contentResolver, newImageUri)
            profile_imgview_image.setImageBitmap(bitmap)

            if (profile_btn_remove_image.visibility == View.GONE) {
                profile_btn_remove_image.visibility = View.VISIBLE
            }
        }

        if (requestCode == 123 && resultCode == Activity.RESULT_OK && data != null) {
            // Selected image
            hasTouchedImage = true
            //newImageUri = data.extras.get("data") as Uri

            var bmp = data.extras.get("data") as Bitmap
            newImageUri = Utils.getImageUriFromBitmap(App.context,bmp)
            //val bitmap = MediaStore.Images.Media.getBitmap(contentResolver, newImageUri)
            profile_imgview_image.setImageBitmap(bmp)

            if (profile_btn_remove_image.visibility == View.GONE) {
                profile_btn_remove_image.visibility = View.VISIBLE
            }
        }
    }

    private fun onBack() {
        // Go to homepage if user is viewing it's own profile
        if (App.context.currentUser!!.id == userProfile.id) {
            ActivitiesManager.redirectToHomepage(this)
        } else {
            // Otherwise go to chat with that user
            ActivitiesManager.redirectToChatWithUser(this, userProfile)
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Back button
        if (item.itemId == android.R.id.home) {
            onBack()
        }

        return super.onOptionsItemSelected(item)
    }

    override fun onBackPressed() {
        super.onBackPressed()
        onBack()
    }

    override fun onDestroy() {
        super.onDestroy()
        newUser = null
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        if(requestCode == 122) {
            if (!grantResults.isEmpty()  && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                var i=Intent(MediaStore.ACTION_IMAGE_CAPTURE)
                startActivityForResult(i,123)
            }
        }
    }
}

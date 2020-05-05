package com.example.chatapp.ui.user

import android.app.Activity
import android.content.DialogInterface
import android.content.Intent
import android.graphics.Typeface
import android.net.Uri
import android.opengl.Visibility
import android.os.Bundle
import android.provider.MediaStore
import android.view.MenuItem
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.example.chatapp.App
import com.example.chatapp.R
import com.example.chatapp.models.User
import com.example.chatapp.models.dto.UpdateUserDTO
import com.example.chatapp.services.UserService
import com.example.chatapp.ui.ActivitiesManager
import com.example.chatapp.ui.notifiers.ToastNotifier
import com.example.chatapp.ui.utils.AlertDialogBuilder
import com.example.chatapp.validators.UpdateUserValidator
import kotlinx.android.synthetic.main.activity_profile.*
import org.koin.android.ext.android.inject
import kotlin.properties.Delegates

class ProfileActivity : AppCompatActivity() {
    private val updateUserValidator: UpdateUserValidator by inject()
    private val toastNotifier: ToastNotifier by inject()
    private val userService: UserService by inject()
    private lateinit var userProfile: User
    private var newImageUri: Uri? = null
    private var hasTouchedImage: Boolean = false
    private var isSaveEnabled: Boolean = true
    private var isBlocked by Delegates.notNull<Boolean>()

    companion object {
        var newUser: User? = null
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        if (App.context.currentUser == null) {
            ActivitiesManager.redirectToLogin(this)
        }

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profile)

        // Add details on action bar
        if (supportActionBar != null) {
            supportActionBar?.setDisplayHomeAsUpEnabled(true);
            supportActionBar?.setDisplayShowHomeEnabled(true);

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

            userService.update(updateUser) {
                enableSave()

                if (it == null || it.isEmpty()) {
                    newUser = App.context.currentUser!!
                } else {
                    // Show the error
                    toastNotifier.notify(this, it, toastNotifier.lengthLong)
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
}

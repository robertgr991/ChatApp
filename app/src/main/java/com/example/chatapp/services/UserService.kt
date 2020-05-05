package com.example.chatapp.services

import android.net.Uri
import android.util.Log
import com.example.chatapp.App
import com.example.chatapp.models.User
import com.example.chatapp.models.dto.CreateUserDTO
import com.example.chatapp.models.dto.UpdateUserDTO
import com.example.chatapp.models.dto.UserLoginDTO
import com.example.chatapp.repositories.UserRepository
import com.google.firebase.auth.ktx.auth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.ktx.Firebase
import org.koin.core.KoinComponent
import org.koin.core.inject

class UserService: KoinComponent {
    private val auth = Firebase.auth
    private val userRepository: UserRepository by inject()

    fun blockUser(user: User) {
        userRepository.blockUser(user)
    }

    fun unBlockUser(user: User) {
        userRepository.unBlockUser(user)
    }

    fun isUserBlocked(user: User, callback: (Boolean) -> Unit) {
        userRepository.isUserBlocked(user, callback)
    }

    fun getCurrentToken(callback: (String?) -> Unit) {
        userRepository.getCurrentToken(callback)
    }

    fun setDeviceToken(token: String) {
        userRepository.setDeviceToken(token)
    }

    fun findById(id: String, callback: (User?) -> Unit) {
        userRepository.findById(id, callback)
    }

    fun isLoggedIn(): Boolean {
        return auth.currentUser != null
    }

    fun getAll(callback: (ArrayList<User>) -> Unit) {
            userRepository.getAll(callback)
    }

    fun getCurrent(callback: (User?) -> Unit) {
        if (auth.currentUser == null) {
            callback(null)
            return
        }

        userRepository.getCurrent(callback)
    }

    fun update(updateUser: UpdateUserDTO, callback:(String?) -> Unit) {
         fun setUserBioAndPersist() {
             App.context.currentUser!!.bio = if (updateUser.bio?.isBlank()!!) {
                 null
             } else {
                 updateUser.bio
             }
             userRepository.persist(App.context.currentUser!!) { result ->
                 callback(result)
             }
         }

        if (App.context.currentUser == null) {
            return
        }

        if (updateUser.hasTouchedImage) {
            if (App.context.currentUser!!.imageName != null) {
                userRepository.removeProfileImage(App.context.currentUser?.imageName!!)
                App.context.currentUser!!.imageName = null
            }

            if (updateUser.newImageUri != null) {
                userRepository.setProfileImage(updateUser.newImageUri) {
                    App.context.currentUser!!.imageName = it
                    setUserBioAndPersist()
                }
            } else {
                setUserBioAndPersist()
            }
        } else {
            setUserBioAndPersist()
        }
    }

    fun create(user: CreateUserDTO, selectedPhotoUri: Uri?, callback: (String?) -> Unit) {
        // Check if username already exists
        userRepository.findByUsername(user.username) { exists ->
            if (exists) {
                callback("Username already exists")
            } else {
                userRepository.create(user, selectedPhotoUri, callback)
            }
        }
    }

    fun signIn(loginUser: UserLoginDTO, callback: (String?) -> Unit) {
        userRepository.signIn(loginUser, callback)
    }

    fun signOut() {
        userRepository.signOut()
    }

    fun setStatus(status: String) {
        if ((status == "online" || status == "offline") && App.context.currentUser != null) {
            userRepository.setStatus(status)
        }
    }
}

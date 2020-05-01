package com.example.chatapp.services

import android.net.Uri
import com.example.chatapp.App
import com.example.chatapp.models.User
import com.example.chatapp.models.dto.CreateUserDTO
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

    fun findById(id: String, callback: (User?) -> Unit) {
        userRepository.findById(id, callback)
    }

    fun isLoggedIn(): Boolean {
        return auth.currentUser != null
    }

    fun getAll(callback: (ArrayList<User>) -> Unit) {
            userRepository.getAll(callback)
    }

    fun getCurrent(callback: () -> Unit) {
        if (auth.uid == null) {
            return
        }

        userRepository.getCurrent(callback)
    }

    fun create(user: CreateUserDTO, selectedPhotoUri: Uri?, callback: (String?) -> Unit) {
        userRepository.create(user, selectedPhotoUri, callback)
    }

    fun signIn(loginUser: UserLoginDTO, callback: (String?) -> Unit) {
        userRepository.signIn(loginUser, callback)
    }

    fun signOut() {
        userRepository.signOut()
        App.context.currentUser = null
    }
}

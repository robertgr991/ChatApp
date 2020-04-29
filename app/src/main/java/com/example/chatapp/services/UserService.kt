package com.example.chatapp.services

import android.net.Uri
import com.example.chatapp.models.dto.CreateUserDTO
import com.example.chatapp.models.dto.UserLoginDTO
import com.example.chatapp.repositories.UserRepository
import org.koin.core.KoinComponent
import org.koin.core.inject

class UserService: KoinComponent {
    private val userRepository: UserRepository by inject()

    fun create(user: CreateUserDTO, selectedPhotoUri: Uri?, callback: (String?) -> Unit) {
        userRepository.create(user, selectedPhotoUri, callback)
    }

    fun signIn(loginUser: UserLoginDTO, callback: (String?) -> Unit) {
        userRepository.signIn(loginUser, callback)
    }
}

package com.example.chatapp.repositories

import android.net.Uri
import com.example.chatapp.models.User
import com.example.chatapp.models.dto.CreateUserDTO
import com.example.chatapp.models.dto.UserLoginDTO
import com.google.firebase.auth.ktx.auth
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.ktx.storage
import java.util.*

class UserRepository {
    private val auth = Firebase.auth
    private val storage = Firebase.storage
    private val database = Firebase.database

    fun create(user: CreateUserDTO, selectedPhotoUri: Uri?, callback: (String?) -> Unit) {
        auth.createUserWithEmailAndPassword(user.email, user.password)
            .addOnSuccessListener {
                val userId = it.user?.uid ?: ""

                if (selectedPhotoUri != null) {
                    val fileName: String? = UUID.randomUUID().toString()
                    val imageRef = storage.getReference("/images/$fileName")
                    imageRef.putFile(selectedPhotoUri)
                        .addOnSuccessListener {
                            imageRef.downloadUrl
                                .addOnSuccessListener {imageUrl ->
                                    if (imageUrl != null) {
                                        persist(User(userId, user.username, user.email, imageUrl.toString())) { message ->
                                            callback(message)
                                        }
                                    }
                                }
                                .addOnFailureListener {
                                    persist(User(userId, user.username, user.email)) { message ->
                                        callback(message)
                                    }
                                }
                        }
                        .addOnFailureListener {
                            persist(User(userId, user.username, user.email)) { message ->
                                callback(message)
                            }
                        }
                } else {
                    persist(User(userId, user.username, user.email)) { message ->
                        callback(message)
                    }
                }
            }
            .addOnFailureListener {
                callback(it.message)
            }
    }

    fun signIn(loginUser: UserLoginDTO, callback: (String?) -> Unit) {
        auth.signInWithEmailAndPassword(loginUser.email, loginUser.password)
            .addOnSuccessListener {
                callback("")
            }
            .addOnFailureListener {
                callback("Invalid username or password")
            }
    }

    fun persist(user: User, callback: (String?) -> Unit) {
        val ref = database.getReference("/users/${user.id}")
        ref.setValue(user)
            .addOnSuccessListener {
                callback("")
            }
            .addOnFailureListener {
                callback(it.message)
            }
    }
}
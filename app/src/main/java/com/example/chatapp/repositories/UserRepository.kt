package com.example.chatapp.repositories

import android.net.Uri
import android.util.Log
import com.example.chatapp.App
import com.example.chatapp.models.User
import com.example.chatapp.models.dto.CreateUserDTO
import com.example.chatapp.models.dto.UserLoginDTO
import com.google.firebase.auth.ktx.auth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.ktx.storage
import java.util.*
import kotlin.collections.ArrayList

class UserRepository {
    private val auth = Firebase.auth
    private val storage = Firebase.storage
    private val database = Firebase.database

    fun getAll(callback: (ArrayList<User>) -> Unit) {
        val ref = database.getReference("/users")
        ref.addValueEventListener(object: ValueEventListener {
            override fun onCancelled(error: DatabaseError) {}

            override fun onDataChange(snapshot: DataSnapshot) {
                val users = ArrayList<User>()

                snapshot.children.forEach {
                    val user = it.getValue(User::class.java)
                    Log.d("user id", user?.id)
                    Log.d("currentuser", App.context.currentUser.toString())

                    if (user != null && user.id != App.context.currentUser?.id ?: "") {
                        users.add(user)
                    }
                }

                callback(users)
            }

        })
    }

    fun getCurrent() {
        // Query current user
        database
            .reference
            .child("/users")
            .orderByChild("id")
            .equalTo(auth.uid)
            .addValueEventListener(object: ValueEventListener {
                override fun onCancelled(error: DatabaseError) {}

                override fun onDataChange(snapshot: DataSnapshot) {
                    snapshot.children.forEach {
                        val user = it.getValue(User::class.java)

                        App.context.currentUser = user
                    }
                }

            })
    }

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
                App.context.currentUser = user
                callback("")
            }
            .addOnFailureListener {
                callback(it.message)
            }
    }

    fun signOut() {
        auth.signOut()
    }
}
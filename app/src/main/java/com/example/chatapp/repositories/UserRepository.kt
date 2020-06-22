package com.example.chatapp.repositories

import android.net.Uri
import android.util.Log
import com.example.chatapp.App
import com.example.chatapp.models.User
import com.example.chatapp.models.dto.CreateUserDTO
import com.example.chatapp.models.dto.UserLoginDTO
import com.google.android.gms.tasks.OnCompleteListener
import com.google.firebase.auth.ktx.auth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.ktx.database
import com.google.firebase.iid.FirebaseInstanceId
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.ktx.storage
import java.util.*
import kotlin.collections.ArrayList

class UserRepository {
    private val auth = Firebase.auth
    private val storage = Firebase.storage
    private val database = Firebase.database
    private val baseBlock = "/block"
    private val baseUsers = "/users"
    private val baseImages = "/images"

    fun isBlockedBy(user: User, byUser: User, callback: (Boolean) -> Unit) {
        database
            .getReference("${baseBlock}/${byUser.id}/${user.id}")
            .addListenerForSingleValueEvent(object: ValueEventListener {
                override fun onCancelled(p0: DatabaseError) {
                    callback(false)
                }

                override fun onDataChange(snapshot: DataSnapshot) {
                    val isBlocked = snapshot.getValue(String::class.java) != null
                    callback(isBlocked)
                }
            })
    }

    fun blockUser(user: User) {
        database
            .getReference("${baseBlock}/${App.context.currentUser!!.id}/${user.id}")
            .setValue("blocked")
    }

    fun unBlockUser(user: User) {
        database
            .getReference("${baseBlock}/${App.context.currentUser!!.id}/${user.id}")
            .removeValue()
    }

    fun isUserBlocked(user: User, callback: (Boolean) -> Unit) {
        database
            .getReference("${baseBlock}/${App.context.currentUser!!.id}/${user.id}")
            .addListenerForSingleValueEvent(object: ValueEventListener {
                override fun onCancelled(p0: DatabaseError) {
                    callback(false)
                }

                override fun onDataChange(snapshot: DataSnapshot) {
                    val isBlocked = snapshot.getValue(String::class.java) != null
                    callback(isBlocked)
                }
            })
    }


    fun setProfileImage(uri: Uri, callback: (String?) -> Unit) {
        val fileName: String? = UUID.randomUUID().toString()
        val imageRef = storage.getReference("/images/$fileName")
        imageRef.putFile(uri)
            .addOnSuccessListener {
                imageRef.downloadUrl
                    .addOnSuccessListener {imageUrl ->
                        if (imageUrl != null) {
                            callback(imageUrl.toString())
                        } else {
                            callback(null)
                        }
                    }
                    .addOnFailureListener {
                        callback(null)
                    }
            }
    }

    fun removeProfileImage(url: String) {
        storage
            .getReferenceFromUrl(url)
            .delete()
    }

    fun findById(id: String, callback: (User?) -> Unit) {
        database
            .getReference("/users/$id")
            .addListenerForSingleValueEvent(object: ValueEventListener {
                override fun onCancelled(p0: DatabaseError) {
                    callback(null)
                }

                override fun onDataChange(snapshot: DataSnapshot) {
                    val user = snapshot.getValue(User::class.java)
                    callback(user)
                }

            })
    }

    fun getAll(callback: (ArrayList<User>) -> Unit) {
        val ref = database.getReference(baseUsers)
        ref.addListenerForSingleValueEvent(object: ValueEventListener {
            override fun onCancelled(error: DatabaseError) {}

            override fun onDataChange(snapshot: DataSnapshot) {
                val users = ArrayList<User>()

                snapshot.children.forEach {
                    val user = it.getValue(User::class.java)

                    if (user != null && user.id != App.context.currentUser?.id ?: "") {
                        users.add(user)
                    }
                }

                callback(users)
            }
        })
    }

    fun findByUsername(username: String, callback: (Boolean) -> Unit) {
        database
            .reference
            .child(baseUsers)
            .orderByChild("username")
            .equalTo(username)
            .addListenerForSingleValueEvent(object: ValueEventListener {
                override fun onCancelled(p0: DatabaseError) {
                    callback(true)
                }

                override fun onDataChange(snapshot: DataSnapshot) {
                    val exists = snapshot.getValue(User::class.java) != null
                    callback(exists)
                }
            })
    }

    fun getCurrent(callback: (User?) -> Unit) {
        // Query current user
        database
            .reference
            .child(baseUsers)
            .orderByChild("id")
            .equalTo(auth.uid)
            .addListenerForSingleValueEvent(object: ValueEventListener {
                override fun onCancelled(error: DatabaseError) {
                    callback(null)
                }

                override fun onDataChange(snapshot: DataSnapshot) {
                    snapshot.children.forEach {
                        val user = it.getValue(User::class.java)

                        App.context.currentUser = user
                        setStatus("online")
                        callback(user)
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
                    val imageRef = storage.getReference("${baseImages}/$fileName")
                    imageRef.putFile(selectedPhotoUri)
                        .addOnSuccessListener {
                            imageRef.downloadUrl
                                .addOnSuccessListener {imageUrl ->
                                    if (imageUrl != null) {
                                        persist(User(userId, user.username, user.email, imageUrl.toString())) { message ->
                                            if (message == null || message == "") {
                                                setStatus("online")
                                            }

                                            callback(message)
                                        }
                                    }
                                }
                                .addOnFailureListener {
                                    persist(User(userId, user.username, user.email)) { message ->
                                        if (message == null || message == "") {
                                            setStatus("online")
                                        }

                                        callback(message)
                                    }
                                }
                        }
                        .addOnFailureListener {
                            persist(User(userId, user.username, user.email)) { message ->
                                if (message == null || message == "") {
                                    setStatus("online")
                                }

                                callback(message)
                            }
                        }
                } else {
                    persist(User(userId, user.username, user.email)) { message ->
                        if (message == null || message == "") {
                            setStatus("online")
                        }

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
                setStatus("online")
                callback("")
            }
            .addOnFailureListener {
                callback("Invalid username or password")
            }
    }

    fun persist(user: User, callback: (String?) -> Unit) {
        val ref = database.getReference("${baseUsers}/${user.id}")
        ref.setValue(user)
            .addOnSuccessListener {
                App.context.currentUser = user
                callback("")
            }
            .addOnFailureListener {
                callback(it.message)
            }
    }

    fun signOut(callback: (() -> Unit)? = null) {
        setStatus("offline") {
            auth.signOut()
            App.context.hasSetToken = false
            App.context.currentUser = null

            if (callback != null) {
                callback()
            }
        }
    }

    fun setStatus(status: String, callback: (() -> Unit)? = null) {
        if ((status == "online" || status == "offline") && App.context.currentUser != null) {
            database
                .getReference("${baseUsers}/${App.context.currentUser!!.id}")
                .child("status")
                .setValue(status)
                .addOnCompleteListener {
                    if (callback != null) {
                        callback()
                    }
                }
        }
    }

    fun setDeviceToken(token: String) {
       if (App.context.currentUser != null) {
           database
               .getReference("${baseUsers}/${App.context.currentUser!!.id}")
               .child("deviceToken")
               .setValue(token)
       }
    }

    fun getCurrentToken(callback: (String?) -> Unit) {
        FirebaseInstanceId.getInstance().instanceId
            .addOnCompleteListener { task ->
                if (!task.isSuccessful) {
                    callback(null)
                }

                // Get new Instance ID token
                val token = task.result?.token
                callback(token)
            }
    }
}
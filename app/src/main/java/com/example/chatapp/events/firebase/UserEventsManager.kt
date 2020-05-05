package com.example.chatapp.events.firebase

import com.example.chatapp.App
import com.example.chatapp.models.User
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase

class UserEventsManager {
    private val database = Firebase.database
    private val usersBase = "/users"
    private val databaseReferences: HashMap<String, DatabaseReference> = HashMap()

    fun onUserToken(user: User, listener: ValueEventListener) {
        if (App.context.currentUser == null) {
            return
        }

        val key = "${usersBase}/${user.id}/deviceToken"

        if (databaseReferences[key] != null) {
            offUserToken(user, listener)
        }

        val messagesRef = database.getReference(key)
        messagesRef.addValueEventListener(listener)
        databaseReferences[key] = messagesRef
    }

    fun offUserToken(user: User, listener: ValueEventListener) {
        if (App.context.currentUser == null) {
            return
        }

        val key = "${usersBase}/${user.id}/deviceToken"
        val ref = databaseReferences[key] ?: return

        ref.removeEventListener(listener)
        databaseReferences.remove(key)
    }

    fun onUserStatus(user: User, listener: ValueEventListener) {
        if (App.context.currentUser == null) {
            return
        }

        val key = "${usersBase}/${user.id}/status"

        if (databaseReferences[key] != null) {
            offUserStatus(user, listener)
        }

        val messagesRef = database.getReference(key)
        messagesRef.addValueEventListener(listener)
        databaseReferences[key] = messagesRef
    }

    fun offUserStatus(user: User, listener: ValueEventListener) {
        if (App.context.currentUser == null) {
            return
        }

        val key = "${usersBase}/${user.id}/status"
        val ref = databaseReferences[key] ?: return

        ref.removeEventListener(listener)
        databaseReferences.remove(key)
    }

    fun onUserImage(user: User, listener: ValueEventListener) {
        if (App.context.currentUser == null) {
            return
        }

        val key = "${usersBase}/${user.id}/imageName"

        if (databaseReferences[key] != null) {
            offUserImage(user, listener)
        }

        val messagesRef = database.getReference(key)
        messagesRef.addValueEventListener(listener)
        databaseReferences[key] = messagesRef
    }

    fun offUserImage(user: User, listener: ValueEventListener) {
        if (App.context.currentUser == null) {
            return
        }

        val key = "${usersBase}/${user.id}/imageName"
        val ref = databaseReferences[key] ?: return

        ref.removeEventListener(listener)
        databaseReferences.remove(key)
    }
}
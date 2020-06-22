package com.example.chatapp.events.firebase

import android.util.Log
import com.example.chatapp.App
import com.example.chatapp.models.User
import com.google.firebase.database.ChildEventListener
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import java.lang.IllegalArgumentException

class ChatEventsManager {
    private val database = Firebase.database
    private val conversationBaseRef = "/messages"
    private val latestMessagesBaseRef = "/latest_messages"
    private val typingBaseRef = "/typing"
    private val databaseReferences: HashMap<String, DatabaseReference> = HashMap()

    fun onLatestMessageWithUser(user: User, listener: ValueEventListener) {
        if (App.context.currentUser == null) {
            return
        }

        val key = "${latestMessagesBaseRef}/${App.context.currentUser!!.id}/${user.id}"

        if (databaseReferences[key] != null) {
            offLatestMessageWithUser(user, listener)
        }

        val messagesRef = database.getReference(key)
        messagesRef.addValueEventListener(listener)
        databaseReferences[key] = messagesRef
    }

    fun offLatestMessageWithUser(user: User, listener: ValueEventListener) {
        if (App.context.currentUser == null) {
            return
        }

        val key = "${latestMessagesBaseRef}/${App.context.currentUser!!.id}/${user.id}"
        val ref = databaseReferences[key] ?: return

        ref.removeEventListener(listener)
        databaseReferences.remove(key)
    }

    fun onLatestMessages(listener: ChildEventListener) {
        if (App.context.currentUser == null) {
            return
        }

        val key = "${latestMessagesBaseRef}/${App.context.currentUser!!.id}"

        if (databaseReferences[key] != null) {
            offLatestMessages(listener)
        }

        val messagesRef = database.getReference(key)
        messagesRef.addChildEventListener(listener)
        databaseReferences[key] = messagesRef
    }

    fun offLatestMessages(listener: ChildEventListener) {
        if (App.context.currentUser == null) {
            return
        }

        val key = "${latestMessagesBaseRef}/${App.context.currentUser!!.id}"
        val ref = databaseReferences[key] ?: return

        ref.removeEventListener(listener)
        databaseReferences.remove(key)
    }

    fun onUserTyping(user: User, listener: ValueEventListener) {
        if (App.context.currentUser == null) {
            return
        }

        val key = "${typingBaseRef}/${user.id}"

        if (databaseReferences[key] != null) {
            offUserTyping(user, listener)
        }

        val messagesRef = database.getReference(key)
        messagesRef.addValueEventListener(listener)
        databaseReferences[key] = messagesRef
    }

    fun offUserTyping(user: User, listener: ValueEventListener) {
        if (App.context.currentUser == null) {
            return
        }

        val key = "${typingBaseRef}/${user.id}"
        val ref = databaseReferences[key] ?: return

        ref.removeEventListener(listener)
        databaseReferences.remove(key)
    }

    fun onUserConversation(withUser: User, listener: ChildEventListener) {
        if (App.context.currentUser == null) {
            return
        }

        val key = "${conversationBaseRef}/${App.context.currentUser!!.id}/${withUser.id}"

        if (databaseReferences[key] != null) {
            offUserConversation(withUser, listener)
        }

        val messagesRef = database.getReference(key)
        messagesRef.addChildEventListener(listener)
        databaseReferences[key] = messagesRef
    }

    fun offUserConversation(withUser: User, listener: ChildEventListener) {
        if (App.context.currentUser == null) {
            return
        }

        val key = "${conversationBaseRef}/${App.context.currentUser!!.id}/${withUser.id}"
        val ref = databaseReferences[key] ?: return

        ref.removeEventListener(listener)
        databaseReferences.remove(key)
    }
}
package com.example.chatapp.firebase_events

import android.util.Log
import com.example.chatapp.App
import com.example.chatapp.models.User
import com.google.firebase.database.ChildEventListener
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import java.lang.IllegalArgumentException

class ChatEventsManager {
    private val database = Firebase.database
    private val conversationBaseRef = "/messages"
    private val latestMessagesBaseRef = "/latest_messages"
    private val databaseReferences: HashMap<String, DatabaseReference> = HashMap()

    fun onLatestMessages(listener: ChildEventListener) {
        Log.d("CURRENT USER LATEST", App.context.currentUser.toString())

        if (App.context.currentUser == null) {
            return
        }

        Log.d("LATEST", "Listening")

        val key = "${latestMessagesBaseRef}/${App.context.currentUser!!.id}"

        if (databaseReferences[key] != null) {
            offLatestMessages(listener)
        }

        val messagesRef = database.getReference(key)
        messagesRef.addChildEventListener(listener)
        Log.d("LATEST LISTENER", key)
        Log.d("LATEST LISTENER", listener.toString())
        databaseReferences[key] = messagesRef
    }

    fun offLatestMessages(listener: ChildEventListener) {
        if (App.context.currentUser == null) {
            return
        }

        val key = "${latestMessagesBaseRef}/${App.context.currentUser!!.id}"
        Log.d("OFF LATEST", key)
        Log.d("OFF LATEST", listener.toString())
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
            throw IllegalArgumentException("Listener already added!")
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
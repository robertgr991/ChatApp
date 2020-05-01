package com.example.chatapp.repositories

import com.example.chatapp.models.Message
import com.example.chatapp.models.dto.CreateMessageDTO
import com.google.firebase.auth.ktx.auth
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase

class ChatRepository {
    private val auth = Firebase.auth
    private val database = Firebase.database

    fun persist(message: CreateMessageDTO, callback: (Boolean, String?) -> Unit) {
        // Save the message for both user
        val baseMessages = "/messages"
        val ref = database.getReference("${baseMessages}/${message.fromId}/${message.toId}").push()
        val reverseRef = database.getReference("${baseMessages}/${message.toId}/${message.fromId}").push()

        if (ref.key != null && reverseRef.key != null) {
            message.id = ref.key!!
            ref
                .setValue(message)
                .addOnSuccessListener {
                    // First save was successful
                    message.id = reverseRef.key!!
                    reverseRef
                        .setValue(message)
                        .addOnSuccessListener {
                            // Both operations were performed with success
                            // Insert latest message
                            val baseLatest = "/latest_messages"
                            val latestMessageRef = database.getReference("${baseLatest}/${message.fromId}/${message.toId}")
                            val reverseLatestMessageRef = database.getReference("${baseLatest}/${message.toId}/${message.fromId}")
                            latestMessageRef.setValue(message)
                            reverseLatestMessageRef.setValue(message)

                            callback(true, null)
                        }
                        .addOnFailureListener {
                            // If the second one save is not successful remove the first message
                            database.getReference("/messages/${message.fromId}/${message.toId}/${ref.key}").removeValue()
                            callback(false, it.message)
                        }
                }
                .addOnFailureListener {
                    // First save failed
                    callback(false, it.message)
                }
        } else {
            callback(false, "There was an error")
        }
    }
}
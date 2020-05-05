package com.example.chatapp.repositories

import android.util.Log
import com.example.chatapp.App
import com.example.chatapp.models.Message
import com.example.chatapp.models.User
import com.example.chatapp.models.dto.CreateMessageDTO
import com.google.android.gms.tasks.Task
import com.google.firebase.auth.ktx.auth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import java.util.*

class ChatRepository {
    private val database = Firebase.database
    private val baseLatest = "/latest_messages"
    private val baseMessages = "/messages"
    private val baseConversation = "/conversations"

    fun getLastMessageInConversation(user1: User, user2: User, callback: (Message?) -> Unit) {
        database
            .getReference("${baseMessages}/${user1.id}/${user2.id}")
            .orderByKey()
            .limitToLast(1)
            .addListenerForSingleValueEvent(object: ValueEventListener {
                override fun onCancelled(p0: DatabaseError) {
                    callback(null)
                }

                override fun onDataChange(snapshot: DataSnapshot) {
                    val message = snapshot
                        .children
                        .elementAt(0)
                        .getValue(Message::class.java)
                    callback(message)
                }
            })
    }

    fun getLatestMessageForUser(user: User, callback: (Message?) -> Unit) {
        database
            .getReference("${baseLatest}/${user.id}/${App.context.currentUser!!.id}")
            .addListenerForSingleValueEvent(object: ValueEventListener {
                override fun onCancelled(p0: DatabaseError) {
                    callback(null)
                }

                override fun onDataChange(snapshot: DataSnapshot) {
                    val message = snapshot.getValue(Message::class.java)
                    callback(message)
                }
            })
    }

    fun getLatestMessageForCurrentUser(user: User, callback: (Message?) -> Unit) {
        database
            .getReference("${baseLatest}/${App.context.currentUser!!.id}/${user.id}")
            .addListenerForSingleValueEvent(object: ValueEventListener {
                override fun onCancelled(p0: DatabaseError) {
                    callback(null)
                }

                override fun onDataChange(snapshot: DataSnapshot) {
                    val message = snapshot.getValue(Message::class.java)
                    callback(message)
                }
            })
    }

    fun setLatestMessageForUser(user: User, message: Message) {
        database
            .getReference("${baseLatest}/${user.id}/${App.context.currentUser!!.id}")
            .setValue(message)
    }

    fun setLatestMessageForCurrentUser(user: User, message: Message) {
        database
            .getReference("${baseLatest}/${App.context.currentUser!!.id}/${user.id}")
            .setValue(message)
    }

    /**
     * Deleting a message for both means that the content
     * is replaced with "This message was deleted" but it's still
     * in the chat log
     */
    fun deleteMessageForBoth(user: User, message: Message) {
        // Delete for current user
        val refCurrentUser = database.getReference("${baseMessages}/${App.context.currentUser!!.id}/${user.id}/${message.id}")
        refCurrentUser
            .child("deleted")
            .setValue("true")
        refCurrentUser
            .child("content")
            .setValue("")
        // Check if this was the latest message to delete it as well
        getLatestMessageForCurrentUser(user) {
            if (it != null && it.id == message.id) {
                val latestRef = database.getReference("${baseLatest}/${App.context.currentUser!!.id}/${user.id}")
                latestRef
                    .child("deleted")
                    .setValue("true")
                latestRef
                    .child("content")
                    .setValue("")
            }
        }
        // If this message still exists in partner conversation
        // set it as deleted as well
        val ref = database.getReference("${baseMessages}/${user.id}/${App.context.currentUser!!.id}/${message.id}")
        ref
            .addListenerForSingleValueEvent(object: ValueEventListener {
                override fun onCancelled(p0: DatabaseError) {}

                override fun onDataChange(snapshot: DataSnapshot) {
                    snapshot.getValue(Message::class.java) ?: return
                    ref
                        .child("deleted")
                        .setValue("true")
                    ref
                        .child("content")
                        .setValue("")
                    getLatestMessageForUser(user) {
                        if (it != null && it.id == message.id) {
                            val latestRef = database.getReference("${baseLatest}/${user.id}/${App.context.currentUser!!.id}")
                            latestRef
                                .child("deleted")
                                .setValue("true")
                            latestRef
                                .child("content")
                                .setValue("")
                        }
                    }
                }
            })
    }

    /**
     * Deletes the message for the current user completely
     * it doesn't affect the partner messages
     */
    fun deleteMessageForMe(user: User, message: Message) {
        database
            .getReference("${baseMessages}/${App.context.currentUser!!.id}/${user.id}")
            .child(message.id)
            .removeValue()
            .addOnSuccessListener {
                // Check if this was the latest message to also delete it from there and replace it with the previous message
                val refLatest = database.getReference("${baseLatest}/${App.context.currentUser!!.id}/${user.id}")
                refLatest
                    .addListenerForSingleValueEvent(object: ValueEventListener {
                        override fun onCancelled(p0: DatabaseError) {}

                        override fun onDataChange(snapshot: DataSnapshot) {
                            val latestMessage = snapshot.getValue(Message::class.java) ?: return

                            // This was the latest message, replace it or remove it
                            // if there are no messages left
                            if (latestMessage.id == message.id) {
                                getLastMessageInConversation(App.context.currentUser!!, user) {
                                    if (it != null) {
                                        refLatest.setValue(it)
                                    } else {
                                        refLatest.removeValue()
                                    }
                                }
                            }
                        }
                    })
            }
    }

    fun setSeenLastMessage(user: User, message: Message) {
        val ref = database.getReference("${baseLatest}/${user.id}/${App.context.currentUser!!.id}")
        ref
            .addListenerForSingleValueEvent(object: ValueEventListener {
                override fun onCancelled(p0: DatabaseError) {}

                override fun onDataChange(snapshot: DataSnapshot) {
                    val msg = snapshot.getValue(Message::class.java) ?: return

                    if (msg.id == message.id) {
                        ref
                            .child("seen")
                            .setValue("true")
                    }
                }
            })
        val refCurrentUser = database.getReference("${baseLatest}/${App.context.currentUser!!.id}/${user.id}")
        refCurrentUser
            .addListenerForSingleValueEvent(object: ValueEventListener {
                override fun onCancelled(p0: DatabaseError) {}

                override fun onDataChange(snapshot: DataSnapshot) {
                    val msg = snapshot.getValue(Message::class.java) ?: return

                    if (msg.id == message.id) {
                        refCurrentUser
                            .child("seen")
                            .setValue("true")
                    }
                }
            })
    }

    fun setSeenMessage(user: User, message: Message) {
        val refCurrentUser = database.getReference("${baseMessages}/${App.context.currentUser!!.id}/${user.id}/${message.id}")
        refCurrentUser
            .child("seen")
            .setValue("true")
        refCurrentUser
            .child("seenAt")
            .setValue(Date())
        val ref = database.getReference("${baseMessages}/${user.id}/${App.context.currentUser!!.id}/${message.id}")
        ref
            .addListenerForSingleValueEvent(object: ValueEventListener {
                override fun onCancelled(p0: DatabaseError) {}

                override fun onDataChange(snapshot: DataSnapshot) {
                    snapshot.getValue(Message::class.java) ?: return
                    ref
                        .child("seen")
                        .setValue("true")
                    ref
                        .child("seenAt")
                        .setValue(Date())
                }
            })
    }

    fun getCurrentConversation(callback: (String?) -> Unit) {
        database
            .getReference("${baseConversation}/${App.context.currentUser!!.id}")
            .addListenerForSingleValueEvent(object: ValueEventListener {
                override fun onCancelled(p0: DatabaseError) {
                    callback(null)
                }

                override fun onDataChange(snapshot: DataSnapshot) {
                    callback(snapshot.getValue(String::class.java))
                }
            })
    }

    fun setOnConversation(user: User) {
        database
            .getReference("${baseConversation}/${App.context.currentUser!!.id}")
            .setValue(user.id)
    }

    fun setOffConversation() {
        database
            .getReference("${baseConversation}/${App.context.currentUser!!.id}")
            .removeValue()
    }

    fun removeMessagesWithUser(user: User, callback: (Boolean) -> Unit) {
        // Remove latest messages
        database
            .getReference("${baseLatest}/${App.context.currentUser!!.id}/${user.id}")
            .removeValue()
            .addOnSuccessListener {
                // Remove messages
                database
                    .getReference("${baseMessages}/${App.context.currentUser!!.id}/${user.id}")
                    .removeValue()
                    .addOnSuccessListener {
                        callback(true)
                    }
                    .addOnFailureListener {
                        callback(false)
                    }
            }
            .addOnFailureListener {
                callback(false)
            }
    }

    fun persist(message: CreateMessageDTO, callback: (Boolean, String?) -> Unit) {
        // Save the message for both user
        val ref = database.getReference("${baseMessages}/${message.fromId}/${message.toId}").push()

        if (ref.key == null) {
            callback(false, "There was an error")
        }

        message.id = ref.key!!
        val reverseRef = database.getReference("${baseMessages}/${message.toId}/${message.fromId}/${message.id}")

        if (reverseRef.key != null) {
            ref
                .setValue(message)
                .addOnSuccessListener {
                    // First save was successful
                    reverseRef
                        .setValue(message)
                        .addOnSuccessListener {
                            // Both operations were performed with success
                            // Insert latest message
                            val latestMessageRef = database.getReference("${baseLatest}/${message.fromId}/${message.toId}")
                            val reverseLatestMessageRef = database.getReference("${baseLatest}/${message.toId}/${message.fromId}")
                            latestMessageRef.setValue(message)
                            reverseLatestMessageRef.setValue(message)
                            callback(true, null)
                        }
                        .addOnFailureListener {
                            // If the second one save is not successful remove the first message
                            database.getReference("/messages/${message.fromId}/${message.toId}/${ref.key!!}").removeValue()
                            database.getReference("/messages/${message.toId}/${message.fromId}/${reverseRef.key!!}").removeValue()
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
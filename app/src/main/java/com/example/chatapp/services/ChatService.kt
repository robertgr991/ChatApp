package com.example.chatapp.services

import com.example.chatapp.App
import com.example.chatapp.models.Message
import com.example.chatapp.models.User
import com.example.chatapp.models.dto.CreateMessageDTO
import com.example.chatapp.repositories.ChatRepository
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import org.koin.core.KoinComponent
import org.koin.core.inject
import java.util.Date

class ChatService: KoinComponent {
    private val auth = Firebase.auth
    private val chatRepository: ChatRepository by inject()

    fun sendMessage(content: Any, toUser: User, callback: (Boolean, String?) -> Unit) {
        if (App.context.currentUser == null) {
            return
        }

        val message = CreateMessageDTO("", App.context.currentUser!!.id, toUser.id, Date(), content)
        chatRepository.persist(message, callback)
    }
}
package com.example.chatapp.models.dto

import com.example.chatapp.models.Message
import com.example.chatapp.models.User

data class MessageWithPartnerUserDTO(val message: Message, val user: User)
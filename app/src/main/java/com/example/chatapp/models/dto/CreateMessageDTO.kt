package com.example.chatapp.models.dto

import com.example.chatapp.models.User
import java.util.*

data class CreateMessageDTO(var id: String, val fromId: String, val toId: String, val date: Date, val content: Any) {
    constructor(): this("", "", "", Date(), "")
}
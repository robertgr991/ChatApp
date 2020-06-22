package com.example.chatapp.models.dto

import java.util.*

data class CreateMessageDTO(
    var id: String,
    val fromId: String,
    val toId: String,
    val date: Date,
    val content: Any,
    var seen: String = "false",
    var seenAt: Date? = null,
    val deleted: String = "false"
) {
    constructor(): this("", "", "", Date(), "")
}
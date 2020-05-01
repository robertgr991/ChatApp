package com.example.chatapp.models

import java.util.*

data class Message(val id: String, val fromId: String, val toId: String, val date: Date, val content: Any) {
    constructor(): this("", "", "", Date(), "")
}
package com.example.chatapp.models

data class User(val id: String, val username: String, val email: String, var imageName: String? = null, var bio: String? = null) {
    constructor(): this("", "", "")
}
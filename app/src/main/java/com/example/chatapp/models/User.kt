package com.example.chatapp.models

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize

@Parcelize
data class User(
    val id: String,
    val username: String,
    val email: String,
    var status: String = "offline",
    var imageName: String? = null,
    var deviceToken: String? = null,
    var bio: String? = null
) :
    Parcelable {
    constructor(): this("", "", "")
}
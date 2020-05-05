package com.example.chatapp.models.dto

import android.net.Uri

data class UpdateUserDTO(val hasTouchedImage: Boolean, val bio: String?, val newImageUri: Uri?)
package com.example.chatapp.validators

import com.example.chatapp.models.dto.UpdateUserDTO

class UpdateUserValidator: Validator {
    private val bioMessage = "Bio length should be between 1 and 50 characters or empty to remove it"

    override fun validate(obj: Any): ValidationResponse {
        if (obj is UpdateUserDTO) {
            if (obj.bio != null && obj.bio.length > 50) {
                return ValidationResponse(false, arrayOf(bioMessage))
            }

            return ValidationResponse(true, arrayOf(""))
        } else {
            return ValidationResponse(false, arrayOf("Invalid input"))
        }
    }
}
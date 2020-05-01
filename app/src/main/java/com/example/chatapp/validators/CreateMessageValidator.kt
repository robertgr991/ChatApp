package com.example.chatapp.validators

class CreateMessageValidator: Validator {
    private val emptyMessage = "You cannot send empty messages"

    override fun validate(obj: Any): ValidationResponse {
        if (obj.toString().isBlank()) {
            return ValidationResponse(false, arrayOf(emptyMessage))
        }

        return ValidationResponse(true, arrayOf(""))
    }
}
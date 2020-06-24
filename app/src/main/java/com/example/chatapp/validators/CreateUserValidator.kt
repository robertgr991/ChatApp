package com.example.chatapp.validators

import com.example.chatapp.models.dto.CreateUserDTO

class CreateUserValidator: Validator {
    private val usernameMessage = "Username should be between 3 and 20 characters long and should have at least one letter"
    private val emailMessage = "Invalid email"
    private val passwordMessage = "Invalid password"
    private val invalidInput = "Invalid input"

    companion object {
         const val passwordExplanationMessage = "Note: The password must be at least 6 characters long, have a letter, number, uppercase letter and one of the following characters: @#$%!-_?&"
    }

    override fun validate(obj: Any): ValidationResponse {
        if (obj is CreateUserDTO) {
            if (obj.username.length < 3 || obj.username.length > 20 || !"[a-zA-Z]+".toRegex().containsMatchIn(obj.username)) {
                return ValidationResponse(false, arrayOf(usernameMessage))
            }

            if (!android.util.Patterns.EMAIL_ADDRESS.matcher(obj.email).matches()) {
                return ValidationResponse(false, arrayOf(emailMessage))
            }

            if (!"^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z])(?=.*[@#\$%!\\-_?&])(?=\\S+\$).{6,}".toRegex().containsMatchIn(obj.password)) {
                return ValidationResponse(false, arrayOf(passwordMessage))
            }

            return ValidationResponse(true, arrayOf(""))
        } else {
            return ValidationResponse(false, arrayOf(invalidInput))
        }
    }
}
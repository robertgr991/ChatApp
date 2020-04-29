package com.example.chatapp.validators

interface Validator {
    fun validate(obj: Any): ValidationResponse
}
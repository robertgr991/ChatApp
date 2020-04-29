package com.example.chatapp.validators

data class ValidationResponse(val status: Boolean, val messages: Array<String>) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as ValidationResponse

        if (status != other.status) return false
        if (!messages.contentEquals(other.messages)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = status.hashCode()
        result = 31 * result + messages.contentHashCode()
        return result
    }
}
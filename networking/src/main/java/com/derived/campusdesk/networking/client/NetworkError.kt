package com.derived.campusdesk.networking.client

sealed class NetworkError : Exception() {
    data object InvalidUrl : NetworkError() {
        private fun readResolve(): Any = InvalidUrl
        override val message: String = "The request URL is invalid."
    }

    data object Unauthorized : NetworkError() {
        private fun readResolve(): Any = Unauthorized
        override val message: String = "Your session expired. Please sign in again."
    }

    data object NotEnrolled : NetworkError() {
        private fun readResolve(): Any = NotEnrolled
        override val message: String = "You are not enrolled in any classes yet."
    }

    data class Client(val status: Int, override val message: String) : NetworkError()
    data class Server(val status: Int, override val message: String) : NetworkError()
    data class Decoding(override val message: String = "The server returned data in an unexpected format.") : NetworkError()
    data class Transport(override val message: String) : NetworkError()
    data object Empty : NetworkError() {
        private fun readResolve(): Any = Empty
        override val message: String = "The server returned an empty response."
    }

    val isNotEnrolled: Boolean
        get() = this is NotEnrolled ||
            (this is Client && message.contains("enroll", ignoreCase = true)) ||
            (this is Server && message.contains("enroll", ignoreCase = true))
}

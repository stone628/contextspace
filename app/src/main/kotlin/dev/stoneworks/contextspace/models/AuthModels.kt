package dev.stoneworks.contextspace.models

import kotlinx.serialization.Serializable

@Serializable
data class RegisterRequest(
    val username: String,
    val password: String,
)

@Serializable
data class LoginRequest(
    val username: String,
    val password: String,
)

@Serializable
data class RefreshRequest(
    val authToken: String? = null,
    val refreshToken: String? = null,
)

@Serializable
data class AuthResponse(
    val authToken: String,
    val refreshToken: String,
)

@Serializable
data class ErrorResponse(
    val error: String,
)

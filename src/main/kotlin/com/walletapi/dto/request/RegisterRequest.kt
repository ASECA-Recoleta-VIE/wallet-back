package com.walletapi.dto.request

data class RegisterRequest(
    val fullName: String,
    val email: String,
    val password: String
)
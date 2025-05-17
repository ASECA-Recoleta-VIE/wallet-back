package com.walletapi.dto.request

data class LoginRequest(
    val email: String,
    val password: String
)
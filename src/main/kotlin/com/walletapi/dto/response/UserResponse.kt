package com.walletapi.dto.response

data class UserResponse(
    val id: String,
    val fullName: String,
    val email: String,
    val wallets: List<WalletResponse>
)

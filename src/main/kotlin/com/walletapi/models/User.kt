package com.walletapi.models

import com.walletapi.models.Wallet

data class User(
    val fullName: String,
    val email: String,
    val password: String,
    val wallets: List<Wallet> = emptyList(),
)

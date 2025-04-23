package com.wallet.walletapi.models

import java.util.*

data class Person(
    val id: UUID = UUID.randomUUID(),
    val fullName: String,
    val email: String
)

package com.walletapi.models

import java.time.LocalDate

data class History(
    val date: LocalDate,
    val description: String,
    val type: TransactionType,
    val amount: Double,
    val balance: Double,
)


enum class TransactionType {
    DEPOSIT,
    WITHDRAWAL,
    TRANSFER_IN,
    TRANSFER_OUT
}
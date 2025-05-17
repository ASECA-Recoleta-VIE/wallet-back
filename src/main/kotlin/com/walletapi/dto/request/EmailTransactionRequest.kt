package com.walletapi.dto.request

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "Request to perform a transaction using a user's email")
data class EmailTransactionRequest(
    @field:Schema(description = "User email", example = "user@example.com")
    val email: String,
    
    @field:Schema(description = "Transaction amount", example = "100.00")
    val amount: Double,
    
    @field:Schema(description = "Optional transaction description", example = "Monthly deposit")
    val description: String? = null
)
package com.walletapi.dto.request

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "Request to transfer funds between users")
data class TransferRequest(
    @field:Schema(description = "Sender's email", example = "sender@example.com")
    val fromEmail: String,
    
    @field:Schema(description = "Recipient's email", example = "recipient@example.com")
    val toEmail: String,
    
    @field:Schema(description = "Transfer amount", example = "50.00")
    val amount: Double,
    
    @field:Schema(description = "Optional transfer description", example = "Paying for dinner")
    val description: String? = null
)
package com.walletapi.dto.response

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "Transaction history record")
data class HistoryResponse(
    @field:Schema(description = "Transaction ID")
    val id: String,
    
    @field:Schema(description = "Transaction amount", example = "100.00")
    val amount: Double,
    
    @field:Schema(description = "Transaction timestamp", example = "2023-04-15T14:30:15.123Z")
    val timestamp: String,
    
    @field:Schema(description = "Optional transaction description")
    val description: String?,
    
    @field:Schema(description = "For transfers, the recipient wallet ID")
    val toWalletId: String?
)
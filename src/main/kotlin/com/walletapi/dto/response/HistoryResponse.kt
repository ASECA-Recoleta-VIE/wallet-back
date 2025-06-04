package com.walletapi.dto.response

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "Transaction history record")
data class HistoryResponse(
    @field:Schema(description = "Transaction ID")
    val id: Long = 0L,
    
    @field:Schema(description = "Transaction amount", example = "100.00")
    val amount: Double = 0.0,
    
    @field:Schema(description = "Transaction timestamp", example = "2023-04-15T14:30:15.123Z")
    val timestamp: String = "",
    
    @field:Schema(description = "Optional transaction description")
    val description: String? = null,
)
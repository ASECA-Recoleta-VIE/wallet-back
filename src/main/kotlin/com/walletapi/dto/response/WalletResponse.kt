package com.walletapi.dto.response

import io.swagger.v3.oas.annotations.media.Schema

data class WalletResponse(
    @field:Schema(description = "Wallet ID")
    val id: String,
    
    @field:Schema(description = "Current balance", example = "1250.50")
    val balance: Double,
    
    @field:Schema(description = "Currency code", example = "USD")
    val currency: String
)
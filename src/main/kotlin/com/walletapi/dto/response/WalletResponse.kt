package com.walletapi.dto.response

import io.swagger.v3.oas.annotations.media.Schema

data class WalletResponse(
    @field:Schema(description = "Wallet Name", example = "Personal Savings")
    val name: String = "Main Wallet",
    
    @field:Schema(description = "Current balance", example = "1250.50")
    val balance: Double = 0.0,
    
    @field:Schema(description = "Currency code", example = "USD")
    val currency: String = "USD",
)
package com.walletapi.dto.response

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "Result of a transfer operation")
data class TransferResponse(
    @field:Schema(description = "Sender's wallet after transfer")
    val fromWallet: WalletResponse = WalletResponse(),
    
    @field:Schema(description = "Recipient's wallet after transfer")
    val toWallet: WalletResponse = WalletResponse(),
)
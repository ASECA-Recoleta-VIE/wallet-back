package com.walletapi.dto.response

import java.math.BigDecimal

data class FundAvailabilityResponse(
    val available: Boolean,
    val amount: BigDecimal,
    val accountId: String,
    val currentBalance: BigDecimal
)
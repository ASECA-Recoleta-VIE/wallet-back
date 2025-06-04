package com.walletapi.dto.request

import java.math.BigDecimal
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.DecimalMin

data class FundAvailabilityRequest(
    @field:NotNull(message = "Amount is required")
    @field:DecimalMin(value = "0.01", message = "Amount must be greater than 0")
    val amount: BigDecimal,
    
    val accountId: String? = null,
    
    val description: String? = null
)
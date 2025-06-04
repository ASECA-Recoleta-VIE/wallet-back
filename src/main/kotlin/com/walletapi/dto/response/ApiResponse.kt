package com.walletapi.dto.response

import java.time.LocalDateTime

data class ApiResponse<T>(
    val success: Boolean,
    val message: String,
    val data: T,
    val timestamp: LocalDateTime = LocalDateTime.now(),
    val transactionId: String? = null
)
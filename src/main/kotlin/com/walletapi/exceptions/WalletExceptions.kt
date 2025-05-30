package com.walletapi.exceptions

import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.ResponseStatus

@ResponseStatus(HttpStatus.NOT_FOUND)
class UserNotFoundException(email: String) : RuntimeException("User not found with email: $email")

@ResponseStatus(HttpStatus.NOT_FOUND)
class WalletNotFoundException(email: String) : RuntimeException("Wallet not found for user with email: $email")

@ResponseStatus(HttpStatus.FORBIDDEN)
class InvalidAmountException(message: String, cause: Throwable? = null) : RuntimeException(message, cause)

@ResponseStatus(HttpStatus.FORBIDDEN)
class InsufficientFundsException(message: String, cause: Throwable? = null) : RuntimeException(message, cause)

@ResponseStatus(HttpStatus.METHOD_NOT_ALLOWED)
class SelfTransferException(email: String) : RuntimeException("Self-transfer not allowed for user: $email")

sealed class WalletException : RuntimeException {
    constructor(message: String) : super(message)
    constructor(message: String, cause: Throwable) : super(message, cause)
} 
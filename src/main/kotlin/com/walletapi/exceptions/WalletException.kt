package com.walletapi.exceptions

import org.springframework.http.HttpStatus

/**
 * Base class for wallet-related exceptions
 */
open class WalletException(
    val status: HttpStatus,
    override val message: String,
    cause: Throwable? = null
) : RuntimeException(message, cause)

/**
 * Exception thrown when a user is not found
 */
class UserNotFoundException(email: String, cause: Throwable? = null) :
    WalletException(HttpStatus.NOT_FOUND, "User not found: $email", cause)

/**
 * Exception thrown when a user tries to transfer money to themselves
 */
class SelfTransferException(email: String, cause: Throwable? = null) :
    WalletException(HttpStatus.METHOD_NOT_ALLOWED, "User cannot transfer money to themselves: $email", cause)

/**
 * Exception thrown when a wallet is not found
 */
class WalletNotFoundException(email: String, cause: Throwable? = null) :
    WalletException(HttpStatus.NOT_FOUND, "No wallet found for user with email: $email", cause)

/**
 * Exception thrown when a transaction amount is invalid
 */
class InvalidAmountException(message: String, cause: Throwable? = null) : 
    WalletException(HttpStatus.FORBIDDEN, message, cause)

/**
 * Exception thrown when there are insufficient funds for a transaction
 */
class InsufficientFundsException(message: String, cause: Throwable? = null) : 
    WalletException(HttpStatus.BAD_REQUEST, message, cause)

/**
 * Exception thrown for general transaction errors
 */
class TransactionException(message: String, cause: Throwable? = null) : 
    WalletException(HttpStatus.BAD_REQUEST, message, cause)
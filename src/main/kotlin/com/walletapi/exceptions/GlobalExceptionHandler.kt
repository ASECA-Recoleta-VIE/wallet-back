package com.walletapi.exceptions

import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ControllerAdvice
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.context.request.WebRequest

/**
 * Global exception handler for the application
 */
@ControllerAdvice
class GlobalExceptionHandler {

    /**
     * Handle WalletException and its subclasses
     */
    @ExceptionHandler(WalletException::class)
    fun handleWalletException(ex: WalletException, request: WebRequest): ResponseEntity<ErrorResponse> {
        val errorResponse = ErrorResponse(
            status = ex.status.value(),
            error = ex.status.reasonPhrase,
            message = ex.message
        )
        return ResponseEntity(errorResponse, ex.status)
    }

    /**
     * Handle general exceptions
     */
    @ExceptionHandler(Exception::class)
    fun handleGeneralException(ex: Exception, request: WebRequest): ResponseEntity<ErrorResponse> {
        val errorResponse = ErrorResponse(
            status = 500,
            error = "Internal Server Error",
            message = "An unexpected error occurred: ${ex.message}"
        )
        return ResponseEntity(errorResponse, org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR)
    }


    /**
     * Handle UserException
     */
    @ExceptionHandler(UserException::class)
    fun handleUserException(ex: UserException, request: WebRequest): ResponseEntity<ErrorResponse> {
        val errorResponse = ErrorResponse(
            status = ex.status.value(),
            error = ex.status.reasonPhrase,
            message = ex.message
        )
        return ResponseEntity(errorResponse, ex.status)
    }
}

/**
 * Standard error response format
 */
data class ErrorResponse(
    val status: Int,
    val error: String,
    val message: String
)
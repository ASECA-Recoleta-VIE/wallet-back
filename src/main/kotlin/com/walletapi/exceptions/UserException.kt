package com.walletapi.exceptions

import org.springframework.http.HttpStatus

open class UserException(
    val status: HttpStatus,
    override val message: String,
    cause: Throwable? = null
) : RuntimeException(message, cause
) {
    /**
     * Exception thrown when a email or password is invalid
     */
    class InvalidCredentialsException(email: String, cause: Throwable? = null) :
        UserException(HttpStatus.UNAUTHORIZED, "email: $email or password is invalid", cause)

    /**
     * Thrown when user and password are empty
     */
    class EmptyCredentialsException(cause: Throwable? = null) :
        UserException(HttpStatus.BAD_REQUEST, "email and password cannot be empty", cause)


    class WeakPasswordException(
        password: String,
        cause: Throwable? = null
    ) : UserException(HttpStatus.BAD_REQUEST, "Password is too weak: $password", cause)

}
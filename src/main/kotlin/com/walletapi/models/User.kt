package com.walletapi.models

import com.walletapi.exceptions.UserException

data class User(
    var fullName: String,
    val email: String,
    val password: String,
    val wallets: List<Wallet> = emptyList(),
) {
    init {
        when (validatePassword(password)) {
            PasswordValidation.PASSWORD_TOO_SHORT -> throw UserException.WeakPasswordException(
                password,
                IllegalArgumentException("Password must be at least 8 characters long")
            )
            PasswordValidation.NO_UPPERCASE -> throw UserException.WeakPasswordException(
                password,
                IllegalArgumentException("Password must contain at least one uppercase letter")
            )
            PasswordValidation.NO_LOWERCASE -> throw UserException.WeakPasswordException(
                password,
                IllegalArgumentException("Password must contain at least one lowercase letter")
            )
            PasswordValidation.NO_NUMBER -> throw UserException.WeakPasswordException(
                password,
                IllegalArgumentException("Password must contain at least one number")
            )
            PasswordValidation.NO_SPECIAL_CHARACTER -> throw UserException.WeakPasswordException(
                password,
                IllegalArgumentException("Password must contain at least one special character")
            )
            PasswordValidation.VALID -> Unit // Valid password, do nothing
        }
    }
}

fun validatePassword(password: String): PasswordValidation {
    if (password.length < 8) {
        return PasswordValidation.PASSWORD_TOO_SHORT
    }
    if (!password.any { it.isUpperCase() }) {
        return PasswordValidation.NO_UPPERCASE
    }
    if (!password.any { it.isLowerCase() }) {
        return PasswordValidation.NO_LOWERCASE
    }
    if (!password.any { it.isDigit() }) {
        return PasswordValidation.NO_NUMBER
    }
    if (!password.any { !it.isLetterOrDigit() }) {
        return PasswordValidation.NO_SPECIAL_CHARACTER
    }
    return PasswordValidation.VALID
}


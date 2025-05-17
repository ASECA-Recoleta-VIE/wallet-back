package com.walletapi.dto
enum class PasswordValidation {
    PASSWORD_TOO_SHORT,
    NO_UPPERCASE,
    NO_LOWERCASE,
    NO_NUMBER,
    NO_SPECIAL_CHAR,
    VALID
}
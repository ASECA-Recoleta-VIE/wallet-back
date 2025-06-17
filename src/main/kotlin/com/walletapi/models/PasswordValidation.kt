package com.walletapi.models

enum class PasswordValidation {
    VALID,
    PASSWORD_TOO_SHORT,
    NO_UPPERCASE,
    NO_LOWERCASE,
    NO_NUMBER,
    NO_SPECIAL_CHARACTER
}

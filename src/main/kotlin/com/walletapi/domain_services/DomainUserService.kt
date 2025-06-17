package com.walletapi.domain_services

import com.walletapi.models.User
import com.walletapi.models.Wallet
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.stereotype.Service

@Service
class DomainUserService {
    private val encoder = BCryptPasswordEncoder()

    fun createUser(fullName: String, email: String, password: String, wallets: List<Wallet>): User {
        val validatePassword = User(fullName, email, password, wallets)
        val encryptedPassword = encryptPassword(password, email)
        val user = User(fullName, email, encryptedPassword, wallets)
        return user
    }


    fun encryptPassword(password: String, email: String): String {

    // Validate inputs
    if (password.isEmpty()) {
        throw IllegalArgumentException("Password cannot be empty")
    }
    
    // No need for custom salt - BCrypt handles this internally
    return encoder.encode(password)
}
    fun verifyPassword(rawPassword: String, encodedPassword: String): Boolean {
        return encoder.matches(rawPassword, encodedPassword)
    }
}
package com.walletapi.domain_services

import com.walletapi.models.User
import com.walletapi.models.Wallet
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder

class DomainUserService {
    private val encoder = BCryptPasswordEncoder()

    fun createUser(fullName: String, email: String, password: String, wallets: List<Wallet>): User {
        val encryptedPassword = encryptPassword(password, email)
        val user = User(fullName, email, encryptedPassword, wallets)
        return user
    }


    fun encryptPassword(password: String, email: String): String {
        return encoder.encode(
            email.substring(0, 3) +
                    password.substring(0,password.length/2) +
                    email.substring(email.length - 3) +
                    password.substring(password.length/2, password.length)
        )
    }

}

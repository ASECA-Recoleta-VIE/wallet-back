package com.walletapi.factories

import com.walletapi.entities.UserEntity
import com.walletapi.entities.WalletEntity

object UserFactory {
    fun createWithWallet(fullName: String, email: String, password: String): UserEntity {
        val user = UserEntity(
            fullName = fullName,
            email = email,
            password = password,
            wallets = mutableListOf()
        )

        val wallet = WalletEntity(
            name = "Main Wallet",
            balance = 10000.0,
            overdraft = 0.0,
            history = mutableListOf(),
            user = user
        )

        user.wallets.add(wallet)
        return user
    }
}
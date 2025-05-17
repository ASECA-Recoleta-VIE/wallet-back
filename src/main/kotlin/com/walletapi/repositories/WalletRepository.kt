package com.walletapi.repositories

import com.walletapi.models.Wallet
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface WalletRepository : JpaRepository<Wallet, String> {
    fun findByUserEmail(email: String): Wallet?
}

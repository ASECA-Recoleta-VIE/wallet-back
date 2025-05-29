package com.walletapi.repositories

import com.walletapi.entities.UserEntity
import com.walletapi.entities.WalletEntity
import com.walletapi.models.Wallet
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface WalletRepository : JpaRepository<WalletEntity, String> {
    fun findByUser(user: UserEntity): List<WalletEntity>
    fun getWalletEntityById(id: String): WalletEntity?
}

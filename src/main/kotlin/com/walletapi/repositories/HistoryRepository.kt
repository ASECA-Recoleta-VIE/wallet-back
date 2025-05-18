package com.walletapi.repositories

import com.walletapi.entities.HistoryEntity
import com.walletapi.entities.WalletEntity
import com.walletapi.models.History
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface HistoryRepository : JpaRepository<HistoryEntity, String> {
    fun findByWallet(wallet: WalletEntity): List<HistoryEntity>
}

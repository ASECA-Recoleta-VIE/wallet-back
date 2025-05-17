package com.walletapi.repositories

import com.walletapi.models.History
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface HistoryRepository : JpaRepository<History, String> {
    fun findByWalletId(walletId: String): List<History>
}

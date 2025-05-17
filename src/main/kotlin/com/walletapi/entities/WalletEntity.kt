package com.walletapi.entities

import com.walletapi.models.Wallet
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.Id
import jakarta.persistence.ManyToOne
import jakarta.persistence.OneToMany

@Entity
class WalletEntity(
    @Column var name: String?,
    @Column var balance: Double?,
    @Column var overdraft: Double?,
    @OneToMany var history: MutableList<HistoryEntity>,
    @ManyToOne var user: UserEntity?
) {
    @Id
    @GeneratedValue(strategy = jakarta.persistence.GenerationType.UUID)
    @Column(unique = true, nullable = false)
    var id: String? = null


    fun toWallet(): Wallet {
        return Wallet(
            name = this.name ?: "",
            balance = this.balance ?: 0.0,
            overdraft = this.overdraft ?: 0.0,
            history = this.history?.map { it.toHistory() } ?: emptyList()
        )
    }
}

fun walletToEntity(wallet: Wallet, user: UserEntity): WalletEntity {
    return WalletEntity(
        name = wallet.getName(),
        balance = wallet.getBalance(),
        overdraft = wallet.getOverdraft(),
        history = wallet.getHistory().map { historyToEntity(it) }.toMutableList(),
        user = user
    )
}
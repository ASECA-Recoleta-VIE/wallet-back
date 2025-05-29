package com.walletapi.entities

import com.walletapi.models.History
import com.walletapi.models.TransactionType
import jakarta.persistence.*
import org.hibernate.annotations.CreationTimestamp
import java.time.LocalDate

@Entity
class HistoryEntity(
    date: LocalDate?,
    description: String?,
    type: TransactionType,
    amount: Double?,
    balance: Double?,
    wallet: WalletEntity?
) {
    @Id
    @GeneratedValue(strategy = jakarta.persistence.GenerationType.SEQUENCE)
    @Column(unique = true, nullable = false)
    var id: Long? = null

    @Column(nullable = false)
    @CreationTimestamp
    var date: LocalDate? = date

    @Column(nullable = false)
    var description: String? = description

    @Column(nullable = false)
    @Enumerated(value = EnumType.STRING)
    var type: TransactionType = type

    @Column(nullable = false)
    var amount: Double? = amount

    @Column(nullable = false)
    var balance: Double? = balance

    @ManyToOne
    @JoinColumn(name = "wallet_id", nullable = false)
    var wallet: WalletEntity? = wallet

    fun toHistory(): History {
        return History(
            date = this.date ?: LocalDate.now(),
            description = this.description ?: "",
            type = this.type,
            amount = this.amount ?: 0.0,
            balance = this.balance ?: 0.0
        )
    }
}

package com.walletapi.entities

import com.walletapi.models.History
import com.walletapi.models.TransactionType
import jakarta.persistence.*
import org.hibernate.annotations.CreationTimestamp
import java.time.LocalDate


@Entity
class HistoryEntity(date: LocalDate?, description: String?, type: TransactionType, amount: Double?, balance: Double?) {
    @Id
    @GeneratedValue(strategy = jakarta.persistence.GenerationType.SEQUENCE)
    @Column(unique = true, nullable = false)
    var id: Long? = null

    @Column(nullable = false)
    @CreationTimestamp
    var date: LocalDate ? = null


    @Column(nullable = false)
    var description: String? = null

    @Column(nullable = false)
    @Enumerated(value = EnumType.STRING)
    lateinit var type: TransactionType

    @Column(nullable = false)
    var amount: Double? = null

    @Column(nullable = false)
    var balance: Double? = null


    fun toHistory(): History {
        return History(
            date = this.date ?: LocalDate.now(),
            description = this.description ?: "",
            type = this.type,
            amount = this.amount ?: 0.0,
            balance = 0.0
        )
    }
}
fun historyToEntity(history: History): HistoryEntity {
    return HistoryEntity(
        date = history.date,
        description = history.description,
        type = history.type,
        amount = history.amount,
        balance = history.balance
    )
}
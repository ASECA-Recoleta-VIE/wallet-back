package com.walletapi.models
import com.walletapi.models.User
import java.time.LocalDate

data class Wallet(
    private val name: String = "",
    private val balance: Double = 0.0,
    private val overdraft: Double = 0.0, // valor default de sobregiro
    private val history: List<History> = emptyList()
) {


    fun deposit(amount: Double, reason: String): Result<Wallet> {
        if (amount < 0) {
            return Result.failure(IllegalArgumentException("Amount must be positive"))
        }
        // adding the deposit to the history
        return Result.success(
            Wallet(
            name = this.name,
            overdraft = this.overdraft,
            balance = this.balance + amount,
            history = this.history + History(
                date = LocalDate.now(),
                description = reason,
                type = TransactionType.DEPOSIT,
                amount = amount,
                balance = this.balance + amount
        )
        ))
    }

    fun withdraw(amount: Double, reason: String = "Withdrawal"): Result<Wallet> {
        if (amount < 0) {
            return Result.failure(IllegalArgumentException("Amount must be positive"))
        }
        if (this.balance - amount < this.overdraft) {
            return Result.failure(IllegalArgumentException("Insufficient funds"))
        }
        // adding the withdrawal to the history
        return Result.success(
            Wallet(
            name = this.name,
            overdraft = this.overdraft,
            balance = this.balance - amount,
            history = this.history + History(
                date = LocalDate.now(),
                description = reason,
                type = TransactionType.WITHDRAWAL,
                amount = amount,
                balance = this.balance - amount
            )
        )
        )
    }


    private fun add(amount: Long): Wallet {
        if (amount < 0) {
            throw IllegalArgumentException("Amount must be positive")
        }
        return Wallet(
            name = this.name,
            overdraft = this.overdraft,
            balance = this.balance + amount,
            history = this.history
        )
    }

    private fun subtract(amount: Long): Wallet {
        if (amount < 0) {
            throw IllegalArgumentException("Amount must be positive")
        }
        if (this.balance + this.overdraft < amount) {
            throw IllegalArgumentException("Insufficient funds")
        }
        return Wallet(
            name = this.name,
            overdraft = this.overdraft,
            balance = this.balance - amount,
            history = this.history
        )
    }

    fun getBalance(): Double {
        return this.balance
    }
    fun getOverdraft(): Double {
        return this.overdraft
    }
    fun getHistory(): List<History> {
        return this.history
    }

    fun getName(): String {
        return this.name
    }

   fun transfer(to: Wallet, amount: Double, fromUser: User, toUser: User): Result<Pair<Wallet, Wallet>> {
        if (amount < 0) {
            return Result.failure(IllegalArgumentException("Amount must be positive"))
        }
        if (this.balance - amount < this.overdraft) {
            return Result.failure(IllegalArgumentException("Insufficient funds"))
        }

        val updatedSender = Wallet(
            name = this.name,
            overdraft = this.overdraft,
            balance = this.balance - amount,
            history = this.history + History(
                date = java.time.LocalDate.now(),
                description = "Transfer to ${toUser.fullName} with balance ${to.getBalance()}",
                type = TransactionType.TRANSFER_OUT,
                amount = amount,
                balance = this.balance - amount
            )
        )

        val updatedReceiver = Wallet(
            name = to.name,
            overdraft = to.getOverdraft(),
            balance = to.getBalance() + amount,
            history = to.getHistory() + History(
                date = java.time.LocalDate.now(),
                description = "Transfer from ${fromUser.fullName} with balance ${this.getBalance()}",
                type = TransactionType.TRANSFER_IN,
                amount = amount,
                balance = to.getBalance() + amount
            )
        )

        return Result.success(Pair(updatedSender, updatedReceiver))
    }



    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Wallet) return false

        if (balance != other.balance) return false
        if (overdraft != other.overdraft) return false
        if (history != other.history) return false

        return true
    }

    override fun hashCode(): Int {
        var result = balance.hashCode()
        result = 31 * result + overdraft.hashCode()
        result = 31 * result + history.hashCode()
        return result
    }
}

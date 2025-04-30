package com.walletapi.models

data class Wallet(
    private val balance: Double = 0.0,
    private val overdraft: Double = 0.0, // valor default de sobregiro
    private val history: List<History> = emptyList()
) {


    fun deposit(amount: Double): Result<Wallet> {
        if (amount < 0) {
            return Result.failure(IllegalArgumentException("Amount must be positive"))
        }
        // adding the deposit to the history
        return Result.success(
            Wallet(
            overdraft = this.overdraft,
            balance = this.balance + amount,
            history = this.history
        )
        )
    }

    fun withdraw(amount: Double): Result<Wallet> {
        if (amount < 0) {
            return Result.failure(IllegalArgumentException("Amount must be positive"))
        }
        if (this.balance - amount < this.overdraft) {
            return Result.failure(IllegalArgumentException("Insufficient funds"))
        }
        // adding the withdrawal to the history
        return Result.success(
            Wallet(
            overdraft = this.overdraft,
            balance = this.balance - amount,
            history = this.history
        )
        )
    }


    private fun add(amount: Long): Wallet {
        if (amount < 0) {
            throw IllegalArgumentException("Amount must be positive")
        }
        return Wallet(
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
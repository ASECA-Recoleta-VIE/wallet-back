package com.walletapi.services

import com.wallet.walletapi.models.Person
import com.wallet.walletapi.services.PersonService
import com.walletapi.models.History
import com.walletapi.models.TransactionType
import com.walletapi.models.Wallet
import java.time.LocalDate

class TransactionService(private val personService: PersonService) {


    fun makeTransaction(amount: Double,wallet: Wallet, wallet2: Wallet): Result<History> {
        if (amount < 0) {
            return Result.failure(IllegalArgumentException("Amount must be positive"))
        }
        if (wallet.getBalance() < amount) {
            return Result.failure(IllegalArgumentException("Insufficient funds"))
        }
        // Perform the transaction
        val newWallet = wallet.withdraw(amount).getOrThrow()
        val newWallet2 = wallet2.deposit(amount).getOrThrow()
        val history = History(
            date = LocalDate.now(),
            description = "Transfer from wallet1 to wallet2",
            type = TransactionType.TRANSFER_OUT,
            amount = amount,
            balance = newWallet.getBalance()
        )
        return Result.success(history)
    }
}
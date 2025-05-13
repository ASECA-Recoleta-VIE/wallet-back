package com.wallet.walletapi.services

import com.walletapi.services.TransactionService
import kotlin.test.Test

class TransferTest {

    @Test
    fun walletTransferShouldAddMoneyToBalance() {
        val wallet1: com.walletapi.models.Wallet = com.walletapi.models.Wallet()
        val wallet2: com.walletapi.models.Wallet = com.walletapi.models.Wallet()
        val wallet1WithMoney = wallet1.deposit(100.0).getOrThrow()
        val newWallets = wallet1WithMoney.transfer(wallet2, 50.0).getOrThrow()
        val finishedWallet1 = newWallets.first
        val finishedWallet2 = newWallets.second
        assert(finishedWallet1.getBalance() == 50.0)
        assert(finishedWallet2.getBalance() == 50.0)
    }


    @Test
    fun walletTransferShouldNotAllowOverdraft() {
        val wallet1 = com.walletapi.models.Wallet()
        val wallet2 = com.walletapi.models.Wallet()
        val wallet1WithMoney = wallet1.deposit(100.0).getOrThrow()
        val result = wallet1WithMoney.transfer(wallet2, 150.0)
        assert(result.isFailure)
        assert(wallet1WithMoney.getBalance() == 100.0)
        assert(wallet2.getBalance() == 0.0)
    }

    @Test
    fun walletTransferShouldNotAllowNegativeAmount() {
        val wallet1 = com.walletapi.models.Wallet()
        val wallet2 = com.walletapi.models.Wallet()
        val wallet1WithMoney = wallet1.deposit(100.0).getOrThrow()
        val result = wallet1WithMoney.transfer(wallet2, -50.0)
        assert(result.isFailure)
        assert(wallet1WithMoney.getBalance() == 100.0)
        assert(wallet2.getBalance() == 0.0)
    }


    @Test
    fun person1ShouldBeAbleToTransferToPerson2() {
        val personService = PersonService()
        val person1 = personService.createPerson("Lautaro González", "lautaro@gmail.com")
        val person2 = personService.createPerson("Pedro Perez", "pedro@gmail.com")
        val wallet1 = com.walletapi.models.Wallet()
        val newWallet1 = wallet1.deposit(100.0).getOrThrow()
        val wallet2 = com.walletapi.models.Wallet()
        val result = TransactionService(personService).makeTransaction(50.0, newWallet1,wallet2)
        println(result)
        assert(result.isSuccess)
        val history = result.getOrThrow()
        assert(history.description == "Transfer from wallet1 to wallet2")
        assert(history.type == com.walletapi.models.TransactionType.TRANSFER_OUT)
        assert(history.amount == 50.0)
    }
}
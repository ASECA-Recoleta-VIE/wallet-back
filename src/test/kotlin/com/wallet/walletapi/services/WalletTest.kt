package com.wallet.walletapi.services

import com.walletapi.models.Wallet
import kotlin.test.Test

class WalletTest {

    @Test
    fun walletDepositShouldAddMoneyToBalance() {
        val wallet: Wallet = Wallet()
        val finishedWallet = wallet.deposit(100.0).getOrNull()!!
        assert(finishedWallet.getBalance() == 100.0)
        assert(wallet.getBalance() == 0.0)
    }

    @Test
    fun walletWithdrawShouldRemoveMoneyFromBalance() {
        var wallet: Wallet = Wallet()
        wallet = wallet.deposit(100.0).getOrNull()!!
        val finishedWallet = wallet.withdraw(50.0).getOrThrow()
        assert(finishedWallet.getBalance() == 50.0)
        assert(wallet.getBalance() == 100.0)
    }


    @Test
    fun walletWithSamePropertiesShouldBeEqual() {
        var wallet1: Wallet = Wallet()
        var wallet2: Wallet = Wallet()
        wallet1 = wallet1.deposit(100.0).getOrNull()!!
        wallet2 = wallet2.deposit(100.0).getOrNull()!!
        assert(wallet1 == wallet2)
    }

    @Test
    fun walletWithSamePropertiesShouldHaveSameHashCode() {
        var wallet1: Wallet = Wallet()
        var wallet2: Wallet = Wallet()
        wallet1 = wallet1.deposit(100.0).getOrNull()!!
        wallet2 = wallet2.deposit(100.0).getOrNull()!!
        assert(wallet1.hashCode() == wallet2.hashCode())
    }

    @Test
    fun walletWithdrawShouldNotAllowOverdraft() {
        var wallet: Wallet = Wallet()
        wallet = wallet.deposit(100.0).getOrNull()!!
        val finishedWallet = wallet.withdraw(150.0)
        assert(finishedWallet.isFailure)
    }
//  @Test
//  fun walletTransferShouldAddMoneyToBalance() {
//        val wallet1: com.walletapi.models.Wallet = com.walletapi.models.Wallet()
//        val wallet2: com.walletapi.models.Wallet = com.walletapi.models.Wallet()
//        wallet1.deposit(100.0)
//        val finishedWallet1 = wallet1.transfer(wallet2, 50.0)
//        assert(finishedWallet1.balance == 50.0)
//        assert(wallet1.balance == 50.0)
//        assert(wallet2.balance == 50.0)
//    }
//
//
//    @Test
//    fun walletTransferShouldNotAllowOverdraft() {
//        val wallet1: com.walletapi.models.Wallet = com.walletapi.models.Wallet()
//        val wallet2: com.walletapi.models.Wallet = com.walletapi.models.Wallet()
//        wallet1.deposit(100.0)
//        val finishedWallet1 = wallet1.transfer(wallet2, 150.0)
//        assert(finishedWallet1.balance == 100.0)
//        assert(wallet1.balance == 100.0)
//        assert(wallet2.balance == 0.0)
//    }
//
//    @Test
//    fun walletTransferShouldNotAllowNegativeAmount() {
//        val wallet1: com.walletapi.models.Wallet = com.walletapi.models.Wallet()
//        val wallet2: com.walletapi.models.Wallet = com.walletapi.models.Wallet()
//        wallet1.deposit(100.0)
//        val finishedWallet1 = wallet1.transfer(wallet2, -50.0)
//        assert(finishedWallet1.balance == 100.0)
//        assert(wallet1.balance == 100.0)
//        assert(wallet2.balance == 0.0)
//    }

}
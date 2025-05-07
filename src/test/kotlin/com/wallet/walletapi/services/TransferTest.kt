package com.wallet.walletapi.services

import kotlin.test.Test

class TransferTest {

      @Test
  fun walletTransferShouldAddMoneyToBalance() {
        val wallet1: com.walletapi.models.Wallet = com.walletapi.models.Wallet()
        val wallet2: com.walletapi.models.Wallet = com.walletapi.models.Wallet()
        val wallet1WithMoney = wallet1.deposit(100.0).getOrNull()
        val newWallets = wallet1WithMoney?.transfer(wallet2, 50.0)?.getOrThrow()
        assert(newWallets != null)
          val finishedWallet1 = newWallets?.first
          val finishedWallet2 = newWallets?.second
        assert(finishedWallet1?.getBalance() == 50.0)
        assert(finishedWallet2?.getBalance() == 50.0)
    }


    @Test
    fun walletTransferShouldNotAllowOverdraft() {
        val wallet1 = com.walletapi.models.Wallet()
        val wallet2 = com.walletapi.models.Wallet()
        val wallet1WithMoney = wallet1.deposit(100.0).getOrNull()
        val result = wallet1WithMoney?.transfer(wallet2, 150.0)
        assert(result != null)
        assert(result!!.isFailure)
        assert(wallet1WithMoney.getBalance() == 100.0)
        assert(wallet2.getBalance() == 0.0)
    }
    @Test
    fun walletTransferShouldNotAllowNegativeAmount() {
        val wallet1 = com.walletapi.models.Wallet()
        val wallet2 = com.walletapi.models.Wallet()
        val wallet1WithMoney = wallet1.deposit(100.0).getOrNull()
        val result = wallet1WithMoney?.transfer(wallet2, -50.0)
        assert(result != null)
        assert(result!!.isFailure)
        assert(wallet1WithMoney.getBalance() == 100.0)
        assert(wallet2.getBalance() == 0.0)
    }
}
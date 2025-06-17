package com.walletapi.unit

import com.walletapi.models.Wallet
import com.walletapi.models.User
import kotlin.test.Test

class WalletTest {

    @Test
    fun walletDepositShouldAddMoneyToBalance() {
        val wallet: Wallet = Wallet()
        val finishedWallet = wallet.deposit(100.0, "Test deposit").getOrNull()!!
        assert(finishedWallet.getBalance() == 100.0)
        assert(wallet.getBalance() == 0.0)
    }

    @Test
    fun walletWithdrawShouldRemoveMoneyFromBalance() {
        var wallet: Wallet = Wallet()
        wallet = wallet.deposit(100.0, "Initial deposit").getOrNull()!!
        val finishedWallet = wallet.withdraw(50.0).getOrThrow()
        assert(finishedWallet.getBalance() == 50.0)
        assert(wallet.getBalance() == 100.0)
    }


    @Test
    fun walletWithSamePropertiesShouldBeEqual() {
        var wallet1: Wallet = Wallet()
        var wallet2: Wallet = Wallet()
        wallet1 = wallet1.deposit(100.0, "Test deposit").getOrNull()!!
        wallet2 = wallet2.deposit(100.0, "Test deposit").getOrNull()!!
        assert(wallet1 == wallet2)
    }

    @Test
    fun walletWithSamePropertiesShouldHaveSameHashCode() {
        var wallet1: Wallet = Wallet()
        var wallet2: Wallet = Wallet()
        wallet1 = wallet1.deposit(100.0, "Test deposit").getOrNull()!!
        wallet2 = wallet2.deposit(100.0, "Test deposit").getOrNull()!!
        assert(wallet1.hashCode() == wallet2.hashCode())
    }

    @Test
    fun walletWithdrawShouldNotAllowOverdraft() {
        var wallet: Wallet = Wallet()
        wallet = wallet.deposit(100.0, "Initial deposit").getOrNull()!!
        val finishedWallet = wallet.withdraw(150.0)
        assert(finishedWallet.isFailure)
        assert(finishedWallet.exceptionOrNull()?.message == "Insufficient funds")
    }

    @Test
    fun walletWithdrawShouldNotAllowNegativeAmount() {
        val wallet: Wallet = Wallet()
        val result = wallet.withdraw(-50.0)
        assert(result.isFailure)
        assert(result.exceptionOrNull()?.message == "Amount must be positive")
    }

    @Test
    fun walletDepositShouldNotAllowNegativeAmount() {
        val wallet: Wallet = Wallet()
        val result = wallet.deposit(-50.0, "Test deposit")
        assert(result.isFailure)
        assert(result.exceptionOrNull()?.message == "Amount must be positive")
    }

    @Test
    fun walletWithdrawShouldAllowZeroAmount() {
        var wallet: Wallet = Wallet()
        wallet = wallet.deposit(100.0, "Initial deposit").getOrNull()!!
        val finishedWallet = wallet.withdraw(0.0)
        assert(finishedWallet.isSuccess)
        assert(finishedWallet.getOrThrow().getBalance() == 100.0)
    }

    @Test
    fun walletDepositShouldAllowZeroAmount() {
        val wallet: Wallet = Wallet()
        val finishedWallet = wallet.deposit(0.0, "Test deposit")
        assert(finishedWallet.isSuccess)
        assert(finishedWallet.getOrThrow().getBalance() == 0.0)
    }

    @Test
    fun walletShouldHaveInitialBalanceOfZero() {
        val wallet: Wallet = Wallet()
        assert(wallet.getBalance() == 0.0)
    }

    @Test
    fun walletShouldHaveInitialOverdraftOfZero() {
        val wallet: Wallet = Wallet()
        assert(wallet.getOverdraft() == 0.0)
    }

    @Test
    fun walletShouldHaveEmptyHistoryInitially() {
        val wallet: Wallet = Wallet()
        assert(wallet.getHistory().isEmpty())
    }

    @Test
    fun walletShouldHaveName() {
        val wallet: Wallet = Wallet(name = "Test Wallet")
        assert(wallet.getName() == "Test Wallet")
    }

    @Test
    fun transferShouldDeductFromSourceAndAddToDestination() {
        var sourceWallet: Wallet = Wallet(name = "Source Wallet")
        val destinationWallet: Wallet = Wallet(name = "Destination Wallet")

        val sourceUser = User("Source User", "source@test.com", "Source123!")
        val destUser = User("Destination User", "dest@test.com", "Dest123!")

        sourceWallet = sourceWallet.deposit(200.0, "Initial deposit").getOrNull()!!

        val transferResult = sourceWallet.transfer(destinationWallet, 50.0, sourceUser, destUser)

        assert(transferResult.isSuccess)
        val (updatedSourceWallet, updatedDestinationWallet) = transferResult.getOrThrow()

        assert(updatedSourceWallet.getBalance() == 150.0)
        assert(updatedDestinationWallet.getBalance() == 50.0)
    }

    @Test
    fun transferShouldFailIfInsufficientFunds() {
        var sourceWallet: Wallet = Wallet(name = "Source Wallet")
        var destinationWallet: Wallet = Wallet(name = "Destination Wallet")

        val sourceUser = User("Source User", "source@test.com", "Source123!")
        val destUser = User("Destination User", "dest@test.com", "Dest123!")

        sourceWallet = sourceWallet.deposit(100.0, "Initial deposit").getOrNull()!!

        val transferResult = sourceWallet.transfer(destinationWallet, 150.0, sourceUser, destUser)

        assert(transferResult.isFailure)
        assert(transferResult.exceptionOrNull()?.message == "Insufficient funds")
    }

    @Test
    fun transferShouldFailIfNegativeAmount() {
        val sourceWallet: Wallet = Wallet(name = "Source Wallet")
        val destinationWallet: Wallet = Wallet(name = "Destination Wallet")

        val sourceUser = User("Source User", "source@test.com", "Source123!")
        val destUser = User("Destination User", "dest@test.com", "Dest123!")

        val transferResult = sourceWallet.transfer(destinationWallet, -50.0, sourceUser, destUser)

        assert(transferResult.isFailure)
        assert(transferResult.exceptionOrNull()?.message == "Amount must be positive")
    }

    @Test
    fun transferShouldFailIfTransferToSelf() {
        var sourceWallet: Wallet = Wallet(name = "Source Wallet")
        var destinationWallet: Wallet = Wallet(name = "Destination Wallet")

        val sameUser = User("Same User", "same@test.com", "SameUser123!")

        sourceWallet = sourceWallet.deposit(100.0, "Initial deposit").getOrNull()!!

        val transferResult = sourceWallet.transfer(destinationWallet, 50.0, sameUser, sameUser)

        assert(transferResult.isFailure)
        assert(transferResult.exceptionOrNull()?.message == "Self transfers are not allowed")
    }

    @Test
    fun transferShouldAllowZeroAmount() {
        var sourceWallet: Wallet = Wallet(name = "Source Wallet")
        var destinationWallet: Wallet = Wallet(name = "Destination Wallet")

        val sourceUser = User("Source User", "source@test.com", "Source123!")
        val destUser = User("Destination User", "dest@test.com", "Dest123!")

        sourceWallet = sourceWallet.deposit(100.0, "Initial deposit").getOrNull()!!

        val transferResult = sourceWallet.transfer(destinationWallet, 0.0, sourceUser, destUser)

        assert(transferResult.isSuccess)
        val (updatedSourceWallet, updatedDestinationWallet) = transferResult.getOrThrow()

        assert(updatedSourceWallet.getBalance() == 100.0)
        assert(updatedDestinationWallet.getBalance() == 0.0)
    }

    @Test
    fun transferShouldCreateTransferHistoryEntries() {
        val sourceWallet = Wallet(name = "Source", balance = 100.0)
        val destWallet = Wallet(name = "Destination")

        val sourceUser = User("Source User", "source@test.com", "Source123!")
        val destUser = User("Destination User", "dest@test.com", "Dest123!")

        val transferResult = sourceWallet.transfer(destWallet, 50.0, sourceUser, destUser)

        assert(transferResult.isSuccess)

        val (updatedSourceWallet, updatedDestinationWallet) = transferResult.getOrThrow()

        assert(updatedSourceWallet.getHistory().size == 1)
        assert(updatedDestinationWallet.getHistory().size == 1)

        val sourceHistoryEntry = updatedSourceWallet.getHistory().first()
        val destHistoryEntry = updatedDestinationWallet.getHistory().first()

        assert(sourceHistoryEntry.amount == 50.0)
        assert(sourceHistoryEntry.type == com.walletapi.models.TransactionType.TRANSFER_OUT)
        assert(destHistoryEntry.amount == 50.0)
        assert(destHistoryEntry.type == com.walletapi.models.TransactionType.TRANSFER_IN)
    }

    @Test
    fun shouldNotTransferWhenInsufficientFunds() {
        val sourceWallet = Wallet(name = "Source", balance = 50.0)
        val destWallet = Wallet(name = "Destination")

        val sourceUser = User("Source User", "source@test.com", "Source123!")
        val destUser = User("Destination User", "dest@test.com", "Dest123!")

        val transferResult = sourceWallet.transfer(destWallet, 100.0, sourceUser, destUser)

        assert(transferResult.isFailure)
        assert(transferResult.exceptionOrNull()?.message == "Insufficient funds")
    }

    @Test
    fun transferToSelfShouldFail() {
        val wallet = Wallet(name = "Self Wallet", balance = 100.0)

        val sameUser = User("Same User", "same@test.com", "SameUser123!")

        val transferResult = wallet.transfer(wallet, 50.0, sameUser, sameUser)

        assert(transferResult.isFailure)
        assert(transferResult.exceptionOrNull()?.message == "Self transfers are not allowed")
    }

    @Test
    fun transferShouldNotAllowNegativeOrZeroAmount() {
        val sourceWallet = Wallet(name = "Source", balance = 100.0)
        val destWallet = Wallet(name = "Destination")

        val sourceUser = User("Source User", "source@test.com", "Source123!")
        val destUser = User("Destination User", "dest@test.com", "Dest123!")

        val negativeTransferResult = sourceWallet.transfer(destWallet, -50.0, sourceUser, destUser)
        val zeroTransferResult = sourceWallet.transfer(destWallet, 0.0, sourceUser, destUser)

        assert(negativeTransferResult.isFailure)
        assert(negativeTransferResult.exceptionOrNull()?.message == "Amount must be positive")

        assert(zeroTransferResult.isSuccess)
        val (updatedSourceWallet, updatedDestinationWallet) = zeroTransferResult.getOrThrow()
        assert(updatedSourceWallet.getBalance() == 100.0)
        assert(updatedDestinationWallet.getBalance() == 0.0)
    }


}

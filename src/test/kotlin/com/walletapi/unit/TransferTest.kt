package com.walletapi.unit

import com.walletapi.models.TransactionType
import com.walletapi.models.User
import com.walletapi.models.Wallet
import com.walletapi.services.UserService
import kotlin.test.Test
import org.springframework.http.HttpStatus

class TransferTest {

    @Test
    fun walletTransferShouldAddMoneyToBalance() {
        val wallet1: Wallet = Wallet()
        val wallet2: Wallet = Wallet()
        val wallet1WithMoney = wallet1.deposit(100.0, "Initial deposit").getOrThrow()

        // Create dummy users for the transfer
        val fromUser = User("From User", "from@example.com", "FromUser123!")
        val toUser = User("To User", "to@example.com", "ToUser123!")

        val newWallets = wallet1WithMoney.transfer(wallet2, 50.0, fromUser, toUser).getOrThrow()
        val finishedWallet1 = newWallets.first
        val finishedWallet2 = newWallets.second
        assert(finishedWallet1.getBalance() == 50.0)
        assert(finishedWallet2.getBalance() == 50.0)
    }


    @Test
    fun walletTransferShouldNotAllowOverdraft() {
        val wallet1 = Wallet()
        val wallet2 = Wallet()
        val wallet1WithMoney = wallet1.deposit(100.0, "Initial deposit").getOrThrow()

        // Create dummy users for the transfer
        val fromUser = User("From User", "from@example.com", "FromUser123!")
        val toUser = User("To User", "to@example.com", "ToUser123!")

        val result = wallet1WithMoney.transfer(wallet2, 150.0, fromUser, toUser)
        assert(result.isFailure)
        assert(wallet1WithMoney.getBalance() == 100.0)
        assert(wallet2.getBalance() == 0.0)
    }

    @Test
    fun walletTransferShouldNotAllowNegativeAmount() {
        val wallet1 = Wallet()
        val wallet2 = Wallet()
        val wallet1WithMoney = wallet1.deposit(100.0, "Initial deposit").getOrThrow()

        // Create dummy users for the transfer
        val fromUser = User("From User", "from@example.com", "FromUser123!")
        val toUser = User("To User", "to@example.com", "ToUser123!")

        val result = wallet1WithMoney.transfer(wallet2, -50.0, fromUser, toUser)
        assert(result.isFailure)
        assert(wallet1WithMoney.getBalance() == 100.0)
        assert(wallet2.getBalance() == 0.0)
    }


    @Test
    fun user1ShouldBeAbleToTransferToUser2() {
        // Create users with wallets
        val user1 = User("Lautaro González", "lautaro@gmail.com", "Lautaro123!")
        val user2 = User("Pedro Perez", "pedro@gmail.com", "Pedro123!")

        // Create and fund wallet1
        val wallet1 = Wallet()
        val wallet1WithMoney = wallet1.deposit(100.0, "Initial deposit").getOrThrow()

        // Create wallet2
        val wallet2 = Wallet()

        // Perform transfer
        val transferResult = wallet1WithMoney.transfer(wallet2, 50.0, user1, user2)

        // Verify transfer was successful
        assert(transferResult.isSuccess)

        // Get the updated wallets
        val (updatedWallet1, updatedWallet2) = transferResult.getOrThrow()

        // Verify balances
        assert(updatedWallet1.getBalance() == 50.0)
        assert(updatedWallet2.getBalance() == 50.0)

        // Verify history
        val history = updatedWallet1.getHistory().last()
        assert(history.type == TransactionType.TRANSFER_OUT)
        assert(history.amount == 50.0)
    }

    @Test
    fun walletTransferShouldNotAllowSelfTransfer() {
        val wallet1 = Wallet()
        val wallet2 = Wallet()
        val wallet1WithMoney = wallet1.deposit(100.0, "Initial deposit").getOrThrow()

        // Create same user for source and destination
        val user = User("Same User", "same@example.com", "SameUser123!")

        val result = wallet1WithMoney.transfer(wallet2, 50.0, user, user)

        // Verify transfer fails due to self transfer
        assert(result.isFailure)
        assert(result.exceptionOrNull()?.message?.contains("Self transfers are not allowed") == true)

        // Verify balances remain unchanged
        assert(wallet1WithMoney.getBalance() == 100.0)
        assert(wallet2.getBalance() == 0.0)
    }

    @Test
    fun walletTransferShouldAllowZeroAmount() {
        val wallet1 = Wallet()
        val wallet2 = Wallet()
        val wallet1WithMoney = wallet1.deposit(100.0, "Initial deposit").getOrThrow()

        val fromUser = User("From User", "from@example.com", "FromUser123!")
        val toUser = User("To User", "to@example.com", "ToUser123!")

        val result = wallet1WithMoney.transfer(wallet2, 0.0, fromUser, toUser)

        // Verify transfer succeeds
        assert(result.isSuccess)
        val (updatedWallet1, updatedWallet2) = result.getOrThrow()
        // Verify balances remain unchanged
        assert(updatedWallet1.getBalance() == 100.0)
        assert(updatedWallet2.getBalance() == 0.0)
    }

    @Test
    fun walletTransferShouldSucceedWithExactAvailableFunds() {
        val wallet1 = Wallet()
        val wallet2 = Wallet()
        val wallet1WithMoney = wallet1.deposit(100.0, "Initial deposit").getOrThrow()

        val fromUser = User("From User", "from@example.com", "FromUser123!")
        val toUser = User("To User", "to@example.com", "ToUser123!")

        // Transfer the exact balance amount
        val result = wallet1WithMoney.transfer(wallet2, 100.0, fromUser, toUser)

        // Verify transfer succeeds
        assert(result.isSuccess)

        val (updatedWallet1, updatedWallet2) = result.getOrThrow()

        // Verify balances
        assert(updatedWallet1.getBalance() == 0.0)
        assert(updatedWallet2.getBalance() == 100.0)

        // Verify history
        assert(updatedWallet1.getHistory().last().type == TransactionType.TRANSFER_OUT)
        assert(updatedWallet1.getHistory().last().amount == 100.0)
        assert(updatedWallet2.getHistory().last().type == TransactionType.TRANSFER_IN)
        assert(updatedWallet2.getHistory().last().amount == 100.0)
    }



    @Test
    fun walletTransferExceedingOverdraftLimitShouldFail() {
        // Create wallet with overdraft capability
        val wallet1 = Wallet(overdraft = 50.0)
        val wallet2 = Wallet()
        val wallet1WithMoney = wallet1.deposit(100.0, "Initial deposit").getOrThrow()

        val fromUser = User("From User", "from@example.com", "FromUser123!")
        val toUser = User("To User", "to@example.com", "ToUser123!")

        // Transfer more than allowed with overdraft
        val result = wallet1WithMoney.transfer(wallet2, 151.0, fromUser, toUser)

        // Verify transfer fails
        assert(result.isFailure)
        assert(result.exceptionOrNull()?.message?.contains("Insufficient funds") == true)

        // Verify balances remain unchanged
        assert(wallet1WithMoney.getBalance() == 100.0)
        assert(wallet2.getBalance() == 0.0)
    }
}

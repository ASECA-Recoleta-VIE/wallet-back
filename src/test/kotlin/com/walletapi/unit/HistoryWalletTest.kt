package com.walletapi.unit

import com.walletapi.models.History
import com.walletapi.models.TransactionType
import com.walletapi.models.User
import com.walletapi.models.Wallet
import java.time.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class HistoryWalletTest {

    @Test
    fun depositShouldCreateHistoryEntryWithCorrectData() {
        val wallet = Wallet(name = "Test Wallet")
        val initialHistorySize = wallet.getHistory().size

        val updatedWallet = wallet.deposit(100.0, "Test deposit").getOrThrow()
        val history = updatedWallet.getHistory()

        assertEquals(initialHistorySize + 1, history.size)
        val latestEntry = history.last()

        assertEquals(LocalDate.now(), latestEntry.date)
        assertEquals("Test deposit", latestEntry.description)
        assertEquals(TransactionType.DEPOSIT, latestEntry.type)
        assertEquals(100.0, latestEntry.amount)
        assertEquals(100.0, latestEntry.balance)
    }

    @Test
    fun withdrawalShouldCreateHistoryEntryWithCorrectData() {
        var wallet = Wallet(name = "Test Wallet")
        wallet = wallet.deposit(200.0, "Initial deposit").getOrThrow()
        val initialHistorySize = wallet.getHistory().size

        val updatedWallet = wallet.withdraw(50.0, "Test withdrawal").getOrThrow()
        val history = updatedWallet.getHistory()

        assertEquals(initialHistorySize + 1, history.size)
        val latestEntry = history.last()

        assertEquals(LocalDate.now(), latestEntry.date)
        assertEquals("Test withdrawal", latestEntry.description)
        assertEquals(TransactionType.WITHDRAWAL, latestEntry.type)
        assertEquals(50.0, latestEntry.amount)
        assertEquals(150.0, latestEntry.balance)
    }

    @Test
    fun transferShouldCreateHistoryEntriesInBothWallets() {
        var sourceWallet = Wallet(name = "Source Wallet")
        val destWallet = Wallet(name = "Destination Wallet")

        val sourceUser = User("Source User", "source@test.com", "Source123!")
        val destUser = User("Destination User", "dest@test.com", "Dest123!")

        sourceWallet = sourceWallet.deposit(200.0, "Initial deposit").getOrThrow()
        val sourceHistorySize = sourceWallet.getHistory().size
        val destHistorySize = destWallet.getHistory().size

        val (updatedSourceWallet, updatedDestWallet) = sourceWallet
            .transfer(destWallet, 75.0, sourceUser, destUser)
            .getOrThrow()

        // Check source wallet history
        val sourceHistory = updatedSourceWallet.getHistory()
        assertEquals(sourceHistorySize + 1, sourceHistory.size)
        val sourceLatestEntry = sourceHistory.last()

        assertEquals(LocalDate.now(), sourceLatestEntry.date)
        assertEquals("Transfer to ${destUser.fullName}", sourceLatestEntry.description)
        assertEquals(TransactionType.TRANSFER_OUT, sourceLatestEntry.type)
        assertEquals(75.0, sourceLatestEntry.amount)
        assertEquals(125.0, sourceLatestEntry.balance)

        // Check destination wallet history
        val destHistory = updatedDestWallet.getHistory()
        assertEquals(destHistorySize + 1, destHistory.size)
        val destLatestEntry = destHistory.last()

        assertEquals(LocalDate.now(), destLatestEntry.date)
        assertEquals("Transfer from ${sourceUser.fullName}", destLatestEntry.description)
        assertEquals(TransactionType.TRANSFER_IN, destLatestEntry.type)
        assertEquals(75.0, destLatestEntry.amount)
        assertEquals(75.0, destLatestEntry.balance)
    }

    @Test
    fun debinShouldCreateHistoryEntryWithCorrectData() {
        val wallet = Wallet(name = "Test Wallet")
        val initialHistorySize = wallet.getHistory().size

        val updatedWallet = wallet.debin(50.0, "Test DEBIN").getOrThrow()
        val history = updatedWallet.getHistory()

        assertEquals(initialHistorySize + 1, history.size)
        val latestEntry = history.last()

        assertEquals(LocalDate.now(), latestEntry.date)
        assertEquals("Test DEBIN", latestEntry.description)
        assertEquals(TransactionType.DEBIN_REQUEST, latestEntry.type)
        assertEquals(50.0, latestEntry.amount)
        assertEquals(50.0, latestEntry.balance)
    }

    @Test
    fun multipleTransactionsShouldCreateOrderedHistory() {
        var wallet = Wallet(name = "Test Wallet")

        // Perform multiple transactions
        wallet = wallet.deposit(100.0, "Initial deposit").getOrThrow()
        wallet = wallet.withdraw(25.0, "First withdrawal").getOrThrow()
        wallet = wallet.deposit(50.0, "Second deposit").getOrThrow()
        wallet = wallet.withdraw(10.0, "Second withdrawal").getOrThrow()

        val history = wallet.getHistory()
        assertEquals(4, history.size)

        // Check order of history entries (first to last)
        assertEquals("Initial deposit", history[0].description)
        assertEquals(TransactionType.DEPOSIT, history[0].type)
        assertEquals(100.0, history[0].balance)

        assertEquals("First withdrawal", history[1].description)
        assertEquals(TransactionType.WITHDRAWAL, history[1].type)
        assertEquals(75.0, history[1].balance)

        assertEquals("Second deposit", history[2].description)
        assertEquals(TransactionType.DEPOSIT, history[2].type)
        assertEquals(125.0, history[2].balance)

        assertEquals("Second withdrawal", history[3].description)
        assertEquals(TransactionType.WITHDRAWAL, history[3].type)
        assertEquals(115.0, history[3].balance)
    }

    @Test
    fun walletBalanceShouldMatchLastHistoryEntry() {
        var wallet = Wallet(name = "Test Wallet")

        wallet = wallet.deposit(100.0, "Initial deposit").getOrThrow()
        assertEquals(100.0, wallet.getBalance())
        assertEquals(100.0, wallet.getHistory().last().balance)

        wallet = wallet.withdraw(30.0, "Withdrawal").getOrThrow()
        assertEquals(70.0, wallet.getBalance())
        assertEquals(70.0, wallet.getHistory().last().balance)

        wallet = wallet.deposit(25.0, "Second deposit").getOrThrow()
        assertEquals(95.0, wallet.getBalance())
        assertEquals(95.0, wallet.getHistory().last().balance)
    }

    @Test
    fun failedTransactionsShouldNotCreateHistory() {
        val wallet = Wallet(name = "Test Wallet", balance = 100.0)
        val initialHistorySize = wallet.getHistory().size

        // This withdrawal should fail
        val result = wallet.withdraw(150.0, "Failed withdrawal")
        assertTrue(result.isFailure)

        // History should remain unchanged
        assertEquals(initialHistorySize, wallet.getHistory().size)
    }

    @Test
    fun transferBetweenWalletsShouldMaintainConsistentHistory() {
        var sourceWallet = Wallet(name = "Source Wallet")
        var destWallet = Wallet(name = "Destination Wallet")

        val sourceUser = User("Source User", "source@test.com", "Source123!")
        val destUser = User("Destination User", "dest@test.com", "Dest123!")

        // First deposit to source wallet
        sourceWallet = sourceWallet.deposit(200.0, "Initial deposit").getOrThrow()

        // First transfer
        var (updatedSource, updatedDest) = sourceWallet
            .transfer(destWallet, 50.0, sourceUser, destUser)
            .getOrThrow()

        // Second transfer
        val transferResult = updatedSource
            .transfer(updatedDest, 30.0, sourceUser, destUser)
            .getOrThrow()

        updatedSource = transferResult.first
        updatedDest = transferResult.second

        // Check history sizes
        assertEquals(3, updatedSource.getHistory().size) // Initial deposit + 2 transfers
        assertEquals(2, updatedDest.getHistory().size)   // 2 transfers in

        // Check balances
        assertEquals(120.0, updatedSource.getBalance())  // 200 - 50 - 30
        assertEquals(80.0, updatedDest.getBalance())     // 0 + 50 + 30

        // Check source wallet history balance progression
        val sourceHistory = updatedSource.getHistory()
        assertEquals(200.0, sourceHistory[0].balance)
        assertEquals(150.0, sourceHistory[1].balance)
        assertEquals(120.0, sourceHistory[2].balance)

        // Check destination wallet history balance progression
        val destHistory = updatedDest.getHistory()
        assertEquals(50.0, destHistory[0].balance)
        assertEquals(80.0, destHistory[1].balance)
    }

    @Test
    fun zeroAmountTransactionsShouldCreateValidHistoryEntries() {
        var wallet = Wallet(name = "Test Wallet")

        // Zero deposit
        wallet = wallet.deposit(0.0, "Zero deposit").getOrThrow()
        var latestEntry = wallet.getHistory().last()
        assertEquals(0.0, latestEntry.amount)
        assertEquals(0.0, latestEntry.balance)

        // Non-zero deposit followed by zero withdrawal
        wallet = wallet.deposit(100.0, "Deposit").getOrThrow()
        wallet = wallet.withdraw(0.0, "Zero withdrawal").getOrThrow()
        latestEntry = wallet.getHistory().last()
        assertEquals(0.0, latestEntry.amount)
        assertEquals(100.0, latestEntry.balance)
    }

    @Test
    fun overdraftLimitShouldAffectWithdrawalHistory() {
        var wallet = Wallet(name = "Test Wallet", balance = 100.0, overdraft = -50.0)

        // Withdrawal within overdraft limit
        wallet = wallet.withdraw(120.0, "Withdrawal with overdraft").getOrThrow()
        val latestEntry = wallet.getHistory().last()

        assertEquals(-20.0, wallet.getBalance())
        assertEquals(-20.0, latestEntry.balance)
        assertEquals(120.0, latestEntry.amount)

        // Attempt withdrawal beyond overdraft limit
        val failedWithdrawal = wallet.withdraw(40.0, "Beyond overdraft")
        assertTrue(failedWithdrawal.isFailure)
    }

    @Test
    fun transferFailuresShouldNotAffectEitherWallet() {
        var sourceWallet = Wallet(name = "Source Wallet", balance = 50.0)
        val destWallet = Wallet(name = "Destination Wallet")

        val sourceUser = User("Source User", "source@test.com", "Source123!")
        val destUser = User("Destination User", "dest@test.com", "Dest123!")

        val sourceHistorySize = sourceWallet.getHistory().size
        val destHistorySize = destWallet.getHistory().size

        // Attempt transfer with insufficient funds
        val transferResult = sourceWallet.transfer(destWallet, 100.0, sourceUser, destUser)
        assertTrue(transferResult.isFailure)

        // Neither wallet should change
        assertEquals(sourceHistorySize, sourceWallet.getHistory().size)
        assertEquals(destHistorySize, destWallet.getHistory().size)
        assertEquals(50.0, sourceWallet.getBalance())
        assertEquals(0.0, destWallet.getBalance())
    }

    @Test
    fun debinShouldCorrectlyTrackFundsAndHistory() {
        var wallet = Wallet(name = "Test Wallet")

        // Process a DEBIN request
        wallet = wallet.debin(75.0, "DEBIN payment").getOrThrow()
        assertEquals(75.0, wallet.getBalance())

        val historyEntry = wallet.getHistory().last()
        assertEquals(TransactionType.DEBIN_REQUEST, historyEntry.type)
        assertEquals(75.0, historyEntry.amount)
        assertEquals(75.0, historyEntry.balance)
    }
}

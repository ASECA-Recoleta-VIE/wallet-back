package com.walletapi.unit

import com.walletapi.models.History
import com.walletapi.models.TransactionType
import java.time.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

class HistoryTest {

    @Test
    fun historyCreationWithValidDataShouldSucceed() {
        val date = LocalDate.now()
        val history = History(
            date = date,
            description = "Test transaction",
            type = TransactionType.DEPOSIT,
            amount = 100.0,
            balance = 100.0
        )

        assertEquals(date, history.date)
        assertEquals("Test transaction", history.description)
        assertEquals(TransactionType.DEPOSIT, history.type)
        assertEquals(100.0, history.amount)
        assertEquals(100.0, history.balance)
    }

    @Test
    fun historiesWithSameDataShouldBeEqual() {
        val date = LocalDate.now()
        val history1 = History(
            date = date,
            description = "Test transaction",
            type = TransactionType.DEPOSIT,
            amount = 100.0,
            balance = 100.0
        )

        val history2 = History(
            date = date,
            description = "Test transaction",
            type = TransactionType.DEPOSIT,
            amount = 100.0,
            balance = 100.0
        )

        assertEquals(history1, history2)
        assertEquals(history1.hashCode(), history2.hashCode())
    }

    @Test
    fun historiesWithDifferentDataShouldNotBeEqual() {
        val date = LocalDate.now()
        val history1 = History(
            date = date,
            description = "Test transaction 1",
            type = TransactionType.DEPOSIT,
            amount = 100.0,
            balance = 100.0
        )

        val history2 = History(
            date = date,
            description = "Test transaction 2",
            type = TransactionType.DEPOSIT,
            amount = 100.0,
            balance = 100.0
        )

        assertNotEquals(history1, history2)
        assertNotEquals(history1.hashCode(), history2.hashCode())
    }

    @Test
    fun historiesWithDifferentTypesShouldNotBeEqual() {
        val date = LocalDate.now()
        val depositHistory = History(
            date = date,
            description = "Test transaction",
            type = TransactionType.DEPOSIT,
            amount = 100.0,
            balance = 100.0
        )

        val withdrawalHistory = History(
            date = date,
            description = "Test transaction",
            type = TransactionType.WITHDRAWAL,
            amount = 100.0,
            balance = 0.0
        )

        assertNotEquals(depositHistory, withdrawalHistory)
    }

    @Test
    fun historyWithDifferentDatesShouldNotBeEqual() {
        val today = LocalDate.now()
        val yesterday = today.minusDays(1)

        val historyToday = History(
            date = today,
            description = "Test transaction",
            type = TransactionType.DEPOSIT,
            amount = 100.0,
            balance = 100.0
        )

        val historyYesterday = History(
            date = yesterday,
            description = "Test transaction",
            type = TransactionType.DEPOSIT,
            amount = 100.0,
            balance = 100.0
        )

        assertNotEquals(historyToday, historyYesterday)
    }

    @Test
    fun historyToStringShouldContainAllData() {
        val date = LocalDate.now()
        val history = History(
            date = date,
            description = "Test transaction",
            type = TransactionType.DEPOSIT,
            amount = 100.0,
            balance = 100.0
        )

        val stringRepresentation = history.toString()

        assert(stringRepresentation.contains(date.toString()))
        assert(stringRepresentation.contains("Test transaction"))
        assert(stringRepresentation.contains("DEPOSIT"))
        assert(stringRepresentation.contains("100.0"))
    }

    @Test
    fun historyCopyShouldCreateDistinctObject() {
        val date = LocalDate.now()
        val originalHistory = History(
            date = date,
            description = "Original transaction",
            type = TransactionType.DEPOSIT,
            amount = 100.0,
            balance = 100.0
        )

        val copiedHistory = originalHistory.copy(
            description = "Modified transaction"
        )

        assertEquals(date, copiedHistory.date)
        assertEquals("Modified transaction", copiedHistory.description)
        assertEquals(TransactionType.DEPOSIT, copiedHistory.type)
        assertEquals(100.0, copiedHistory.amount)
        assertEquals(100.0, copiedHistory.balance)

        // Original should remain unchanged
        assertEquals("Original transaction", originalHistory.description)
    }

    @Test
    fun historyComponentsShouldBeAccessible() {
        val date = LocalDate.now()
        val history = History(
            date = date,
            description = "Test transaction",
            type = TransactionType.TRANSFER_IN,
            amount = 100.0,
            balance = 200.0
        )

        // Component access using standard getter methods
        assertEquals(date, history.date)
        assertEquals("Test transaction", history.description)
        assertEquals(TransactionType.TRANSFER_IN, history.type)
        assertEquals(100.0, history.amount)
        assertEquals(200.0, history.balance)

        // Test component access using destructuring
        val (historyDate, historyDesc, historyType, historyAmount, historyBalance) = history
        assertEquals(date, historyDate)
        assertEquals("Test transaction", historyDesc)
        assertEquals(TransactionType.TRANSFER_IN, historyType)
        assertEquals(100.0, historyAmount)
        assertEquals(200.0, historyBalance)
    }

    @Test
    fun historyWithZeroAmountShouldBeValid() {
        val date = LocalDate.now()
        val history = History(
            date = date,
            description = "Zero amount transaction",
            type = TransactionType.DEPOSIT,
            amount = 0.0,
            balance = 100.0
        )

        assertEquals(0.0, history.amount)
    }

    @Test
    fun historyWithNegativeBalanceShouldBeValid() {
        val date = LocalDate.now()
        val history = History(
            date = date,
            description = "Overdraft transaction",
            type = TransactionType.WITHDRAWAL,
            amount = 150.0,
            balance = -50.0
        )

        assertEquals(-50.0, history.balance)
    }

    @Test
    fun historyWithHistoricalDateShouldBeValid() {
        val historicalDate = LocalDate.of(2000, 1, 1)
        val history = History(
            date = historicalDate,
            description = "Historical transaction",
            type = TransactionType.DEPOSIT,
            amount = 100.0,
            balance = 100.0
        )

        assertEquals(historicalDate, history.date)
    }

    @Test
    fun historyWithFutureDateShouldBeValid() {
        val futureDate = LocalDate.now().plusYears(1)
        val history = History(
            date = futureDate,
            description = "Future transaction",
            type = TransactionType.DEPOSIT,
            amount = 100.0,
            balance = 100.0
        )

        assertEquals(futureDate, history.date)
    }

    @Test
    fun historyWithEmptyDescriptionShouldBeValid() {
        val date = LocalDate.now()
        val history = History(
            date = date,
            description = "",
            type = TransactionType.DEPOSIT,
            amount = 100.0,
            balance = 100.0
        )

        assertEquals("", history.description)
    }

    @Test
    fun historyWithLongDescriptionShouldBeValid() {
        val date = LocalDate.now()
        val longDescription = "A".repeat(1000)
        val history = History(
            date = date,
            description = longDescription,
            type = TransactionType.DEPOSIT,
            amount = 100.0,
            balance = 100.0
        )

        assertEquals(longDescription, history.description)
        assertEquals(1000, history.description.length)
    }

    @Test
    fun historyWithSpecialCharactersInDescriptionShouldBeValid() {
        val date = LocalDate.now()
        val specialDescription = "Special chars: !@#$%^&*()_+{}|:<>?~`-=[]\\;',./€£¥©®™"
        val history = History(
            date = date,
            description = specialDescription,
            type = TransactionType.DEPOSIT,
            amount = 100.0,
            balance = 100.0
        )

        assertEquals(specialDescription, history.description)
    }

    @Test
    fun historyWithLargeAmountShouldPreservePrecision() {
        val date = LocalDate.now()
        val largeAmount = 9999999999.99
        val history = History(
            date = date,
            description = "Large amount transaction",
            type = TransactionType.DEPOSIT,
            amount = largeAmount,
            balance = largeAmount
        )

        assertEquals(largeAmount, history.amount)
    }

    @Test
    fun historyWithSmallAmountShouldPreservePrecision() {
        val date = LocalDate.now()
        val smallAmount = 0.00000001
        val history = History(
            date = date,
            description = "Small amount transaction",
            type = TransactionType.DEPOSIT,
            amount = smallAmount,
            balance = smallAmount
        )

        assertEquals(smallAmount, history.amount)
    }

    @Test
    fun allTransactionTypesShouldBeSupported() {
        val date = LocalDate.now()
        val depositHistory = History(date, "Deposit", TransactionType.DEPOSIT, 100.0, 100.0)
        val withdrawalHistory = History(date, "Withdrawal", TransactionType.WITHDRAWAL, 50.0, 50.0)
        val transferInHistory = History(date, "Transfer In", TransactionType.TRANSFER_IN, 25.0, 75.0)
        val transferOutHistory = History(date, "Transfer Out", TransactionType.TRANSFER_OUT, 25.0, 25.0)
        val debinRequestHistory = History(date, "DEBIN Request", TransactionType.DEBIN_REQUEST, 10.0, 35.0)

        assertEquals(TransactionType.DEPOSIT, depositHistory.type)
        assertEquals(TransactionType.WITHDRAWAL, withdrawalHistory.type)
        assertEquals(TransactionType.TRANSFER_IN, transferInHistory.type)
        assertEquals(TransactionType.TRANSFER_OUT, transferOutHistory.type)
        assertEquals(TransactionType.DEBIN_REQUEST, debinRequestHistory.type)
    }
}

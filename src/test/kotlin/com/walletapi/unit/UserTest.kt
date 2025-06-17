package com.walletapi.unit

import com.walletapi.models.User
import com.walletapi.models.Wallet
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

class UserTest {

    @Test
    fun userWithNameAndEmailShouldBeCreated() {
        val fullName = "Lautaro González"
        val email = "lauti@example.com"
        val password = "Password1!"

        val user = User(fullName, email, password)

        assertEquals(fullName, user.fullName)
        assertEquals(email, user.email)
        assertEquals(password, user.password)
        assertEquals(emptyList(), user.wallets)
    }

    @Test
    fun usersWithSameDataShouldBeEqual() {
        val user1 = User("John Doe", "john@example.com", "Password1!")
        val user2 = User("John Doe", "john@example.com", "Password1!")

        assertEquals(user1, user2)
        assertEquals(user1.hashCode(), user2.hashCode())
    }

    @Test
    fun usersWithDifferentDataShouldNotBeEqual() {
        val user1 = User("John Doe", "john@example.com", "Password1!")
        val user2 = User("Jane Doe", "jane@example.com", "Password1!")

        assertNotEquals(user1, user2)
        assertNotEquals(user1.hashCode(), user2.hashCode())
    }

    @Test
    fun userCanHaveWallets() {
        val wallet = Wallet("Test Wallet", 100.0)
        val user = User("John Doe", "john@example.com", "Password1!", listOf(wallet))

        assertEquals(1, user.wallets.size)
        assertEquals("Test Wallet", user.wallets[0].getName())
        assertEquals(100.0, user.wallets[0].getBalance())
    }
}
package com.wallet.walletapi.services

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class PersonTest {

    @Test
    fun `should create person with name and email`() {
        val service = PersonService()

        val person = service.createPerson("Lautaro González", "lauti@example.com")

        assertEquals("Lautaro González", person.fullName)
        assertEquals("lauti@example.com", person.email)
    }

    @Test
    fun `should retrieve person by email`() {
        val service = PersonService()
        val created = service.createPerson("Juan Pérez", "juan@example.com")

        val found = service.getPersonByEmail("juan@example.com")

        assertEquals(created, found)
    }

    @Test
    fun `should return null when person is not found`() {
        val service = PersonService()

        val result = service.getPersonByEmail("noone@example.com")

        assertNull(result)
    }
}

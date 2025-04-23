package com.wallet.walletapi.services

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.util.*

class PersonServiceTest {

    @Test
    fun `should create person with name and email`() {
        val service = PersonService()

        val person = service.createPerson("Lautaro González", "lauti@example.com")

        assertEquals("Lautaro González", person.fullName)
        assertEquals("lauti@example.com", person.email)
        assertNotNull(person.id)
    }

    @Test
    fun `should retrieve person by email`() {
        val service = PersonService()
        val created = service.createPerson("Juan Pérez", "juan@example.com")

        val found = service.getPersonByEmail("juan@example.com")

        assertEquals(created.id, found?.id)
    }

    @Test
    fun `should return null when person is not found`() {
        val service = PersonService()

        val result = service.getPersonByEmail("noone@example.com")

        assertNull(result)
    }
}

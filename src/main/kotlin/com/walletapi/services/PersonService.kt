package com.wallet.walletapi.services

import com.wallet.walletapi.models.Person
import com.walletapi.models.Wallet

class PersonService {
    private val people = mutableListOf<Person>()

    fun createPerson(fullName: String, email: String): Person {
        val person = Person(fullName = fullName, email = email)
        people.add(person)
        return person
    }
    fun getPersonByEmail(email: String): Person? {
        return people.find { it.email == email }
    }

}

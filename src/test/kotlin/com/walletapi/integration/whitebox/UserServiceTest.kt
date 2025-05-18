package com.walletapi.integration.whitebox

import com.walletapi.domain_services.DomainUserService
import com.walletapi.dto.PasswordValidation
import com.walletapi.models.User
import com.walletapi.repositories.UserRepository
import com.walletapi.services.UserService
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import org.mockito.Mockito.`when`
import org.springframework.http.HttpStatus

class UserServiceTest {

    private lateinit var userService: UserService
    private lateinit var mockDomainUserService: DomainUserService
    private lateinit var mockUserRepository: UserRepository

    @BeforeEach
    fun setup() {
        mockDomainUserService = Mockito.mock(DomainUserService::class.java)
        mockUserRepository = Mockito.mock(UserRepository::class.java)

        userService = UserService()
        userService.userService = mockDomainUserService
        userService.userRepository = mockUserRepository

        `when`(mockUserRepository.existsByEmail(Mockito.anyString())).thenReturn(false)

        `when`(mockDomainUserService.createUser(
            Mockito.anyString(), 
            Mockito.anyString(), 
            Mockito.anyString(), 
            Mockito.anyList()
        )).thenAnswer { invocation ->
            val fullName = invocation.getArgument<String>(0)
            val email = invocation.getArgument<String>(1)
            val password = invocation.getArgument<String>(2)
            User(fullName, email, password)
        }
    }

    @Test
    fun shouldCreateUserWithNameAndEmail() {
        val fullName = "Lautaro González"
        val email = "lauti@example.com"
        val password = "Password1!"
        val response = userService.createUser(fullName, email, password)
        assertTrue(response.statusCode == HttpStatus.CREATED)
        val user = response.body as User
        assertEquals(fullName, user.fullName)
        assertEquals(email, user.email)
    }

    @Test
    fun shouldNotCreateUserWithInvalidPassword() {
        val fullName = "Juan Pérez"
        val email = "juan@example.com"
        val password = "password" 
        val response = userService.createUser(fullName, email, password)
        assertTrue(response.statusCode == HttpStatus.BAD_REQUEST)
    }

    @Test
    fun shouldNotCreateUserWithEmptyFields() {
        val fullName = ""
        val email = "email@example.com"
        val password = "Password1!"
        val response = userService.createUser(fullName, email, password)
        assertTrue(response.statusCode == HttpStatus.BAD_REQUEST)
    }
}

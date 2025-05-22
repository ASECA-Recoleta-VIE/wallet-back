package com.walletapi.integration.blackbox

import com.walletapi.dto.request.LoginRequest
import com.walletapi.dto.request.RegisterRequest
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.cookie
import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get

@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc
class UserApiTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    @Test
    fun shouldRegisterUserSuccessfully() {
        val request = RegisterRequest(
            fullName = "Test User",
            email = "test.register.${System.currentTimeMillis()}@example.com", // Unique email
            password = "Password1!"
        )

        mockMvc.perform(
            post("/api/users/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
        )
            .andExpect(status().isCreated)
    }

    @Test
    fun shouldNotRegisterUserWithInvalidData() {
        val request = RegisterRequest(
            fullName = "",
            email = "invalid@example.com",
            password = "Password1!"
        )

        mockMvc.perform(
            post("/api/users/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
        )
            .andExpect(status().isBadRequest)
    }

    @Test
    fun shouldLoginUserAndReceiveAuthCookie() {
        // First register a user
        val email = "test.login@example.com"
        val registerRequest = RegisterRequest(
            fullName = "Login Test User",
            email = email,
            password = "Password1!"
        )

        mockMvc.perform(
            post("/api/users/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(registerRequest))
        )
            .andExpect(status().isCreated)

        // Then login with that user
        val loginRequest = LoginRequest(
            email = email,
            password = "Password1!"
        )

        mockMvc.perform(
            post("/api/users/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginRequest))
        )
            .andExpect(status().isOk)
            .andExpect(cookie().exists("token"))
    }

    @Test
    fun shouldNotLoginWithInvalidCredentials() {
        // First register a user
        val email = "test.login@example.com"
        val registerRequest = RegisterRequest(
            fullName = "Login Test User",
            email = email,
            password = "Password1!"
        )

        mockMvc.perform(
            post("/api/users/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(registerRequest))
        )
            .andExpect(status().isCreated)
        val loginRequest = LoginRequest(
            email = "test.login@example.com",
            password = "WrongPassword!"
        )

        mockMvc.perform(
            post("/api/users/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginRequest))
        )
            .andExpect(status().isUnauthorized)
        }

    @Test
    fun shouldNotLoginWithNonExistentUser() {
        val loginRequest = LoginRequest(
            email = "test.login@example.com",
            password = "WrongPassword!"
        )
        mockMvc.perform(
            post("/api/users/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginRequest))
        )
            .andExpect(status().isNotFound)
    }

    @Test
    fun shouldNotRegisterUserWithExistingEmail() {
        val email = "test.register.${System.currentTimeMillis()}@example.com"
        val request = RegisterRequest(
            fullName = "Test User",
            email = email,
            password = "Password1!"
        )

        mockMvc.perform(
            post("/api/users/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
        )
            .andExpect(status().isCreated)

        // Attempt to register the same user again
        mockMvc.perform(
            post("/api/users/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
        )
            .andExpect(status().isBadRequest)
    }

    @Test
    fun shouldNotRegisterUserWithWeakPassword() {
        val request = RegisterRequest(
            fullName = "Test User",
            email = "test.weakpassword.${System.currentTimeMillis()}@example.com", // Unique email
            password = "weak" // Weak password
        )

        mockMvc.perform(
            post("/api/users/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
        )
            .andExpect(status().isBadRequest)
    }
    @Test
    fun shouldCreateWalletForUserWhenRegistering() {
        val request = RegisterRequest(
            fullName = "Test User",
            email = "test.wallet.${System.currentTimeMillis()}@example.com",
            password = "Password1!"
        )

        // Paso 1: Registrar usuario
        val registerResult = mockMvc.perform(
            post("/api/users/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
        )
            .andExpect(status().isCreated)
            .andReturn()

        val responseBody = registerResult.response.contentAsString
        val userIdNode = objectMapper.readTree(responseBody).get("id")
        assert(userIdNode != null) { "Response does not contain 'id' field" }
        val userId = userIdNode.asText()

        // Paso 2: Obtener wallets del usuario
        val walletsResult = mockMvc.perform(
            get("/api/wallets")
                .param("userId", userId)
        )
            .andExpect(status().isOk)
            .andReturn()

        val walletsJson = objectMapper.readTree(walletsResult.response.contentAsString)
        assert(walletsJson.isArray) { "Expected wallet list in response" }
        assert(walletsJson.size() > 0) { "No wallet was created for user" }

        val wallet = walletsJson[0]
        assert(wallet.get("name").asText() == "Main Wallet") { "Wallet name is incorrect" }
        assert(wallet.get("balance").asDouble() == 10000.0) { "Wallet balance is incorrect" }
    }
}
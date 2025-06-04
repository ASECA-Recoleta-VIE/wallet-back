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
import com.walletapi.repositories.HistoryRepository
import com.walletapi.repositories.UserRepository
import com.walletapi.repositories.WalletRepository
import io.jsonwebtoken.Claims
import java.security.Key
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.SignatureAlgorithm
import jakarta.servlet.http.Cookie
import org.junit.jupiter.api.BeforeEach
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.ResultActions
import javax.crypto.spec.SecretKeySpec

@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc
class UserApiTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var userRepository: UserRepository
    @Autowired
    private lateinit var historyRepository: HistoryRepository
    @Autowired
    private lateinit var walletRepository: WalletRepository


    private val logger: Logger = LoggerFactory.getLogger(UserApiTest::class.java)


    val secretKey = "abcdefghijklmnopqrstuvwxyz1234567890!@#$%^&*()_+-=[pipe]{}|;':\",.<div>?/"
    private val key: Key = SecretKeySpec(secretKey.toByteArray(), SignatureAlgorithm.HS256.jcaName)

    @BeforeEach
    fun setup() {
        logger.info("Clearing database for test")
        try {
            historyRepository.deleteAll()
            walletRepository.deleteAll()
            userRepository.deleteAll()
            userRepository.flush()
            logger.info("Database cleared successfully")
        } catch (e: Exception) {
            logger.error("Error clearing database: ${e.message}", e)
            throw e
        }
    }


    @Autowired
    private lateinit var objectMapper: ObjectMapper

    @Test
    fun shouldRegisterUserSuccessfully() {
        val request = RegisterRequest(
            fullName = "Test User",
            email = "example@email.com", // Unique email
            password = "Password1!"
        )

        logger.info("Registering user with email: ${request.email}")

        logger.info("Checking if user is registered in the database")
        val user = userRepository.findByEmail(request.email)
        assert(user != null) { "User should be registered in the database" }
        assert(user!!.fullName == request.fullName) { "User full name should match" }
        assert(user.email == request.email) { "User email should match" }
        assert(user.password != request.password) { "User password should be hashed" }
        logger.info("Database check passed, user registered successfully")

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

        // check db
        logger.info("Checking if user is not registered in the database")
        val user = userRepository.findByEmail(request.email)
        assert(user == null) { "User should not be registered in the database" }
        logger.info("Database check passed, user not registered as expected")
    }

    @Test
    fun shouldLoginUserAndReceiveAuthCookie() {
        // First register a user
        val email = "test.login@example.com"
        val password = "Password1!"
        val registerRequest = RegisterRequest(
            fullName = "Login Test User",
            email,
            password
        )

        mockMvc.perform(
            post("/api/users/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(registerRequest))
        )
            .andExpect(status().isCreated)

        // Then login with that user
        val loginRequest = LoginRequest(
            email,
            password
        )

        val request: ResultActions = mockMvc.perform(
            post("/api/users/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginRequest))
        )
            .andExpect(status().isOk)
            .andExpect(cookie().exists("token"))

        // check that the cookie's subject is the user's id
        val user = userRepository.findByEmail(email)
        assert(user != null) { "User should be registered in the database" }
        val tokenCookie: Cookie? = request.andReturn().response.getCookie("token")
        assert(tokenCookie != null) { "Token cookie should be present in the response" }
        assert(tokenCookie!!.value.isNotEmpty()) { "Token cookie value should not be empty" }
        logger.info("User logged in successfully, received token cookie with value: ${tokenCookie.value}")
        val claims: Claims = Jwts.parserBuilder()
            .setSigningKey(key)
            .build()
            .parseClaimsJws(tokenCookie.value)
            .body

        assert(claims.subject == user!!.id) { "Token subject should match user ID" }
    }
}
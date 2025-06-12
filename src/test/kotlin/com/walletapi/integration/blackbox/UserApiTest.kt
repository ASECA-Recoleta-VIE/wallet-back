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
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
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
        walletRepository.deleteAll()
        historyRepository.deleteAll()
        userRepository.deleteAll()
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

        val response = mockMvc.perform(
            post("/api/users/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
        )
            .andExpect(status().isCreated)

        // check db
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
    fun shouldNotRegisterUserWithWeakPassword() {
        val weakPassword = listOf<String>(
            "hi",
            "password",
            "12345678",
            "weakpassword",
            "Password",
            "Pass1234"
        )

        weakPassword.forEach { password ->
            val request = RegisterRequest(
                fullName = "Weak Password User",
                email = "example@mail.com",
                password = password
            )
            logger.info("Attempting to register user with weak password: $password")
            mockMvc.perform(
                post("/api/users/register")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request))
            )
                .andExpect(status().isBadRequest)
        }
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


    @Test
    fun listUsersByEmailPrefixShouldReturnUsers() {
        // Register multiple users
        val password = "Password1!"
        val users = ArrayList<RegisterRequest>(20)
        for (i in 0..19) {
            val email = if (i % 2 == 0) "even${i}@example.com" else "odd${i}example.com"
            val registerRequest = RegisterRequest(
                fullName = if (i % 2 == 0) "Even User $i" else "Odd User $i",
                email = email,
                password = password
            )
            users.add(registerRequest)
        }
        users.forEach { user ->
            mockMvc.perform(
                post("/api/users/register")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(user))
            )
                .andExpect(status().isCreated)
        }

        //get a valid cookie for the user
        val loginRequest = LoginRequest(
            email = "even0@example.com",
            password = password
        )
        val loginResponse = mockMvc.perform(
            post("/api/users/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginRequest))
        )
            .andExpect(status().isOk)
            .andExpect(cookie().exists("token"))
            .andReturn()

        val tokenCookie: Cookie? = loginResponse.response.getCookie("token")

        // List users by email prefix "even"
        val prefix = "even"
        val response = mockMvc.perform(
            get("/api/users/list")
                .param("prefix", prefix)
                .contentType(MediaType.APPLICATION_JSON)
                .cookie(tokenCookie!!) // Use the valid token cookie
        )
            .andExpect(status().isOk)
            .andExpect { result ->
                val users = objectMapper.readValue(result.response.contentAsString, List::class.java)
                assert(users.size == 10) { "Should return 10 users with prefix '$prefix'" }
                users.forEach { user ->
                    assert((user as Map<String, String>)["email"]!!.startsWith(prefix)) {
                        "User email should start with prefix '$prefix'"
                    }
                }
            }
        logger.info("List users by email prefix '$prefix' test passed, found ${response.andReturn().response.contentAsString.length} users")

        // List users by email prefix "odd"
        val oddPrefix = "odd"
        mockMvc.perform(
            get("/api/users/list")
                .param("prefix", oddPrefix)
                .contentType(MediaType.APPLICATION_JSON)
                .cookie(tokenCookie!!) // Use the valid token cookie
        )
            .andExpect(status().isOk)
            .andExpect { result ->
                val users = objectMapper.readValue(result.response.contentAsString, List::class.java)
                assert(users.size == 10) { "Should return 10 users with prefix '$oddPrefix'" }
                users.forEach { user ->
                    assert((user as Map<String, String>)["email"]!!.startsWith(oddPrefix)) {
                        "User email should start with prefix '$oddPrefix'"
                    }
                }
            }
        logger.info("List users by email prefix '$oddPrefix' test passed, found ${response.andReturn().response.contentAsString.length} users")
    }
}
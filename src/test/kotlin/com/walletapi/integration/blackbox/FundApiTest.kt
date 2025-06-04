package com.walletapi.integration.blackbox

import com.fasterxml.jackson.databind.ObjectMapper
import com.walletapi.config.TestConfig
import com.walletapi.dto.response.WalletResponse
import com.walletapi.helper.UserHelperService
import jakarta.servlet.http.Cookie
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc
class FundApiTest {


    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var userHelperService: UserHelperService

    @Autowired
    private lateinit var fakeApiService: TestConfig.TestFakeApiService

    private val email = "test@example.com"
    private val name = "Test User"
    private val password = "Password123!"
    private var userCookie: Cookie? = null

    @BeforeEach
    fun setup() {
        // Create a user
        mockMvc.perform(
            post("/api/users/register")
                .contentType("application/json")
                .content(
                    """
                    {
                        "fullName": "$name",
                        "email": "$email",
                        "password": "$password"
                    }
                    """.trimIndent()
                )
        )
            .andExpect { status().isOk() }

        // Login the user and get the token
        val response = mockMvc.perform(
            post("/api/users/login")
                .contentType("application/json")
                .content(
                    """
                    {
                        "email": "$email",
                        "password": "$password"
                    }
                    """.trimIndent()
                )
        )
            .andExpect { status().isOk() }
            .andReturn().response

        userCookie = response.getCookie("token")
    }

    @Test
    fun requestFundsShouldDepositToUserWalletWhenFundsAreAvailable() {
        // Configure the fake API service to return a successful response
        fakeApiService.configureFundsAvailable()

        // Request funds
        mockMvc.perform(
            post("/request-funds")
                .cookie(userCookie)
                .contentType("application/json")
                .content(
                    """
                    {
                        "amount": 250.00,
                        "description": "Test fund request"
                    }
                    """.trimIndent()
                )
        )
            .andExpect(status().isOk)
            .andExpect { result ->
                val content: WalletResponse = ObjectMapper().readValue(
                    result.response.contentAsString,
                    WalletResponse::class.java
                )
                assert(content.balance == 10250.0) { "Wallet balance should be 10250.0 after fund request" }
            }

        // Persistence check
        val user = userHelperService.getUserWithWalletsAndHistory(email)
        assert(user != null) { "User should be registered in the database" }
        val wallet = user!!.wallets.firstOrNull()
        assert(wallet != null) { "User should have at least one wallet" }
        assert(wallet!!.balance == 10250.0) { "Wallet balance should be 10250.0 after fund request" }
    }

    @Test
    fun requestFundsShouldFailWhenFundsAreNotAvailable() {
        // Configure the fake API service to return a response indicating funds are not available
        fakeApiService.configureFundsNotAvailable()

        // Request funds
        mockMvc.perform(
            post("/request-funds")
                .cookie(userCookie)
                .contentType("application/json")
                .content(
                    """
                    {
                        "amount": 250.00,
                        "description": "Test fund request"
                    }
                    """.trimIndent()
                )
        )
            .andExpect(status().isBadRequest)

        // Persistence check - balance should remain unchanged
        val user = userHelperService.getUserWithWalletsAndHistory(email)
        assert(user != null) { "User should be registered in the database" }
        val wallet = user!!.wallets.firstOrNull()
        assert(wallet != null) { "User should have at least one wallet" }
        assert(wallet!!.balance == 10000.0) { "Wallet balance should remain unchanged at 10000.0" }
    }
}

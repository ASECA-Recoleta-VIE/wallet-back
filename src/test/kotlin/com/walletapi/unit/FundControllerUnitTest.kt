package com.walletapi.unit

import com.walletapi.controllers.FundController
import com.walletapi.dto.request.EmailTransactionRequest
import com.walletapi.dto.request.FundAvailabilityRequest
import com.walletapi.dto.response.ApiResponse
import com.walletapi.dto.response.FundAvailabilityResponse
import com.walletapi.dto.response.WalletResponse
import com.walletapi.entities.UserEntity
import com.walletapi.services.FakeApiService
import com.walletapi.services.WalletService
import jakarta.servlet.http.HttpServletRequest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.Mockito.`when`
import org.mockito.Mockito.verify
import org.mockito.junit.jupiter.MockitoExtension
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import java.math.BigDecimal
import java.time.LocalDateTime
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

@ExtendWith(MockitoExtension::class)
class FundControllerUnitTest {

    @Mock
    private lateinit var fakeApiService: FakeApiService

    @Mock
    private lateinit var walletService: WalletService

    @Mock
    private lateinit var httpRequest: HttpServletRequest

    @InjectMocks
    private lateinit var fundController: FundController

    private lateinit var testUser: UserEntity

    @BeforeEach
    fun setUp() {
        // Create test user
        testUser = UserEntity(
            fullName = "Test User",
            email = "test@example.com",
            password = "hashedPassword123",
            wallets = mutableListOf()
        )
        // Set ID manually
        val idField = UserEntity::class.java.getDeclaredField("id")
        idField.isAccessible = true
        idField.set(testUser, UUID.randomUUID().toString())
    }

    @Test
    fun `test user not found returns 404`() {
        // Arrange
        val request = FundAvailabilityRequest(
            amount = BigDecimal("100.00"),
            accountId = "ACC123",
            description = "Test transaction"
        )
        
        `when`(httpRequest.getAttribute("user")).thenReturn(null)

        // Act
        val response = fundController.requestFunds(request, httpRequest)

        // Assert
        assertEquals(HttpStatus.NOT_FOUND, response.statusCode)
    }

    @Test
    fun `test successful fund request`() {
        // Arrange
        val request = FundAvailabilityRequest(
            amount = BigDecimal("100.00"),
            accountId = "ACC123",
            description = "Test transaction"
        )
        
        val apiResponse = ApiResponse(
            success = true,
            message = "Funds are available",
            data = FundAvailabilityResponse(
                available = true,
                amount = request.amount,
                accountId = request.accountId!!,
                currentBalance = BigDecimal("500.00")
            ),
            timestamp = LocalDateTime.now(),
            transactionId = "DEB-12345"
        )
        
        val walletResponse = WalletResponse(
            name = "Main Wallet",
            balance = 1100.00,
            currency = "USD"
        )
        
        `when`(httpRequest.getAttribute("user")).thenReturn(testUser)
        `when`(fakeApiService.checkFundAvailability(request)).thenReturn(apiResponse)
        `when`(walletService.requestFunds(testUser, EmailTransactionRequest(
            amount = request.amount.toDouble(),
            description = request.description
        ))).thenReturn(walletResponse)

        // Act
        val response = fundController.requestFunds(request, httpRequest)

        // Assert
        assertEquals(HttpStatus.OK, response.statusCode)
        assertNotNull(response.body)
        assertEquals("Main Wallet", response.body?.name)
        assertEquals(1100.00, response.body?.balance)
        assertEquals("USD", response.body?.currency)
    }
}
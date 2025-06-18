package com.walletapi.integration

import ch.qos.logback.classic.Logger
import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.core.read.ListAppender
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
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers.any
import org.mockito.ArgumentMatchers.eq
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.Mockito.`when`
import org.mockito.Mockito.verify
import org.mockito.junit.jupiter.MockitoExtension
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import java.math.BigDecimal
import java.time.LocalDateTime
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

@ExtendWith(MockitoExtension::class)
class FundControllerIntegrationTest {

    @Mock
    private lateinit var fakeApiService: FakeApiService

    @Mock
    private lateinit var walletService: WalletService

    @Mock
    private lateinit var httpRequest: HttpServletRequest

    @InjectMocks
    private lateinit var fundController: FundController

    private val loggingEventCaptor: ArgumentCaptor<ILoggingEvent> = ArgumentCaptor.forClass(ILoggingEvent::class.java)

    private lateinit var logAppender: ListAppender<ILoggingEvent>
    private lateinit var testUser: UserEntity

    @BeforeEach
    fun setUp() {
        // Set up logging capture
        logAppender = ListAppender()
        logAppender.start()
        val logger = LoggerFactory.getLogger(FundController::class.java) as Logger
        logger.addAppender(logAppender)

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

        // Create the expected EmailTransactionRequest
        val expectedEmailRequest = EmailTransactionRequest(
            amount = request.amount.toDouble(),
            description = request.description
        )

        // Verify wallet service was called with the expected parameters
        verify(walletService).requestFunds(testUser, expectedEmailRequest)
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

        // Verify log message
        assert(logAppender.list.any { it.message.contains("User not found in request") })
    }

    @Test
    fun `test account ID required returns 400`() {
        // Arrange
        val request = FundAvailabilityRequest(
            amount = BigDecimal("100.00"),
            accountId = null,
            description = "Test transaction"
        )

        val apiResponse = ApiResponse(
            success = false,
            message = "Account ID is required",
            data = FundAvailabilityResponse(
                available = false,
                amount = request.amount,
                accountId = "",
                currentBalance = BigDecimal.ZERO
            ),
            timestamp = LocalDateTime.now(),
            transactionId = "DEB-12345"
        )

        `when`(httpRequest.getAttribute("user")).thenReturn(testUser)
        `when`(fakeApiService.checkFundAvailability(request)).thenReturn(apiResponse)

        // Act
        val response = fundController.requestFunds(request, httpRequest)

        // Assert
        assertEquals(HttpStatus.BAD_REQUEST, response.statusCode)

        // Verify log message
        assert(logAppender.list.any { it.message.contains("Account ID is required") })
    }

    @Test
    fun `test account not found returns 404`() {
        // Arrange
        val request = FundAvailabilityRequest(
            amount = BigDecimal("100.00"),
            accountId = "INVALID_ACC",
            description = "Test transaction"
        )

        val apiResponse = ApiResponse(
            success = true,
            message = "Account not found",
            data = FundAvailabilityResponse(
                available = false,
                amount = request.amount,
                accountId = request.accountId!!,
                currentBalance = BigDecimal.ZERO
            ),
            timestamp = LocalDateTime.now(),
            transactionId = "DEB-12345"
        )

        `when`(httpRequest.getAttribute("user")).thenReturn(testUser)
        `when`(fakeApiService.checkFundAvailability(request)).thenReturn(apiResponse)

        // Act
        val response = fundController.requestFunds(request, httpRequest)

        // Assert
        assertEquals(HttpStatus.NOT_FOUND, response.statusCode)

        // Verify log message
        assert(logAppender.list.any { it.message.contains("Account not found") })
    }

    @Test
    fun `test insufficient funds returns 403`() {
        // Arrange
        val request = FundAvailabilityRequest(
            amount = BigDecimal("1500.00"),
            accountId = "ACC123",
            description = "Test transaction"
        )

        val apiResponse = ApiResponse(
            success = true,
            message = "Insufficient funds",
            data = FundAvailabilityResponse(
                available = false,
                amount = request.amount,
                accountId = request.accountId!!,
                currentBalance = BigDecimal("1000.00")
            ),
            timestamp = LocalDateTime.now(),
            transactionId = "DEB-12345"
        )

        `when`(httpRequest.getAttribute("user")).thenReturn(testUser)
        `when`(fakeApiService.checkFundAvailability(request)).thenReturn(apiResponse)

        // Act
        val response = fundController.requestFunds(request, httpRequest)

        // Assert
        assertEquals(HttpStatus.FORBIDDEN, response.statusCode)

        // Verify log message
        assert(logAppender.list.any { it.message.contains("Insufficient funds") })
    }

    @Test
    fun `test funds not available returns 403`() {
        // Arrange
        val request = FundAvailabilityRequest(
            amount = BigDecimal("100.00"),
            accountId = "ACC123",
            description = "Test transaction"
        )

        val apiResponse = ApiResponse(
            success = false,
            message = "Funds are not available",
            data = FundAvailabilityResponse(
                available = false,
                amount = request.amount,
                accountId = request.accountId!!,
                currentBalance = BigDecimal("1000.00")
            ),
            timestamp = LocalDateTime.now(),
            transactionId = "DEB-12345"
        )

        `when`(httpRequest.getAttribute("user")).thenReturn(testUser)
        `when`(fakeApiService.checkFundAvailability(request)).thenReturn(apiResponse)

        // Act
        val response = fundController.requestFunds(request, httpRequest)

        // Assert
        assertEquals(HttpStatus.FORBIDDEN, response.statusCode)

        // Verify log message
        assert(logAppender.list.any { it.message.contains("Insufficient funds") })
    }

    @Test
    fun `test service exception returns 500`() {
        // Arrange
        val request = FundAvailabilityRequest(
            amount = BigDecimal("100.00"),
            accountId = "ACC123",
            description = "Test transaction"
        )

        `when`(httpRequest.getAttribute("user")).thenReturn(testUser)
        `when`(fakeApiService.checkFundAvailability(request)).thenThrow(RuntimeException("Test exception"))

        // Act
        val response = fundController.requestFunds(request, httpRequest)

        // Assert
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.statusCode)

        // Verify log message
        assert(logAppender.list.any { it.message.contains("Error processing fund request") })
    }

    @ParameterizedTest
    @CsvSource(
        "100.00, ACC123, Test transaction, true, Funds are available, true, 1000.00, 200",
        "100.00, '', Test transaction, false, Account ID is required, false, 0.00, 400",
        "100.00, INVALID_ACC, Test transaction, true, Account not found, false, 0.00, 404",
        "1500.00, ACC123, Test transaction, true, Insufficient funds, false, 1000.00, 403",
        "100.00, ACC123, Test transaction, false, Funds are not available, false, 1000.00, 403"
    )
    fun `test fund request with different scenarios`(
        amount: BigDecimal,
        accountId: String?,
        description: String,
        responseSuccess: Boolean,
        responseMessage: String,
        fundsAvailable: Boolean,
        currentBalance: BigDecimal,
        expectedStatus: Int
    ) {
        // Arrange
        val request = FundAvailabilityRequest(
            amount = amount,
            accountId = if (accountId.isNullOrEmpty()) null else accountId,
            description = description
        )

        val apiResponse = ApiResponse(
            success = responseSuccess,
            message = responseMessage,
            data = FundAvailabilityResponse(
                available = fundsAvailable,
                amount = request.amount,
                accountId = request.accountId ?: "",
                currentBalance = currentBalance
            ),
            timestamp = LocalDateTime.now(),
            transactionId = "DEB-12345"
        )

        `when`(httpRequest.getAttribute("user")).thenReturn(testUser)
        `when`(fakeApiService.checkFundAvailability(request)).thenReturn(apiResponse)

        if (responseSuccess && fundsAvailable) {
            val walletResponse = WalletResponse(
                name = "Main Wallet",
                balance = (currentBalance.toDouble() + amount.toDouble()),
                currency = "USD"
            )

            // Create a specific EmailTransactionRequest to avoid using matchers
            val emailRequest = EmailTransactionRequest(
                amount = amount.toDouble(),
                description = description
            )

            `when`(walletService.requestFunds(testUser, emailRequest)).thenReturn(walletResponse)
        }

        // Act
        val response = fundController.requestFunds(request, httpRequest)

        // Assert
        assertEquals(HttpStatus.valueOf(expectedStatus), response.statusCode)

        // Verify log message
        assert(logAppender.list.any { it.message.contains(responseMessage) ||
                                     it.message.contains("Checking fund availability") ||
                                     it.message.contains("User not found") ||
                                     it.message.contains("Error processing") })
    }
}

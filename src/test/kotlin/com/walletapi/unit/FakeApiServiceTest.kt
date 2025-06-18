package com.walletapi.unit

import com.walletapi.dto.request.FundAvailabilityRequest
import com.walletapi.dto.response.ApiResponse
import com.walletapi.dto.response.FundAvailabilityResponse
import com.walletapi.services.FakeApiService
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers.any
import org.mockito.ArgumentMatchers.eq
import org.mockito.Captor
import org.mockito.Mock
import org.mockito.Mockito.`when`
import org.mockito.Mockito.verify
import org.mockito.junit.jupiter.MockitoExtension
import org.springframework.core.ParameterizedTypeReference
import org.springframework.http.HttpEntity
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.client.HttpClientErrorException
import org.springframework.web.client.RestTemplate
import java.io.IOException
import java.math.BigDecimal
import java.time.LocalDateTime
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlin.test.fail

@ExtendWith(MockitoExtension::class)
class FakeApiServiceTest {

    @Mock
    private lateinit var restTemplate: RestTemplate

    private lateinit var fakeApiService: FakeApiService

    @Captor
    private lateinit var httpEntityCaptor: ArgumentCaptor<HttpEntity<FundAvailabilityRequest>>

    @BeforeEach
    fun setUp() {
        fakeApiService = FakeApiService(restTemplate)
        // Set the fakeApiBaseUrl field using reflection
        val field = FakeApiService::class.java.getDeclaredField("fakeApiBaseUrl")
        field.isAccessible = true
        field.set(fakeApiService, "http://localhost:9090")
    }

    @Test
    fun `test successful fund availability check`() {
        // Arrange
        val request = FundAvailabilityRequest(
            amount = BigDecimal("100.00"),
            accountId = "ACC123",
            description = "Test transaction"
        )

        val responseData = FundAvailabilityResponse(
            available = true,
            amount = request.amount,
            accountId = request.accountId!!,
            currentBalance = BigDecimal("500.00")
        )

        val apiResponse = ApiResponse(
            success = true,
            message = "Funds are available",
            data = responseData,
            timestamp = LocalDateTime.now(),
            transactionId = "DEB-${System.currentTimeMillis()}-1234"
        )

        val responseEntity = ResponseEntity.ok(apiResponse)

        `when`(restTemplate.exchange(
            eq("http://localhost:9090/api/check-funds"),
            eq(HttpMethod.POST),
            any(),
            any<ParameterizedTypeReference<ApiResponse<FundAvailabilityResponse>>>()
        )).thenReturn(responseEntity)

        // Act
        val result = fakeApiService.checkFundAvailability(request)

        // Assert
        verify(restTemplate).exchange(
            eq("http://localhost:9090/api/check-funds"),
            eq(HttpMethod.POST),
            httpEntityCaptor.capture(),
            any<ParameterizedTypeReference<ApiResponse<FundAvailabilityResponse>>>()
        )

        val capturedEntity = httpEntityCaptor.value
        assertEquals(request, capturedEntity.body)
        assertEquals("application/json", capturedEntity.headers.contentType.toString())

        assertEquals(true, result.success)
        assertEquals("Funds are available", result.message)
        assertNotNull(result.data)
        assertEquals(true, result.data?.available)
        assertEquals(BigDecimal("100.00"), result.data?.amount)
        assertEquals("ACC123", result.data?.accountId)
        assertEquals(BigDecimal("500.00"), result.data?.currentBalance)
    }

    @Test
    fun `test account ID required error`() {
        // Arrange
        val request = FundAvailabilityRequest(
            amount = BigDecimal("100.00"),
            accountId = null,
            description = "Test transaction"
        )

        // We don't need to create an ApiResponse object here since we're mocking an exception

        `when`(restTemplate.exchange(
            eq("http://localhost:9090/api/check-funds"),
            eq(HttpMethod.POST),
            any(),
            any<ParameterizedTypeReference<ApiResponse<FundAvailabilityResponse>>>()
        )).thenThrow(HttpClientErrorException(HttpStatus.BAD_REQUEST))

        // Act & Assert
        val exception = assertFailsWith<RuntimeException> {
            fakeApiService.checkFundAvailability(request)
        }
        assertTrue(exception.message!!.contains("Failed to check fund availability"))
    }

    @Test
    fun `test account not found`() {
        // Arrange
        val request = FundAvailabilityRequest(
            amount = BigDecimal("100.00"),
            accountId = "INVALID_ACC",
            description = "Test transaction"
        )

        val responseData = FundAvailabilityResponse(
            available = false,
            amount = request.amount,
            accountId = request.accountId!!,
            currentBalance = BigDecimal.ZERO
        )

        val apiResponse = ApiResponse(
            success = true,
            message = "Account not found",
            data = responseData,
            timestamp = LocalDateTime.now(),
            transactionId = "DEB-${System.currentTimeMillis()}-1234"
        )

        val responseEntity = ResponseEntity.ok(apiResponse)

        `when`(restTemplate.exchange(
            eq("http://localhost:9090/api/check-funds"),
            eq(HttpMethod.POST),
            any(),
            any<ParameterizedTypeReference<ApiResponse<FundAvailabilityResponse>>>()
        )).thenReturn(responseEntity)

        // Act
        val result = fakeApiService.checkFundAvailability(request)

        // Assert
        assertEquals(true, result.success)
        assertEquals("Account not found", result.message)
        assertNotNull(result.data)
        assertEquals(false, result.data?.available)
        assertEquals(BigDecimal("100.00"), result.data?.amount)
        assertEquals("INVALID_ACC", result.data?.accountId)
        assertEquals(BigDecimal.ZERO, result.data?.currentBalance)
    }

    @Test
    fun `test insufficient funds`() {
        // Arrange
        val request = FundAvailabilityRequest(
            amount = BigDecimal("500.00"),
            accountId = "ACC123",
            description = "Test transaction"
        )

        val responseData = FundAvailabilityResponse(
            available = false,
            amount = request.amount,
            accountId = request.accountId!!,
            currentBalance = BigDecimal("100.00")
        )

        val apiResponse = ApiResponse(
            success = true,
            message = "Insufficient funds",
            data = responseData,
            timestamp = LocalDateTime.now(),
            transactionId = "DEB-${System.currentTimeMillis()}-1234"
        )

        val responseEntity = ResponseEntity.ok(apiResponse)

        `when`(restTemplate.exchange(
            eq("http://localhost:9090/api/check-funds"),
            eq(HttpMethod.POST),
            any(),
            any<ParameterizedTypeReference<ApiResponse<FundAvailabilityResponse>>>()
        )).thenReturn(responseEntity)

        // Act
        val result = fakeApiService.checkFundAvailability(request)

        // Assert
        assertEquals(true, result.success)
        assertEquals("Insufficient funds", result.message)
        assertNotNull(result.data)
        assertEquals(false, result.data?.available)
        assertEquals(BigDecimal("500.00"), result.data?.amount)
        assertEquals("ACC123", result.data?.accountId)
        assertEquals(BigDecimal("100.00"), result.data?.currentBalance)
    }

    @Test
    fun `test connection error`() {
        // Arrange
        val request = FundAvailabilityRequest(
            amount = BigDecimal("100.00"),
            accountId = "ACC123",
            description = "Test transaction"
        )

        `when`(restTemplate.exchange(
            eq("http://localhost:9090/api/check-funds"),
            eq(HttpMethod.POST),
            any(),
            any<ParameterizedTypeReference<ApiResponse<FundAvailabilityResponse>>>()
        )).thenThrow(RuntimeException("Connection refused"))

        // Act & Assert
        val exception = assertFailsWith<RuntimeException> {
            fakeApiService.checkFundAvailability(request)
        }
        assertTrue(exception.message!!.contains("Failed to check fund availability"))
    }
}

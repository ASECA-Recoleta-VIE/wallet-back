package com.walletapi.config

import com.walletapi.dto.request.FundAvailabilityRequest
import com.walletapi.dto.response.ApiResponse
import com.walletapi.dto.response.FundAvailabilityResponse
import com.walletapi.services.FakeApiService
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Primary
import org.springframework.context.annotation.Profile
import org.springframework.web.client.RestTemplate
import java.math.BigDecimal
import java.time.LocalDateTime

@Configuration
@Profile("test")
class TestConfig {

    @Bean
    @Primary
    fun testFakeApiService(): TestFakeApiService {
        return TestFakeApiService()
    }

    /**
     * Test implementation of FakeApiService that returns configurable responses
     */
    class TestFakeApiService : FakeApiService(RestTemplate()) {

        // Configurable properties
        var fundsAvailable: Boolean = true
        var responseSuccess: Boolean = true
        var responseMessage: String = "Funds are available"
        var accountId: String = "FAKE-001"
        var currentBalance: BigDecimal = BigDecimal("0")

        /**
         * Configure the service to return a response indicating funds are available
         */
        fun configureFundsAvailable() {
            fundsAvailable = true
            responseSuccess = true
            responseMessage = "Funds are available"
        }

        /**
         * Configure the service to return a response indicating funds are not available
         */
        fun configureFundsNotAvailable() {
            fundsAvailable = false
            responseSuccess = true
            responseMessage = "Funds are not available"
        }

        // Override the checkFundAvailability method to return a configurable response
        override fun checkFundAvailability(request: FundAvailabilityRequest): ApiResponse<FundAvailabilityResponse> {
            return ApiResponse(
                success = responseSuccess,
                message = responseMessage,
                data = FundAvailabilityResponse(
                    available = fundsAvailable,
                    amount = request.amount,
                    accountId = accountId,
                    currentBalance = currentBalance
                ),
                timestamp = LocalDateTime.now(),
                transactionId = "DEB-1749007449025-8451"
            )
        }
    }
}

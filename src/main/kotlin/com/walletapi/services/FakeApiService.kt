package com.walletapi.services

import com.walletapi.dto.request.FundAvailabilityRequest
import com.walletapi.dto.response.ApiResponse
import com.walletapi.dto.response.FundAvailabilityResponse
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import org.springframework.web.client.RestTemplate
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.core.ParameterizedTypeReference
import org.springframework.http.HttpMethod

@Service
class FakeApiService(private val restTemplate: RestTemplate) {

    private val logger = LoggerFactory.getLogger(FakeApiService::class.java)

    @Value("\${fake.api.url:http://localhost:9090}")
    private lateinit var fakeApiBaseUrl: String

    fun checkFundAvailability(request: FundAvailabilityRequest): ApiResponse<FundAvailabilityResponse> {
        logger.info("Checking fund availability for amount: ${request.amount}")

        val url = "$fakeApiBaseUrl/api/check-funds"

        val headers = HttpHeaders()
        headers.contentType = MediaType.APPLICATION_JSON

        val httpEntity = HttpEntity(request, headers)

        val responseType = object : ParameterizedTypeReference<ApiResponse<FundAvailabilityResponse>>() {}

        try {
            val response = restTemplate.exchange(
                url,
                HttpMethod.POST,
                httpEntity,
                responseType
            )

            logger.info("Fund availability check response: ${response.body}")
            return response.body ?: throw RuntimeException("Empty response from fake API")
        } catch (e: Exception) {
            logger.error("Error checking fund availability: ${e.message}", e)
            throw RuntimeException("Failed to check fund availability: ${e.message}", e)
        }
    }
}

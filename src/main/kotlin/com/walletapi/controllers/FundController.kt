package com.walletapi.controllers

import com.walletapi.dto.request.EmailTransactionRequest
import com.walletapi.dto.request.FundAvailabilityRequest
import com.walletapi.dto.response.WalletResponse
import com.walletapi.entities.UserEntity
import com.walletapi.services.FakeApiService
import com.walletapi.services.WalletService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.responses.ApiResponse as SwaggerApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import jakarta.servlet.http.HttpServletRequest
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController

@RestController
class FundController(
    private val fakeApiService: FakeApiService,
    private val walletService: WalletService
) {
    private val logger = LoggerFactory.getLogger(FundController::class.java)

    @Operation(summary = "Request funds from fake API and deposit to user's wallet if available")
    @ApiResponses(value = [
        SwaggerApiResponse(responseCode = "200", description = "Funds requested and deposited successfully"),
        SwaggerApiResponse(responseCode = "400", description = "Invalid request"),
        SwaggerApiResponse(responseCode = "404", description = "User not found"),
        SwaggerApiResponse(responseCode = "500", description = "Error processing request or communicating with fake API")
    ])
    @PostMapping("/request-funds")
    fun requestFunds(
        @RequestBody request: FundAvailabilityRequest,
        httpRequest: HttpServletRequest
    ): ResponseEntity<WalletResponse> {
        val user: UserEntity? = httpRequest.getAttribute("user") as? UserEntity
        if (user == null) {
            logger.error("User not found in request")
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null)
        }

        try {
            logger.info("Checking fund availability for amount: ${request.amount}")
            val apiResponse = fakeApiService.checkFundAvailability(request)

            // Check the message from the API response
            when (apiResponse.message) {
                "Account not found" -> {
                    logger.warn("Account not found")
                    return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null)
                }
                "Insufficient funds", "Funds are not available" -> {
                    logger.warn("Insufficient funds: ${apiResponse.message}")
                    return ResponseEntity.status(HttpStatus.FORBIDDEN).body(null)
                }
                "Account ID is required" -> {
                    logger.warn("Account ID is required")
                    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(null)
                }
                else -> {
                    // If the message is not one of the above, check if funds are available
                    if (apiResponse.success && apiResponse.data?.available == true) {
                        logger.info("Funds are available, depositing to user's wallet")

                        val depositRequest = EmailTransactionRequest(
                            amount = request.amount.toDouble(),
                            description = request.description ?: "Deposit from fund request"
                        )

                        val walletResponse = walletService.requestFunds(user, depositRequest)

                        logger.info("Fund request successful, new balance: ${walletResponse.balance}")
                        return ResponseEntity.ok(walletResponse)
                    } else {
                        logger.warn("Funds not available or API response not successful")
                        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(null)
                    }
                }
            }
        } catch (e: Exception) {
            logger.error("Error processing fund request: ${e.message}", e)
            return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(null)
        }
    }

}

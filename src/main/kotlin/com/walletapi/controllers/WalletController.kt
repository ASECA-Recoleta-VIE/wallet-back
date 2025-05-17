package com.walletapi.controllers

import com.walletapi.services.WalletService
import com.walletapi.models.History
import com.walletapi.models.Wallet
import com.walletapi.dto.request.EmailTransactionRequest
import com.walletapi.dto.request.TransferRequest
import com.walletapi.dto.response.HistoryResponse
import com.walletapi.dto.response.TransferResponse
import com.walletapi.dto.response.WalletResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
class WalletController(private val walletService: WalletService) {

    @Operation(summary = "Deposit funds to a user's wallet")
    @ApiResponses(value = [
        ApiResponse(responseCode = "200", description = "Deposit successful"),
        ApiResponse(responseCode = "400", description = "Invalid request or amount"),
        ApiResponse(responseCode = "404", description = "User wallet not found")
    ])
    @PostMapping("/deposit")
    fun deposit(
        @RequestBody request: EmailTransactionRequest
    ): ResponseEntity<WalletResponse> {
        val walletResponse = walletService.deposit(request)
        return ResponseEntity(walletResponse, HttpStatus.OK)
    }

    @Operation(summary = "Withdraw funds from a user's wallet")
    @ApiResponses(value = [
        ApiResponse(responseCode = "200", description = "Withdrawal successful"),
        ApiResponse(responseCode = "400", description = "Invalid request or insufficient funds"),
        ApiResponse(responseCode = "404", description = "User wallet not found")
    ])
    @PostMapping("/withdraw")
    fun withdraw(
        @RequestBody request: EmailTransactionRequest
    ): ResponseEntity<WalletResponse> {
        val walletResponse = walletService.withdraw(request.email, request.amount, request.description)
        return ResponseEntity(walletResponse, HttpStatus.OK)
    }

    @Operation(summary = "Transfer funds between users' wallets")
    @ApiResponses(value = [
        ApiResponse(responseCode = "200", description = "Transfer successful"),
        ApiResponse(responseCode = "400", description = "Invalid request or insufficient funds"),
        ApiResponse(responseCode = "404", description = "One or both user wallets not found")
    ])
    @PostMapping("/transfer")
    fun transfer(
        @RequestBody request: TransferRequest
    ): ResponseEntity<TransferResponse> {
        val transferResponse = walletService.transfer(
            request.fromEmail,
            request.toEmail,
            request.amount,
            request.description
        )
        return ResponseEntity(transferResponse, HttpStatus.OK)
    }

    @Operation(summary = "Get transaction history for a user's wallet")
    @ApiResponses(value = [
        ApiResponse(responseCode = "200", description = "History retrieved successfully"),
        ApiResponse(responseCode = "404", description = "User wallet not found")
    ])
    @GetMapping("/history")
    fun getHistory(@RequestParam email: String): ResponseEntity<List<HistoryResponse>> {
        val history = walletService.getHistory(email)

        val historyResponses = history.map { h ->
            HistoryResponse(
                id = "${h.date}-${h.type}", // Generate a pseudo-ID from date and type
                amount = h.amount,
                timestamp = h.date.toString(),
                description = h.description,
                toWalletId = null // This information is not available in the History model
            )
        }

        return ResponseEntity(historyResponses, HttpStatus.OK)
    }
}

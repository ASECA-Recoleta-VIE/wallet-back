package com.walletapi.controllers

import com.walletapi.services.WalletService
import com.walletapi.dto.request.EmailTransactionRequest
import com.walletapi.dto.request.TransferRequest
import com.walletapi.dto.response.HistoryResponse
import com.walletapi.dto.response.TransferResponse
import com.walletapi.dto.response.WalletResponse
import com.walletapi.entities.UserEntity
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import jakarta.servlet.http.HttpServletRequest

import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
class WalletController(private val walletService: WalletService) {

    @Operation(summary = "Deposit funds to a user's wallet")
    @ApiResponses(value = [
        ApiResponse(responseCode = "200", description = "Deposit successful"),
        ApiResponse(responseCode = "403", description = "Invalid request or amount"),
        ApiResponse(responseCode = "404", description = "User wallet not found")
    ])
    @PostMapping("/deposit")
    fun deposit(
        @RequestBody body: EmailTransactionRequest,
        request: HttpServletRequest
    ): ResponseEntity<WalletResponse> {
        val user: UserEntity? = request.getAttribute("user") as? UserEntity
        if (user == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null)
        }
        val walletResponse = walletService.deposit(user,body)
        return ResponseEntity(walletResponse, HttpStatus.OK)
    }

    @Operation(summary = "Withdraw funds from a user's wallet")
    @ApiResponses(value = [
        ApiResponse(responseCode = "200", description = "Withdrawal successful"),
        ApiResponse(responseCode = "403", description = "Invalid request or insufficient funds"),
        ApiResponse(responseCode = "404", description = "User wallet not found")
    ])
    @PostMapping("/withdraw")
    fun withdraw(
        @RequestBody body: EmailTransactionRequest,
        request: HttpServletRequest
    ): ResponseEntity<WalletResponse> {
        val user: UserEntity? = request.getAttribute("user") as? UserEntity
        if (user == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null)
        }

        val walletResponse = walletService.withdraw(user, body.amount, body.description)
        return ResponseEntity(walletResponse, HttpStatus.OK)
    }

    @Operation(summary = "Transfer funds between users' wallets")
    @ApiResponses(value = [
        ApiResponse(responseCode = "200", description = "Transfer successful"),
        ApiResponse(responseCode = "400", description = "Invalid request"),
        ApiResponse(responseCode = "403", description = "Insufficient funds for transfer"),
        ApiResponse(responseCode = "404", description = "One or both user wallets not found"),
        ApiResponse(responseCode = "405", description = "Transfer not allowed between same user wallets")
    ])
    @PostMapping("/transfer")
    fun transfer(
        @RequestBody body: TransferRequest,  request: HttpServletRequest
    ): ResponseEntity<TransferResponse> {
        val user: UserEntity? = request.getAttribute("user") as? UserEntity
        if (user == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null)
        }
        val transferResponse = walletService.transfer(
            user,
            body.toEmail,
            body.amount,
            body.description
        )
        return ResponseEntity(transferResponse, HttpStatus.OK)
    }

    @Operation(summary = "Get transaction history for a user's wallet")
    @ApiResponses(value = [
        ApiResponse(responseCode = "200", description = "History retrieved successfully"),
        ApiResponse(responseCode = "404", description = "User wallet not found")
    ])
    @GetMapping("/history")
    fun getHistory(
        request: HttpServletRequest
    ): ResponseEntity<List<HistoryResponse>> {
        val user: UserEntity? = request.getAttribute("user") as? UserEntity
        if (user == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(emptyList())
        }
        val history = walletService.getHistory(user)

        val historyResponses = history.map { h ->
            HistoryResponse(
                id = h.id!!,
                amount = h.amount!!,
                timestamp = h.date.toString(),
                description = h.description,
                toWalletId = h.wallet?.id
            )
        }

        return ResponseEntity(historyResponses, HttpStatus.OK)
    }
    @Operation(summary = "Get wallet details for a user")
    @ApiResponses(value = [
        ApiResponse(responseCode = "200", description = "Wallet details retrieved successfully"),
        ApiResponse(responseCode = "404", description = "User wallet not found")
    ])
    @GetMapping("/wallet")
    fun getWallet(
        request: HttpServletRequest
    ): ResponseEntity<WalletResponse> {
        val user: UserEntity? = request.getAttribute("user") as? UserEntity
        if (user == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null)
        }
        val wallet = walletService.getWallet(user)

        return ResponseEntity(
            WalletResponse(
                id = wallet.id,
                balance = wallet.balance,
                currency = wallet.currency
            ),
            HttpStatus.OK
        )
    }
}

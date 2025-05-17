package com.walletapi.services

import com.walletapi.dto.request.EmailTransactionRequest
import com.walletapi.dto.response.TransferResponse
import com.walletapi.dto.response.WalletResponse
import com.walletapi.models.History
import com.walletapi.models.TransactionType
import com.walletapi.models.User
import com.walletapi.models.Wallet
import com.walletapi.repositories.HistoryRepository
import com.walletapi.repositories.WalletRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate

@Service
class WalletService(
    private val walletRepository: WalletRepository,
    private val historyRepository: HistoryRepository
) {

    @Transactional
    fun deposit(depositReqInfo: EmailTransactionRequest): WalletResponse {
        // Find wallet by user email
        val wallet = findWalletByUserEmail(depositReqInfo.email)
            ?: throw IllegalArgumentException("No wallet found for user with email: ${depositReqInfo.email}")

        // Use wallet's own method for deposit
        val updatedWalletResult = wallet.deposit(
            amount = depositReqInfo.amount,
            reason = depositReqInfo.description ?: "None"
        )

        if (updatedWalletResult.isFailure) {
            throw updatedWalletResult.exceptionOrNull() ?: IllegalStateException("Unknown error during deposit")
        }

        val updatedWallet = updatedWalletResult.getOrNull()!!

        val savedWallet = walletRepository.save(updatedWallet)

        return WalletResponse(
            id = savedWallet.getName(), // Using name as ID since Wallet doesn't have an ID property
            balance = savedWallet.getBalance(),
            currency = "USD" // Assuming USD as default currency
        )
    }

    @Transactional
    fun withdraw(userEmail: String, amount: Double, description: String? = null): WalletResponse {
        val wallet = findWalletByUserEmail(userEmail)
            ?: throw IllegalArgumentException("No wallet found for user with email: $userEmail")

        // Use wallet's own method for withdraw
        val updatedWalletResult = wallet.withdraw(amount)

        if (updatedWalletResult.isFailure) {
            throw updatedWalletResult.exceptionOrNull() ?: IllegalStateException("Unknown error during withdrawal")
        }

        val updatedWallet = updatedWalletResult.getOrNull()!!

        val savedWallet = walletRepository.save(updatedWallet)

        return WalletResponse(
            id = savedWallet.getName(), // Using name as ID since Wallet doesn't have an ID property
            balance = savedWallet.getBalance(),
            currency = "USD" // Assuming USD as default currency
        )
    }

    @Transactional
    fun transfer(fromUserEmail: String, toUserEmail: String, amount: Double, description: String? = null): TransferResponse {
        val fromWallet = findWalletByUserEmail(fromUserEmail)
            ?: throw IllegalArgumentException("No wallet found for user with email: $fromUserEmail")

        val toWallet = findWalletByUserEmail(toUserEmail)
            ?: throw IllegalArgumentException("No wallet found for user with email: $toUserEmail")

        // For simplicity, assuming we have a way to get User objects from emails
        val fromUser = User(email = fromUserEmail, fullName = fromUserEmail, password = "") // This is a placeholder
        val toUser = User(email = toUserEmail, fullName = toUserEmail, password = "") // This is a placeholder

        // Use wallet's own method for transfer
        val transferResult = fromWallet.transfer(toWallet, amount, fromUser, toUser)

        if (transferResult.isFailure) {
            throw transferResult.exceptionOrNull() ?: IllegalStateException("Unknown error during transfer")
        }

        val (updatedFromWallet, updatedToWallet) = transferResult.getOrNull()!!

        val savedFromWallet = walletRepository.save(updatedFromWallet)
        val savedToWallet = walletRepository.save(updatedToWallet)

        val fromWalletResponse = WalletResponse(
            id = savedFromWallet.getName(),
            balance = savedFromWallet.getBalance(),
            currency = "USD"
        )

        val toWalletResponse = WalletResponse(
            id = savedToWallet.getName(),
            balance = savedToWallet.getBalance(),
            currency = "USD"
        )

        return TransferResponse(
            fromWallet = fromWalletResponse,
            toWallet = toWalletResponse
        )
    }

    fun getHistory(userEmail: String): List<History> {
        val wallet = findWalletByUserEmail(userEmail)
            ?: throw IllegalArgumentException("No wallet found for user with email: $userEmail")

        return wallet.getHistory()
    }

    // Helper methods
    private fun findWalletByUserEmail(email: String): Wallet? {
        return walletRepository.findByUserEmail(email)
    }
}

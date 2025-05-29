package com.walletapi.services

import com.walletapi.dto.request.EmailTransactionRequest
import com.walletapi.dto.response.TransferResponse
import com.walletapi.dto.response.WalletResponse
import com.walletapi.entities.HistoryEntity
import com.walletapi.entities.WalletEntity
import com.walletapi.entities.walletToEntity
import com.walletapi.models.History
import com.walletapi.models.TransactionType
import com.walletapi.models.User
import com.walletapi.models.Wallet
import com.walletapi.repositories.HistoryRepository
import com.walletapi.repositories.UserRepository
import com.walletapi.repositories.WalletRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate

@Service
class WalletService(
    private val walletRepository: WalletRepository,
    private val historyRepository: HistoryRepository,
    private val userRepository: UserRepository // Add this if not already present
) {

    @Transactional
    fun deposit(depositReqInfo: EmailTransactionRequest): WalletResponse {
        val userEntity = userRepository.findByEmail(depositReqInfo.email) ?:
        throw IllegalArgumentException("User not found: ${depositReqInfo.email}")
        val walletEntity = walletRepository.findByUser(userEntity).firstOrNull()
            ?: throw IllegalArgumentException("No wallet found for user with email: ${depositReqInfo.email}")
        
        val wallet = walletEntity.toWallet()
        
        val updatedWalletResult = wallet.deposit(
            amount = depositReqInfo.amount,
            reason = depositReqInfo.description ?: "None"
        )
        

        if (updatedWalletResult.isFailure) {
            throw updatedWalletResult.exceptionOrNull() ?: IllegalStateException("Unknown error during deposit")
        }

        val updatedWallet = updatedWalletResult.getOrNull()!!
        val histories = updatedWallet.getHistory()
        val historyEntities = ArrayList<HistoryEntity>()
        histories.forEach { history ->
            val entity = HistoryEntity(
                date = history.date,
                description = history.description,
                type = history.type,
                amount = history.amount,
                balance = updatedWallet.getBalance(),
                wallet = walletEntity
            )
            val historyEntity = historyRepository.save(entity)
            historyEntities.add(historyEntity)
        }
        walletEntity.history = historyEntities
        walletEntity.balance = updatedWallet.getBalance()
        val updatedWalletEntity = walletRepository.save(walletEntity)


        return WalletResponse(
            id = walletEntity.name!!,
            balance = walletEntity.balance!!,
            currency = "USD"
        )
    }

    @Transactional
    fun withdraw(userEmail: String, amount: Double, description: String? = null): WalletResponse {
        val userEntity = userRepository.findByEmail(userEmail) ?: throw IllegalArgumentException("User not found: $userEmail")
        // Find wallet entity by user email
        val walletEntity = walletRepository.findByUser(userEntity).firstOrNull()
            ?: throw IllegalArgumentException("No wallet found for user with email: $userEmail")
        
        // Convert entity to domain model
        val wallet = walletEntity.toWallet()
        
        // Use wallet's own method for withdraw
        val updatedWalletResult = wallet.withdraw(amount)

        if (updatedWalletResult.isFailure) {
            throw updatedWalletResult.exceptionOrNull() ?: IllegalStateException("Unknown error during withdrawal")
        }

        val updatedWallet = updatedWalletResult.getOrNull()!!
        
        val updatedEntity = walletToEntity(updatedWallet, userEntity)
        val savedEntity = walletRepository.save(updatedEntity)

        return WalletResponse(
            id = savedEntity.name!!,
            balance = savedEntity.balance!!,
            currency = "USD"
        )
    }

    @Transactional
    fun transfer(fromUserEmail: String, toUserEmail: String, amount: Double, description: String? = null): TransferResponse {
        val userFromEntity = userRepository.findByEmail(fromUserEmail) ?: throw IllegalArgumentException("User not found: $fromUserEmail")
        val userToEntity = userRepository.findByEmail(toUserEmail) ?: throw IllegalArgumentException("User not found: $toUserEmail")
        val fromWalletEntity = walletRepository.findByUser(userFromEntity)  .firstOrNull()
            ?: throw IllegalArgumentException("No wallet found for user with email: $fromUserEmail")
        
        val toWalletEntity = walletRepository.findByUser(userToEntity) .firstOrNull()
            ?: throw IllegalArgumentException("No wallet found for user with email: $toUserEmail")
        
        // Convert entities to domain models
        val fromWallet = fromWalletEntity.toWallet()
        val toWallet = toWalletEntity.toWallet()
        
        // Get user entities
        val fromUserEntity = userRepository.findByEmail(fromUserEmail)
            ?: throw IllegalArgumentException("User not found: $fromUserEmail")
        
        val toUserEntity = userRepository.findByEmail(toUserEmail)
            ?: throw IllegalArgumentException("User not found: $toUserEmail")
        
        // Convert to domain users for the transfer
        val fromUser = fromUserEntity.toUser()
        val toUser = toUserEntity.toUser()

        // Use wallet's own method for transfer
        val transferResult = fromWallet.transfer(toWallet, amount, fromUser, toUser)

        if (transferResult.isFailure) {
            throw transferResult.exceptionOrNull() ?: IllegalStateException("Unknown error during transfer")
        }

        val (updatedFromWallet, updatedToWallet) = transferResult.getOrNull()!!
        val lastFromHistory = updatedFromWallet.getHistory().last()
        val lastToHistory = updatedToWallet.getHistory().last()
        
        // Convert back to entities and save
        fromWalletEntity.balance = updatedFromWallet.getBalance()
        toWalletEntity.balance = updatedToWallet.getBalance()
        fromWalletEntity.history = fromWalletEntity.history.plus(
            HistoryEntity(
                date = lastFromHistory.date,
                description = lastFromHistory.description,
                type = lastFromHistory.type,
                amount = lastFromHistory.amount,
                balance = updatedFromWallet.getBalance(),
                wallet = fromWalletEntity
            )
        ).toMutableList()
        historyRepository.save(fromWalletEntity.history.last())
        toWalletEntity.history = toWalletEntity.history.plus(
            HistoryEntity(
                date = lastToHistory.date,
                description = lastToHistory.description,
                type = lastToHistory.type,
                amount = lastToHistory.amount,
                balance = updatedToWallet.getBalance(),
                wallet = toWalletEntity
            )
        ).toMutableList()
        historyRepository.save(toWalletEntity.history.last())
        val savedFromEntity = walletRepository.save(fromWalletEntity)
        val savedToEntity = walletRepository.save(toWalletEntity)

        val fromWalletResponse = WalletResponse(
            id = savedFromEntity.name!!,
            balance = savedFromEntity.balance!!,
            currency = "ARS"
        )

        val toWalletResponse = WalletResponse(
            id = savedToEntity.name!!,
            balance = savedToEntity.balance!!,
            currency = "ARS"
        )

        return TransferResponse(
            fromWallet = fromWalletResponse,
            toWallet = toWalletResponse
        )
    }

    fun getHistory(userEmail: String): List<History> {
        val userEntity = userRepository.findByEmail(userEmail) ?: throw IllegalArgumentException("User not found: $userEmail")
        val walletEntity = walletRepository.findByUser(userEntity).firstOrNull()
            ?: throw IllegalArgumentException("No wallet found for user with email: $userEmail")
        
        val wallet = walletEntity.toWallet()
        return wallet.getHistory()
    }

    // This helper method should be removed or properly implemented
    private fun findWalletByUserEmail(email: String): Wallet? {
        // This method is incorrect - it needs to convert entities to domain objects
        val userEntity = userRepository.findByEmail(email)
        if (userEntity == null) {
            return null
        }
        val walletEntity = walletRepository.findByUser(userEntity).firstOrNull()
        return walletEntity!!.toWallet()
    }
}
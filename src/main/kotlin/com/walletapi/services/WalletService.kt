package com.walletapi.services

import com.walletapi.dto.request.EmailTransactionRequest
import com.walletapi.dto.response.HistoryResponse
import com.walletapi.dto.response.TransferResponse
import com.walletapi.dto.response.WalletResponse
import com.walletapi.entities.HistoryEntity
import com.walletapi.entities.UserEntity
import com.walletapi.entities.WalletEntity
import com.walletapi.exceptions.InsufficientFundsException
import com.walletapi.exceptions.InvalidAmountException
import com.walletapi.exceptions.SelfTransferException
import com.walletapi.exceptions.TransactionException
import com.walletapi.exceptions.UserNotFoundException
import com.walletapi.exceptions.WalletException
import com.walletapi.exceptions.WalletNotFoundException
import com.walletapi.models.History
import com.walletapi.models.Wallet
import com.walletapi.repositories.HistoryRepository
import com.walletapi.repositories.UserRepository
import com.walletapi.repositories.WalletRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate

@Service
@Transactional
class WalletService(
    private val walletRepository: WalletRepository,
    private val historyRepository: HistoryRepository,
    private val userRepository: UserRepository
) {
    private val defaultCurrency: String = "USD"
    /**
     * Finds a user by email or throws an exception if not found
     */
    private fun findUserByEmail(email: String): UserEntity {
        return userRepository.findByEmail(email)
            ?: throw UserNotFoundException(email)
    }

    /**
     * Finds a wallet by user or throws an exception if not found
     */
    private fun findWalletByUser(userEntity: UserEntity, email: String?): WalletEntity {
        return walletRepository.findByUser(userEntity).firstOrNull()
            ?: throw WalletNotFoundException(userEntity.email ?: email ?: "Unknown user")
    }

    /**
     * Handles a Result object, throwing an appropriate exception if it's a failure
     */
    private fun <T> handleResult(result: Result<T>, errorMessage: String): T {
        if (result.isFailure) {
            val exception = result.exceptionOrNull()
            when (exception) {
                is IllegalArgumentException -> {
                    if (exception.message?.contains("Amount must be positive") == true) {
                        throw InvalidAmountException(exception.message ?: "Amount must be positive", exception)
                    } else if (exception.message?.contains("Insufficient funds") == true) {
                        throw InsufficientFundsException(exception.message ?: "Insufficient funds", exception)
                    } else {
                        throw TransactionException(exception.message ?: errorMessage, exception)
                    }
                }
                else -> throw TransactionException(errorMessage, exception)
            }
        }
        return result.getOrNull()!!
    }

    /**
     * Creates a HistoryEntity from a History model and saves it
     */
    private fun createAndSaveHistoryEntity(
        history: History,
        walletEntity: WalletEntity,
        balance: Double
    ): HistoryEntity {
        val entity = HistoryEntity(
            date = history.date,
            description = history.description,
            type = history.type,
            amount = history.amount,
            balance = balance,
            wallet = walletEntity
        )
        return historyRepository.save(entity)
    }

    /**
     * Updates a wallet entity with new balance and history
     */
    private fun updateWalletEntity(
        walletEntity: WalletEntity,
        wallet: Wallet
    ): WalletEntity {
        // Update balance
        walletEntity.balance = wallet.getBalance()

        // Get the latest history record
        val latestHistory = wallet.getHistory().lastOrNull()

        // If there's a new history record, add it to the entity
        if (latestHistory != null) {
            val historyEntity = createAndSaveHistoryEntity(
                latestHistory,
                walletEntity,
                wallet.getBalance()
            )
            walletEntity.history = walletEntity.history.plus(historyEntity).toMutableList()
        }

        return walletRepository.save(walletEntity)
    }

    /**
     * Creates a WalletResponse from a WalletEntity
     */
    private fun createWalletResponse(walletEntity: WalletEntity, currency: String = defaultCurrency): WalletResponse {
        return WalletResponse(
            name = walletEntity.name ?: "",
            balance = walletEntity.balance ?: 0.0,
            currency = currency
        )
    }

    @Transactional
    fun deposit(user: UserEntity, depositReqInfo: EmailTransactionRequest): WalletResponse {
        try {
            // Find user and wallet
            val walletEntity = findWalletByUser(user, user.email)

            // Convert entity to domain model
            val wallet = walletEntity.toWallet()

            // Perform deposit operation
            val updatedWalletResult = wallet.deposit(
                amount = depositReqInfo.amount,
                reason = depositReqInfo.description ?: "None"
            )

            // Handle result and get updated wallet
            val updatedWallet = handleResult(
                updatedWalletResult, 
                "Unknown error during deposit"

            )

            // Update wallet entity with new balance and history
            val updatedWalletEntity = updateWalletEntity(walletEntity, updatedWallet)

            // Create and return response
            return createWalletResponse(updatedWalletEntity)
        } catch (e: WalletException) {
            // Re-throw WalletExceptions as they are already properly typed
            throw e
        } catch (e: Exception) {
            // Convert any other exceptions to TransactionException
            throw TransactionException("Error processing deposit: ${e.message}", e)


        }
    }

    @Transactional
    fun requestFunds(user: UserEntity, requestInfo: EmailTransactionRequest): WalletResponse {
        try {
            // Find user and wallet
            val walletEntity = findWalletByUser(user, user.email)

            // Convert entity to domain model
            val wallet = walletEntity.toWallet()

            // Perform debin operation
            val updatedWalletResult = wallet.debin(
                amount = requestInfo.amount,
                reason = requestInfo.description ?: "Fund request"
            )

            // Handle result and get updated wallet
            val updatedWallet = handleResult(
                updatedWalletResult, 
                "Unknown error during fund request"
            )

            // Update wallet entity with new balance and history
            val updatedWalletEntity = updateWalletEntity(walletEntity, updatedWallet)

            // Create and return response
            return createWalletResponse(updatedWalletEntity)
        } catch (e: WalletException) {
            // Re-throw WalletExceptions as they are already properly typed
            throw e
        } catch (e: Exception) {
            // Convert any other exceptions to TransactionException
            throw TransactionException("Error processing fund request: ${e.message}", e)
        }
    }

    @Transactional
    fun withdraw(user: UserEntity, amount: Double, description: String? = null): WalletResponse {
        try {
            // Find user and wallet
            val walletEntity = findWalletByUser(user, user.email)

            // Convert entity to domain model
            val wallet = walletEntity.toWallet()

            // Perform withdraw operation
            val updatedWalletResult = wallet.withdraw(amount, description ?: "Withdrawal")

            // Handle result and get updated wallet
            val updatedWallet = handleResult(
                updatedWalletResult, 
                "Unknown error during withdrawal"
            )

            // Update wallet entity with new balance and history
            val updatedWalletEntity = updateWalletEntity(walletEntity, updatedWallet)

            // Create and return response
            return createWalletResponse(updatedWalletEntity)
        } catch (e: WalletException) {
            // Re-throw WalletExceptions as they are already properly typed
            throw e
        } catch (e: Exception) {
            // Convert any other exceptions to TransactionException
            throw TransactionException("Error processing withdrawal: ${e.message}", e)
        }
    }

    @Transactional
    fun transfer(user: UserEntity, toUserEmail: String, amount: Double, description: String? = null): TransferResponse {
        try {
            // Find users and wallets

            // refetch user to dont have lazy loading issues
            val user = userRepository.findByEmail(user.email)
                ?: throw UserNotFoundException(user.email)

            val toUserEntity = findUserByEmail(toUserEmail)
            // Check if users are the same
            if (user.email == toUserEmail) {
                throw SelfTransferException(user.email)
            }
            val fromWalletEntity = findWalletByUser(user, user.email)
            val toWalletEntity = findWalletByUser(toUserEntity, toUserEmail)

            // Convert entities to domain models
            val fromWallet = fromWalletEntity.toWallet()
            val toWallet = toWalletEntity.toWallet()

            // Convert to domain users for the transfer
            val fromUser = user.toUser()
            val toUser = toUserEntity.toUser()

            // Perform transfer operation
            val transferResult = fromWallet.transfer(toWallet, amount, fromUser, toUser)

            // Handle result and get updated wallets
            val (updatedFromWallet, updatedToWallet) = handleResult(
                transferResult,
                "Unknown error during transfer"
            )



            // Update wallet entities with new balances and histories
            val savedFromEntity = updateWalletEntity(fromWalletEntity, updatedFromWallet)
            val savedToEntity = updateWalletEntity(toWalletEntity, updatedToWallet)

            // Create and return response
            return TransferResponse(
                fromWallet = createWalletResponse(savedFromEntity),
                toWallet = createWalletResponse(savedToEntity)
            )
        } catch (e: WalletException) {
            // Re-throw WalletExceptions as they are already properly typed
            throw e
        } catch (e: Exception) {
            // Convert any other exceptions to TransactionException
            throw TransactionException("Error processing transfer: ${e.message}", e)
        }
    }

    fun getHistory(user: UserEntity): List<HistoryResponse> {
        try {
            // Find user and wallet
            val walletEntity = findWalletByUser(user, user.email)

            // Convert entity to domain model and return history
            val wallet = walletEntity.toWallet()
            return historyRepository.findByWallet(walletEntity).map {
                HistoryResponse(
                    id = it.id!!,
                    amount = it.amount!!,
                    timestamp = it.date.toString()!!,
                    description = it.description ?: "No description",
                    type = it.type.name
                )
            }
        } catch (e: WalletException) {
            // Re-throw WalletExceptions as they are already properly typed
            throw e
        } catch (e: Exception) {
            // Convert any other exceptions to TransactionException
            throw TransactionException("Error retrieving transaction history: ${e.message}", e)
        }
    }

    fun getWallet(user: UserEntity): WalletResponse {
        try {
            // Find user and wallet
            val walletEntity = findWalletByUser(user, user.email)

            // Create and return response
            return createWalletResponse(walletEntity)
        } catch (e: WalletException) {
            // Re-throw WalletExceptions as they are already properly typed
            throw e
        } catch (e: Exception) {
            // Convert any other exceptions to TransactionException
            throw TransactionException("Error retrieving wallet info: ${e.message}", e)
        }
    }

}

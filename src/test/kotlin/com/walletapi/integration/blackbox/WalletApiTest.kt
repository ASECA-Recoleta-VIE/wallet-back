package com.walletapi.integration.blackbox

import com.fasterxml.jackson.databind.ObjectMapper
import com.walletapi.dto.response.HistoryResponse
import com.walletapi.dto.response.TransferResponse
import com.walletapi.dto.response.WalletResponse
import com.walletapi.helper.UserHelperService
import com.walletapi.models.TransactionType
import com.walletapi.repositories.HistoryRepository
import com.walletapi.repositories.UserRepository
import com.walletapi.repositories.WalletRepository
import jakarta.persistence.EntityManager
import jakarta.servlet.http.Cookie
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.transaction.annotation.Transactional
import org.springframework.transaction.support.TransactionTemplate

@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc
class WalletApiTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var userRepository: UserRepository

    @Autowired
    private lateinit var historyRepository: HistoryRepository

    @Autowired
    private lateinit var walletRepository: WalletRepository

    @Autowired
    private lateinit var entityManager: EntityManager

    @Autowired
    private lateinit var userHelperService: UserHelperService

    @Autowired
    private lateinit var transactionManager: PlatformTransactionManager
    private val firstEmail = "test@example.com"
    private val secondEmail = "test2@example.com"
    private val firstName = "Test User Number One"
    private val secondName = "Test User Number Two"
    private val password = "Password123!"
    private var firstUserCookie: Cookie? = null

    @BeforeEach
    fun setup() {
        val status = TransactionTemplate(transactionManager).execute { status ->
            // Clear the database before each test
            entityManager.createNativeQuery("DELETE FROM wallet_entity_history").executeUpdate()
            historyRepository.deleteAll()
            walletRepository.deleteAll()
            userRepository.deleteAll()
            null
        }

        // create a user
        mockMvc.perform(
            post("/api/users/register")
                .contentType("application/json")
                .content(
                    """
                {
                    "fullName": "$firstName",
                    "email": "$firstEmail",
                    "password": "$password"
                    }
                """.trimIndent()
                )
        )
            .andExpect { status().isOk() }
        // create a second user
        mockMvc.perform(
            post("/api/users/register")
                .contentType("application/json")
                .content(
                    """
                {
                    "fullName": "$secondName",
                    "email": "$secondEmail",
                    "password": "$password"
                    }
                """.trimIndent()
                )
        )
            .andExpect { status().isOk() }

        // login the first user and get the token
        val response = mockMvc.perform(
            post("/api/users/login")
                .contentType("application/json")
                .content(
                    """
                    {
                        "email": "$firstEmail",
                        "password": "$password"
                    }
                """.trimIndent()
                )
        )
            .andExpect { status().isOk() }
            .andReturn().response

        firstUserCookie = response.getCookie("token")

    }

    @Test
    fun userShouldHaveWallet() {
        // Check if the first user has a wallet
        val user = userHelperService.getUserWithWalletsAndHistory(firstEmail)
        assert(user != null) { "User should be registered in the database" }
        assert(user!!.wallets.isNotEmpty()) { "User should have at least one wallet" }
    }

    @Test
    fun walletEndpointShouldReturnWalletInformation() {
        // Check if the wallet endpoint returns the wallet information for the first user
        mockMvc.perform(
            get("/wallet")
                .cookie(firstUserCookie)
                .contentType("application/json")
        )
            .andExpect(status().isOk)
            .andExpect { result ->
                val content: WalletResponse =
                    ObjectMapper().readValue(result.response.contentAsString, WalletResponse::class.java)
                assert(content.name == "Main Wallet") { "Wallet name should be 'Main Wallet'" }
                assert(content.balance >= 0) { "Wallet balance should be non-negative" }
                assert(content.currency == "USD") { "Wallet currency should be 'USD'" }
                assert(content.balance == 0.0) { "Wallet balance should be 0.0" }
            }

        // persistence check
        val user = userHelperService.getUserWithWalletsAndHistory(firstEmail)
        assert(user != null) { "User should be registered in the database" }
        assert(user!!.wallets.isNotEmpty()) { "User should have at least one wallet" }
        val wallet = user.wallets.first()
        assert(wallet.name == "Main Wallet") { "Wallet name should be 'Main Wallet'" }
        assert(wallet.balance!! >= 0.0) { "Wallet balance should be non-negative" }
        assert(wallet.balance == 0.0) { "Wallet balance should be 0.0" }
    }

    /**
     * This test checks that a new user has an empty transaction history.
     * It assumes that the wallet service initializes a new user's history as empty.
     */
    @Test
    fun newUserShouldHaveEmptyListAsHistory() {
        // Check if the first user has an empty transaction history
        mockMvc.perform(
            get("/history")
                .cookie(firstUserCookie)
                .contentType("application/json")
        )
            .andExpect(status().isOk)
            .andExpect { result ->
                val content: List<HistoryResponse> = ObjectMapper().readValue(
                    result.response.contentAsString,
                    ObjectMapper().typeFactory.constructCollectionType(List::class.java, HistoryResponse::class.java)
                )
                assert(content.isEmpty()) { "Transaction history should be empty for a new user" }
            }

        // persistence check
        val user = userHelperService.getUserWithWalletsAndHistory(firstEmail)
        assert(user != null) { "User should be registered in the database" }
        val wallet = user!!.wallets.firstOrNull()
        assert(wallet != null) { "User should have at least one wallet" }
        val history = wallet!!.history
        assert(history.isEmpty()) { "Transaction history should be empty for a new user" }
    }

    /**
     * This test checks that a user can add money to their wallet.
     * It assumes that the wallet service allows deposits and updates the balance accordingly.
     */
    @Test
    fun userShouldBeAbleToAddMoneyToWallet() {
        // Add money to the wallet
        mockMvc.perform(
            post("/deposit")
                .cookie(firstUserCookie)
                .contentType("application/json")
                .content(
                    """
                    {
                        "amount": 1000,
                        "description": "Monthly salary"
                    }
                """.trimIndent()
                )
        )
            .andExpect(status().isOk)
            .andExpect { result ->
                val content: WalletResponse = ObjectMapper().readValue(
                    result.response.contentAsString,
                    WalletResponse::class.java
                )
                assert(content.balance == 1000.0) { "Wallet balance should be 1000.0 after adding money" }
            }


        // persistence check
        val user = userHelperService.getUserWithWalletsAndHistory(firstEmail)
        assert(user != null) { "User should be registered in the database" }
        val wallet = user!!.wallets.firstOrNull()
        assert(wallet != null) { "User should have at least one wallet" }
        assert(wallet!!.balance == 1000.0) { "Wallet balance should be 1000.0 after adding money" }
    }

    /**
     * This test checks that a user can add money to their wallet without authentication.
     * It assumes that the wallet service allows deposits without authentication.
     */
    @Test
    fun userShouldBeAbleToAddMoneyToWalletWithoutAuthentication() {
        // Add money to the wallet without authentication (no cookie)
        mockMvc.perform(
            post("/deposit")
                .contentType("application/json")
                .content(
                    """
                    {
                        "amount": 1000,
                        "description": "Monthly salary without authentication"
                    }
                """.trimIndent()
                )
        )
            .andExpect(status().isOk)
            .andExpect { result ->
                val content: WalletResponse = ObjectMapper().readValue(
                    result.response.contentAsString,
                    WalletResponse::class.java
                )
                assert(content.balance == 1000.0) { "Wallet balance should be 1000.0 after adding money without authentication" }
            }

        // persistence check
        val user = userHelperService.getUserWithWalletsAndHistory(firstEmail)
        assert(user != null) { "User should be registered in the database" }
        val wallet = user!!.wallets.firstOrNull()
        assert(wallet != null) { "User should have at least one wallet" }
        assert(wallet!!.balance == 1000.0) { "Wallet balance should be 1000.0 after adding money without authentication" }
    }

    /**
     * This test checks that a user cannot add a negative amount to their wallet.
     * It assumes that the wallet service validates the amount before processing the deposit.
     */
    @Test
    fun userShouldNotBeAbleToAddNegativeAmountToWallet() {
        // Try to add a negative amount to the wallet
        mockMvc.perform(
            post("/deposit")
                .cookie(firstUserCookie)
                .contentType("application/json")
                .content(
                    """
                    {
                        "amount": -100,
                        "description": "Invalid deposit"
                    }
                """.trimIndent()
                )
        )
            .andExpect(status().isForbidden)
            .andExpect { status().reason("Amount must be positive") }

        // persistence check
        val user = userHelperService.getUserWithWalletsAndHistory(firstEmail)
        assert(user != null) { "User should be registered in the database" }
        val wallet = user!!.wallets.firstOrNull()
        assert(wallet != null) { "User should have at least one wallet" }
        assert(wallet!!.balance == 0.0) { "Wallet balance should remain unchanged at 0.0" }
    }

    /**
     * This test checks that a user can withdraw money from their wallet.
     * It assumes that the wallet service allows withdrawals and updates the balance accordingly.
     */
    @Test
    fun userShouldBeAbleToWithdrawMoneyFromWallet() {
        // First deposit money to the wallet
        mockMvc.perform(
            post("/deposit")
                .cookie(firstUserCookie)
                .contentType("application/json")
                .content(
                    """
                    {
                        "amount": 1000,
                        "description": "Initial deposit"
                    }
                """.trimIndent()
                )
        )
            .andExpect(status().isOk)

        // Withdraw money from the wallet
        mockMvc.perform(
            post("/withdraw")
                .cookie(firstUserCookie)
                .contentType("application/json")
                .content(
                    """
                    {
                        "amount": 500,
                        "description": "Grocery shopping"
                    }
                """.trimIndent()
                )
        )
            .andExpect(status().isOk)
            .andExpect { result ->
                val content: WalletResponse = ObjectMapper().readValue(
                    result.response.contentAsString,
                    WalletResponse::class.java
                )
                assert(content.balance == 500.0) { "Wallet balance should be 500.0 after withdrawal" }
            }

        // persistence check
        val user = userHelperService.getUserWithWalletsAndHistory(firstEmail)
        assert(user != null) { "User should be registered in the database" }
        val wallet = user!!.wallets.firstOrNull()
        assert(wallet != null) { "User should have at least one wallet" }
        assert(wallet!!.balance == 500.0) { "Wallet balance should be 500.0 after withdrawal" }
    }

    /**
     * This test checks that a user cannot withdraw more than their wallet balance.
     * It assumes that the wallet service validates the balance before processing the withdrawal.
     */
    @Test
    fun userShouldNotBeAbleToWithdrawMoreThanBalance() {
        // Try to withdraw more than the wallet balance (which is initially 0)
        mockMvc.perform(
            post("/withdraw")
                .cookie(firstUserCookie)
                .contentType("application/json")
                .content(
                    """
                    {
                        "amount": 100,
                        "description": "Invalid withdrawal"
                    }
                """.trimIndent()
                )
        )
            .andExpect(status().isForbidden)
            .andExpect { status().reason("Insufficient funds") }

        // persistence check
        val user = userHelperService.getUserWithWalletsAndHistory(firstEmail)
        assert(user != null) { "User should be registered in the database" }
        val wallet = user!!.wallets.firstOrNull()
        assert(wallet != null) { "User should have at least one wallet" }
        assert(wallet!!.balance == 0.0) { "Wallet balance should remain unchanged at 0.0" }
    }


    /**
     * This test checks that the transaction history reflects deposits and withdrawals.
     * It assumes that the wallet service records transactions in the history.
     */
    @Test
    fun transactionHistoryShouldReflectDepositsAndWithdrawals() {
        // make deposits and withdrawals
        mockMvc.perform(
            post("/deposit")
                .cookie(firstUserCookie)
                .contentType("application/json")
                .content(
                    """
                    {
                        "amount": 1000,
                        "description": "Monthly salary"
                    }
                """.trimIndent()
                )
        )
            .andExpect(status().isOk)
        mockMvc.perform(
            post("/withdraw")
                .cookie(firstUserCookie)
                .contentType("application/json")
                .content(
                    """
                    {
                        "amount": 500,
                        "description": "Grocery shopping"
                    }
                """.trimIndent()
                )
        )
            .andExpect(status().isOk)
        // Check if the transaction history reflects the deposits and withdrawals
        mockMvc.perform(
            get("/history")
                .cookie(firstUserCookie)
                .contentType("application/json")
        )
            .andExpect(status().isOk)
            .andExpect { result ->
                val content: List<HistoryResponse> = ObjectMapper().readValue(
                    result.response.contentAsString,
                    ObjectMapper().typeFactory.constructCollectionType(List::class.java, HistoryResponse::class.java)
                )
                assert(content.size == 2) { "Transaction history should contain two entries" }
                assert(content[0].description == "Monthly salary") { "First transaction should be a deposit" }
                assert(content[1].description == "Grocery shopping") { "Second transaction should be a withdrawal" }
            }

        // persistence check
        val user = userHelperService.getUserWithWalletsAndHistory(firstEmail)
        assert(user != null) { "User should be registered in the database" }
        val wallet = user!!.wallets.firstOrNull()
        assert(wallet != null) { "User should have at least one wallet" }
        val history = wallet!!.history
        assert(history.size == 2) { "Transaction history should contain two entries" }
        assert(history[0].description == "Monthly salary") { "First transaction should be a deposit" }
        assert(history[1].description == "Grocery shopping") { "Second transaction should be a withdrawal" }
        assert(history[0].type == TransactionType.DEPOSIT) { "First transaction should be a deposit" }
        assert(history[1].type == TransactionType.WITHDRAWAL) { "Second transaction should be a withdrawal" }
        assert(history[0].amount == 1000.0) { "First transaction should be a deposit of 1000.0" }
        assert(history[1].amount == 500.0) { "Second transaction should be a withdrawal of 500.0" }
    }


    /**
     * This test checks that a user can transfer money to another user.
     * It assumes that the wallet service allows transfers and updates the balances accordingly.
     */
    @Test
    fun userShouldBeAbleToTransferMoneyToAnotherUser() {
        // First deposit money to the wallet
        mockMvc.perform(
            post("/deposit")
                .cookie(firstUserCookie)
                .contentType("application/json")
                .content(
                    """
                    {
                        "amount": 1000,
                        "description": "Initial deposit"
                    }
                """.trimIndent()
                )
        )
            .andExpect(status().isOk)

        // Transfer money to the second user
        mockMvc.perform(
            post("/transfer")
                .cookie(firstUserCookie)
                .contentType("application/json")
                .content(
                    """
                    {
                        "toEmail": "$secondEmail",
                        "amount": 200,
                        "description": "Gift"
                    }
                """.trimIndent()
                )
        )
            .andExpect(status().isOk)
            .andExpect { result ->
                val content: TransferResponse = ObjectMapper().readValue(result.response.contentAsString, TransferResponse::class.java)
                val fromWallet = content.fromWallet
                val toWallet = content.toWallet
                assert(fromWallet.balance == 800.0) { "From wallet balance should be 800.0 after transfer" }
                assert(toWallet.balance == 200.0) { "To wallet balance should be 200.0 after transfer" }
            }


        // persistence check
        val firstUser = userHelperService.getUserWithWalletsAndHistory(firstEmail)
        assert(firstUser != null) { "First user should be registered in the database" }
        val firstWallet = firstUser!!.wallets.firstOrNull()
        assert(firstWallet != null) { "First user should have at least one wallet" }
        assert(firstWallet!!.balance == 800.0) { "First user's wallet balance should be 800.0 after transfer" }

        val secondUser = userHelperService.getUserWithWalletsAndHistory(secondEmail)
        assert(secondUser != null) { "Second user should be registered in the database" }
        val secondWallet = secondUser!!.wallets.firstOrNull()
        assert(secondWallet != null) { "Second user should have at least one wallet" }
        assert(secondWallet!!.balance == 200.0) { "Second user's wallet balance should be 200.0 after transfer" }
    }

    /**
     * This test checks that a user cannot transfer money to themselves.
     * It assumes that the wallet service prevents self-transfers.
     */
    @Test
    fun userShouldNotBeAbleToTransferMoneyToSelf() {
        // Try to transfer money to the same user
        mockMvc.perform (
            post("/deposit")
                .cookie(firstUserCookie)
                .contentType("application/json")
                .content(
                    """
                    {
                        "amount": 1000,
                        "description": "Initial deposit"
                    }
                """.trimIndent()
                )
        );
        mockMvc.perform(
            post("/transfer")
                .cookie(firstUserCookie)
                .contentType("application/json")
                .content(
                    """
                    {
                        "toEmail": "$firstEmail",
                        "amount": 100,
                        "description": "Invalid transfer"
                    }
                """.trimIndent()
                )
        )
            .andExpect(status().isMethodNotAllowed)
            .andExpect { status().reason("User cannot transfer money to themselves: $firstEmail") }
        // persistence check
        val firstUser = userHelperService.getUserWithWalletsAndHistory(firstEmail)
        assert(firstUser != null) { "First user should be registered in the database" }
        val firstWallet = firstUser!!.wallets.firstOrNull()
        assert(firstWallet != null) { "First user should have at least one wallet" }
        assert(firstWallet!!.balance == 1000.0) { "First user's wallet balance should remain unchanged at 1000.0" }
    }


    /**
     * This test checks that a user cannot transfer more than their wallet balance.
     * It assumes that the wallet service validates the balance before processing the transfer.
     */
    @Test
    fun userShouldNotBeAbleToTransferMoreThanBalance() {
        // Try to transfer more than the wallet balance (which is initially 0)
        mockMvc.perform(
            post("/transfer")
                .cookie(firstUserCookie)
                .contentType("application/json")
                .content(
                    """
                    {
                        "toEmail": "$secondEmail",
                        "amount": 100,
                        "description": "Invalid transfer"
                    }
                """.trimIndent()
                )
        )
            .andExpect(status().isForbidden)
            .andExpect { status().reason("Insufficient funds") }

        // persistence check
        val firstUser = userHelperService.getUserWithWalletsAndHistory(firstEmail)
        assert(firstUser != null) { "First user should be registered in the database" }
        val firstWallet = firstUser!!.wallets.firstOrNull()
        assert(firstWallet != null) { "First user should have at least one wallet" }
        assert(firstWallet!!.balance == 0.0) { "First user's wallet balance should remain unchanged at 0.0" }
    }


    /**
     * This test checks that a user cannot transfer a negative amount.
     * It assumes that the wallet service validates the amount before processing the transfer.
     */
    @Test
    fun userShouldNotBeAbleToTransferNegativeAmount() {
        // Try to transfer a negative amount
        mockMvc.perform(
            post("/transfer")
                .cookie(firstUserCookie)
                .contentType("application/json")
                .content(
                    """
                    {
                        "toEmail": "$secondEmail",
                        "amount": -100,
                        "description": "Invalid transfer"
                    }
                """.trimIndent()
                )
        )
            .andExpect(status().isForbidden)
            .andExpect { status().reason("Amount must be positive") }

        // persistence check
        val firstUser = userHelperService.getUserWithWalletsAndHistory(firstEmail)
        assert(firstUser != null) { "First user should be registered in the database" }
        val firstWallet = firstUser!!.wallets.firstOrNull()
        assert(firstWallet != null) { "First user should have at least one wallet" }
        assert(firstWallet!!.balance == 0.0) { "First user's wallet balance should remain unchanged at 0.0" }
    }


    /**
     * This test checks that a user can view their transaction history after making a transfer.
     * It assumes that the wallet service records transfers in the transaction history.
     */
    @Test
    fun userShouldBeAbleToViewTransactionHistory() {
        // First deposit money to the wallet
        mockMvc.perform(
            post("/deposit")
                .cookie(firstUserCookie)
                .contentType("application/json")
                .content(
                    """
                    {
                        "amount": 1000,
                        "description": "Initial deposit"
                    }
                """.trimIndent()
                )
        )
            .andExpect(status().isOk)

        mockMvc.perform(
            post("/transfer")
                .cookie(firstUserCookie)
                .contentType("application/json")
                .content(
                    """
                    {
                        "toEmail": "$secondEmail",
                        "amount": 100,
                        "description": "Test transfer"
                    }
                """.trimIndent()
                ))
            .andExpect(status().isOk)
        // Check if the transaction history reflects the transfer
        mockMvc.perform(
            get("/history")
                .cookie(firstUserCookie)
                .contentType("application/json")
        )
            .andExpect(status().isOk)
            .andExpect { result ->
                val content: List<HistoryResponse> = ObjectMapper().readValue(
                    result.response.contentAsString,
                    ObjectMapper().typeFactory.constructCollectionType(List::class.java, HistoryResponse::class.java)
                )
                assert(content.isNotEmpty()) { "Transaction history should contain at least one entry" }
                assert(content.any { it.description == "Transfer to $secondName" }) { "Transaction history should contain the transfer to $secondEmail" }
                assert(content.any { it.amount == 100.0 }) { "Transaction history should contain the transfer amount of 100.0" }
            }
        // persistence check
        val firstUser = userHelperService.getUserWithWalletsAndHistory(firstEmail)
        assert(firstUser != null) { "First user should be registered in the database" }
        val firstWallet = firstUser!!.wallets.firstOrNull()
        assert(firstWallet != null) { "First user should have at least one wallet" }
        val history = firstWallet!!.history
        assert(history.isNotEmpty()) { "Transaction history should not be empty" }
        assert(history.any { it.description == "Transfer to $secondName" }) { "Transaction history should contain the transfer to $secondEmail" }
        assert(history.any { it.amount == 100.0 }) { "Transaction history should contain the transfer amount of 100.0" }
        assert(history.any { it.type == TransactionType.TRANSFER_OUT }) { "Transaction history should contain a transfer entry" }

        val secondUser = userHelperService.getUserWithWalletsAndHistory(secondEmail)
        assert(secondUser != null) { "Second user should be registered in the database" }
        val secondWallet = secondUser!!.wallets.firstOrNull()
        assert(secondWallet != null) { "Second user should have at least one wallet" }
        val secondHistory = secondWallet!!.history
        assert(secondHistory.isNotEmpty()) { "Second user's transaction history should not be empty" }
        assert(secondHistory.any { it.description == "Transfer from $firstName" }) { "Second user's transaction history should contain the transfer from $firstEmail" }
        assert(secondHistory.any { it.amount == 100.0 }) { "Second user's transaction history should contain the transfer amount of 100.0" }
        assert(secondHistory.any { it.type == TransactionType.TRANSFER_IN }) { "Second user's transaction history should contain a transfer entry" }
    }
}

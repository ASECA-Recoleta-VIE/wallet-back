package com.walletapi.integration.blackbox

import com.walletapi.repositories.HistoryRepository
import com.walletapi.repositories.UserRepository
import com.walletapi.repositories.WalletRepository
import org.junit.jupiter.api.BeforeEach
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc

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


    @BeforeEach
    fun setup() {
        // Clear the database before each test
        historyRepository.deleteAll()
        walletRepository.deleteAll()
        userRepository.deleteAll()

        // create a test
    }
}
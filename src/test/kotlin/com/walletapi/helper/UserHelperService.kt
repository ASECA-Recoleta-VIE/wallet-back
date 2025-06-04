package com.walletapi.helper

import com.walletapi.entities.UserEntity
import com.walletapi.repositories.UserRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class UserHelperService(
    private val userRepository: UserRepository
) {
    fun getUserWithWalletsAndHistory(email: String): UserEntity? {
        val user = userRepository.findByEmailEager(email) ?: return null
        // Force loading of wallet histories (but won't be used directly)
        userRepository.findWalletsWithHistoryByUserEmail(email)
        return user
    }
}

package com.walletapi.repositories

import com.walletapi.entities.UserEntity
import com.walletapi.entities.WalletEntity
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.util.*

interface UserRepository: JpaRepository<UserEntity, String> {
    fun findByEmail(email: String): UserEntity?

    fun findByEmailStartsWith(emailPrefix: String): List<UserEntity>

    @Query("SELECT u FROM UserEntity u LEFT JOIN FETCH u.wallets WHERE u.email = :email")
    fun findByEmailEager(@Param("email") email: String): UserEntity?

    @Query("SELECT w FROM WalletEntity w JOIN w.user u LEFT JOIN FETCH w.history WHERE u.email = :email")
    fun findWalletsWithHistoryByUserEmail(@Param("email") email: String): List<WalletEntity>
    fun existsByEmail(email: String): Boolean

    fun getUserEntityById(userId: String): Optional<UserEntity>
}
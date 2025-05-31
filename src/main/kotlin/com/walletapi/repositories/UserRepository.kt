package com.walletapi.repositories

import com.walletapi.entities.UserEntity
import org.springframework.data.jpa.repository.JpaRepository
import java.util.Optional

interface UserRepository: JpaRepository<UserEntity, String> {
    fun findByEmail(email: String): UserEntity?
    fun existsByEmail(email: String): Boolean

    fun getUserEntityById(userId: String): Optional<UserEntity>
}
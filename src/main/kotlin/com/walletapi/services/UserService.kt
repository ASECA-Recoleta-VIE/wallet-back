package com.walletapi.services

import com.walletapi.domain_services.DomainUserService
import com.walletapi.dto.response.UserResponse
import com.walletapi.entities.UserEntity
import com.walletapi.entities.userToEntity
import com.walletapi.entities.walletToEntity
import com.walletapi.exceptions.UserException
import com.walletapi.models.Wallet
import com.walletapi.repositories.HistoryRepository
import com.walletapi.repositories.UserRepository
import com.walletapi.repositories.WalletRepository
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.SignatureAlgorithm
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseCookie
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Service
import java.security.Key
import java.time.LocalDateTime
import java.util.*
import javax.crypto.spec.SecretKeySpec

@Service
class UserService {
    @Autowired
    private lateinit var walletRepository: WalletRepository

    @Autowired
    lateinit var userService: DomainUserService

    @Autowired
    lateinit var userRepository: UserRepository
    fun createUser(fullName: String, email: String, password: String): ResponseEntity<UserResponse> {
        if (email.isEmpty() || password.isEmpty() || fullName.isEmpty()) {
            throw UserException.EmptyCredentialsException()
        }

        if (userRepository.existsByEmail(email)) {
            throw IllegalArgumentException("User with email $email already exists")
        }
        val user = userService.createUser(
            fullName = fullName,
            email = email,
            password = password,
            wallets = listOf(
                Wallet(
                    name = "Main Wallet",
                    balance = 0.0,
                )
            )
        )
        try {
            userRepository.save(userToEntity(user))
            val userEntity = userRepository.findByEmail(email)!!
            walletRepository.save(walletToEntity(user.wallets[0], userEntity))

        } catch (e: Exception) {
            if (e is UserException.WeakPasswordException) {
                throw e
            }
            throw RuntimeException("Failed to create user: ${e.message}", e)
        }
        return ResponseEntity(
            UserResponse(fullName = user.fullName, email = user.email),
            HttpStatus.CREATED
        )
    }

    fun loginUser(email: String, password: String): ResponseEntity<UserResponse> {
        if (email.isEmpty() || password.isEmpty()) {
            throw UserException.EmptyCredentialsException()
        }

        val user = userRepository.findByEmail(email)
            ?: throw UserException.InvalidCredentialsException(email)

        if (!this.checkPassword(email, password, user.password)) {
            throw UserException.InvalidCredentialsException(email)
        }

        // Generate JWT token
        val token = generateJwtToken(user)

        // Create a secure cookie with the token
        val cookie = ResponseCookie.from("token", token)
            .httpOnly(true)
            .secure(false)
            .path("/")
            .maxAge(60 * 60 * 2)
            .sameSite("Strict")
            .build()

        val userResponse: UserResponse = UserResponse(fullName = user.fullName, email = user.email)

        return ResponseEntity
            .ok()
            .header("Set-Cookie", cookie.toString())
            .body(userResponse)
    }

    fun listUsers(prefix: String): ResponseEntity<List<UserResponse>> {
        val users = userRepository.findByEmailStartsWith(prefix)
        val userResponses = users.map { UserResponse(fullName = it.fullName, email = it.email) }
        return ResponseEntity(userResponses, HttpStatus.OK)
    }

    private fun generateJwtToken(user: UserEntity): String {
        val secretKey = "abcdefghijklmnopqrstuvwxyz1234567890!@#$%^&*()_+-=[pipe]{}|;':\",.<div>?/"
        val key: Key = SecretKeySpec(secretKey.toByteArray(), SignatureAlgorithm.HS256.jcaName)
        return Jwts.builder()
            .setSubject(user.id)
            .claim("email", user.email)
            .claim("fullName", user.fullName)
            .setIssuedAt(Date())
            .setExpiration(
                Date.from(
                    LocalDateTime.now().plusHours(2).atZone(java.time.ZoneId.systemDefault()).toInstant()
                )
            )
            .signWith(key, SignatureAlgorithm.HS256)
            .compact()
    }


   private fun checkPassword(email: String, password: String, hash: String): Boolean {
        return userService.verifyPassword(password, hash)
    }
}
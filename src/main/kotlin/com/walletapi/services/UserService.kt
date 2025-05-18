package com.walletapi.services

import com.walletapi.dto.PasswordValidation
import com.walletapi.domain_services.DomainUserService
import com.walletapi.entities.UserEntity
import com.walletapi.entities.userToEntity
import com.walletapi.models.Wallet
import com.walletapi.repositories.UserRepository
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
    lateinit var userService: DomainUserService
    @Autowired
    lateinit var userRepository: UserRepository
    fun createUser(fullName: String, email: String, password: String): ResponseEntity<Any> {
        if(email.isEmpty() || password.isEmpty() || fullName.isEmpty()) {
            return ResponseEntity(
                "Email, password and full name cannot be empty",
                HttpStatus.BAD_REQUEST
            )
        }

        val passwordValidation = validatePassword(password)
        if (passwordValidation != PasswordValidation.VALID) {
            return ResponseEntity(
                passwordValidation.name,
                HttpStatus.BAD_REQUEST
            )
        }

        if(userRepository.existsByEmail(email)) {
            throw IllegalArgumentException("User with email $email already exists")
        }
        val user = userService.createUser(
            fullName = fullName,
            email = email,
            password = password,
            wallets = listOf(Wallet(
                name = "Main Wallet",
                balance = 10000.0,
            ))
        )
        try {
            userRepository.save(userToEntity(user))
        } catch (e: Exception) {
            return ResponseEntity(
                "Error creating user: ${e.message}",
                HttpStatus.INTERNAL_SERVER_ERROR
            )
        }
        return ResponseEntity(
            user,
            HttpStatus.CREATED
        )
    }

    fun loginUser(email: String, password: String): ResponseEntity<Any> {
        if (email.isEmpty() || password.isEmpty()) {
            return ResponseEntity(
                "Email and password cannot be empty",
                HttpStatus.BAD_REQUEST
            )
        }

        val user = userRepository.findByEmail(email)
            ?: return ResponseEntity(
                "User with email $email not found",
                HttpStatus.NOT_FOUND
            )

        if (!this.checkPassword(email, password, user.password)) {
            return ResponseEntity(
                "Invalid password",
                HttpStatus.UNAUTHORIZED
            )
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

        return ResponseEntity
            .ok()
            .header("Set-Cookie", cookie.toString())
            .body("login-success")
    }

    private fun generateJwtToken(user: UserEntity): String {
        val secretKey = "abcdefghijklmnopqrstuvwxyz1234567890!@#$%^&*()_+-=[pipe]{}|;':\",.<div>?/"
        val key: Key = SecretKeySpec(secretKey.toByteArray(), SignatureAlgorithm.HS256.jcaName)
        return Jwts.builder()
            .setSubject(user.email)
            .claim("id", user.id)
            .claim("fullName", user.fullName)
            .setIssuedAt(Date())
            .setExpiration( Date.from(LocalDateTime.now().plusHours(2).atZone(java.time.ZoneId.systemDefault()).toInstant()))
            .signWith(key, SignatureAlgorithm.HS256)
            .compact()
    }



    fun checkPassword(email:String, password: String, hash: String): Boolean {
        return userService.verifyPassword(password, hash)
    }






    fun validatePassword(password: String): PasswordValidation {
        // should have 1 upper case letter, 1 lower case letter, 1 digit and 1 special character
        if (password.length < 8) {
            return PasswordValidation.PASSWORD_TOO_SHORT
        }
        if (!password.any { it.isUpperCase() }) {
            return PasswordValidation.NO_UPPERCASE
        }
        if (!password.any { it.isLowerCase() }) {
            return PasswordValidation.NO_LOWERCASE
        }
        if (!password.any { it.isDigit() }) {
            return PasswordValidation.NO_NUMBER
        }
        if (!password.any { it in "!@#$%^&*()_+-=[]{}|;':\",.<>?/" }) {
            return PasswordValidation.NO_SPECIAL_CHAR
        }
        return PasswordValidation.VALID
    }
}
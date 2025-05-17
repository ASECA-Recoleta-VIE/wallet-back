package com.walletapi.controllers

import com.walletapi.dto.request.LoginRequest
import com.walletapi.dto.request.RegisterRequest
import com.walletapi.services.UserService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/users")
class UserController {

    @Autowired
    lateinit var userService: UserService

    @PostMapping("/register")
    fun registerUser(@RequestBody request: RegisterRequest): ResponseEntity<Any> {
        return userService.createUser(
            fullName = request.fullName,
            email = request.email,
            password = request.password
        )
    }

    @PostMapping("/login")
    fun loginUser(@RequestBody request: LoginRequest): ResponseEntity<Any> {
        return userService.loginUser(
            email = request.email,
            password = request.password
        )
    }
}

package com.walletapi.controllers

import com.walletapi.dto.request.LoginRequest
import com.walletapi.dto.request.RegisterRequest
import com.walletapi.dto.response.UserResponse
import com.walletapi.services.UserService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/users")
class UserController {

    @Autowired
    lateinit var userService: UserService

    @Operation(summary = "Register a new user")
    @ApiResponses(value = [
        ApiResponse(responseCode = "201", description = "User registered successfully"),
        ApiResponse(responseCode = "400", description = "Invalid request data")
    ])
    @PostMapping("/register")
    fun registerUser(@RequestBody request: RegisterRequest): ResponseEntity<UserResponse> {
        return userService.createUser(
            fullName = request.fullName,
            email = request.email,
            password = request.password
        )
    }

    @Operation(summary = "Login a user")
    @ApiResponses(value = [
        ApiResponse(responseCode = "200", description = "User logged in successfully"),
        ApiResponse(responseCode = "401", description = "Invalid email or password")
    ])
    @PostMapping("/login")
    fun loginUser(@RequestBody request: LoginRequest): ResponseEntity<UserResponse> {
        return userService.loginUser(
            email = request.email,
            password = request.password
        )
    }

    @Operation(summary = "List users by email prefix")
    @ApiResponses(value = [
        ApiResponse(responseCode = "200", description = "Users retrieved successfully"),
    ])
    @GetMapping("/list")
    fun listUsersByEmailPrefix(@RequestParam prefix: String): ResponseEntity<List<UserResponse>> {
        if (prefix.isEmpty()) {
            return ResponseEntity.ok(emptyList())
        }
        val users = userService.listUsers(prefix)
        return users
    }
}

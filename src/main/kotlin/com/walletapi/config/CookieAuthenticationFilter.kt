package com.walletapi.config

import com.walletapi.repositories.UserRepository
import io.jsonwebtoken.Claims
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.SignatureAlgorithm
import jakarta.servlet.Filter
import jakarta.servlet.FilterChain
import jakarta.servlet.ServletRequest
import jakarta.servlet.ServletResponse
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter
import java.security.Key
import javax.crypto.spec.SecretKeySpec
import kotlin.jvm.optionals.getOrNull

@Component
class CookieAuthenticationFilter @Autowired constructor(
    private val userRepository: UserRepository
) : OncePerRequestFilter() {

    val secretKey = "abcdefghijklmnopqrstuvwxyz1234567890!@#$%^&*()_+-=[pipe]{}|;':\",.<div>?/"
    private val key: Key = SecretKeySpec(secretKey.toByteArray(), SignatureAlgorithm.HS256.jcaName)

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain
    ) {
        val httpRequest = request
        val httpResponse = response
        // Skip authentication for login, register, and Swagger/OpenAPI endpoints
        if (httpRequest.requestURI.contains("/api/users/login") ||
            "OPTIONS" == httpRequest.method ||
            httpRequest.requestURI.contains("/api/users/register") ||
            httpRequest.requestURI.contains("swagger-ui") ||
            httpRequest.requestURI.contains("api-docs") ||
            httpRequest.requestURI == "/swagger-ui.html"
        ) {
            filterChain.doFilter(httpRequest, httpResponse)
            return
        }

        val cookies = httpRequest.cookies
        if (cookies == null || cookies.isEmpty()) {
            httpResponse.status = HttpStatus.UNAUTHORIZED.value()
            httpResponse.writer.write("Unauthorized: No authentication cookie found")
            return
        }

        val tokenCookie = cookies.find { it.name == "token" }
        if (tokenCookie == null) {
            httpResponse.status = HttpStatus.UNAUTHORIZED.value()
            httpResponse.writer.write("Unauthorized: No token cookie found")
            return
        }

        val token = tokenCookie.value

        try {
            val claims: Claims = Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token)
                .body

            if (claims.expiration.before(java.util.Date())) {
                httpResponse.status = HttpStatus.UNAUTHORIZED.value()
                httpResponse.writer.write("Unauthorized: Token has expired")
                return
            }

            // Get user Id from claims and put it in the request attribute
            val userId = claims.subject
            if (userId.isNullOrEmpty()) {
                httpResponse.status = HttpStatus.UNAUTHORIZED.value()
                httpResponse.writer.write("Unauthorized: Invalid token")
                return
            }
            // check if there is a user with this id in the database
            val optionalUser = userRepository.getUserEntityById(userId)
            if (optionalUser.isEmpty) {
                httpResponse.status = HttpStatus.UNAUTHORIZED.value()
                httpResponse.writer.write("Unauthorized: User not found")
                return
            }

            httpRequest.setAttribute("user", optionalUser.get())

            // Token is valid, continue with the request
            filterChain.doFilter(httpRequest, httpResponse)

        } catch (e: Exception) {
            if (e is io.jsonwebtoken.ExpiredJwtException) {
                httpResponse.status = HttpStatus.UNAUTHORIZED.value()
                httpResponse.writer.write("Unauthorized: Token has expired")
            } else if (e is io.jsonwebtoken.InvalidClaimException) {
                httpResponse.status = HttpStatus.UNAUTHORIZED.value()
                httpResponse.writer.write("Unauthorized: Invalid token claims")
            } else if (e is io.jsonwebtoken.MalformedJwtException) {
                httpResponse.status = HttpStatus.UNAUTHORIZED.value()
                httpResponse.writer.write("Unauthorized: Malformed token")
            }
            else {
                httpResponse.status = HttpStatus.UNAUTHORIZED.value()
                httpResponse.writer.write("Unauthorized: Invalid token")
            }
        }
    }
}

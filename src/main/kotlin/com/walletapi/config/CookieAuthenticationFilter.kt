package com.walletapi.config

import io.jsonwebtoken.Claims
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.SignatureAlgorithm
import jakarta.servlet.Filter
import jakarta.servlet.FilterChain
import jakarta.servlet.ServletRequest
import jakarta.servlet.ServletResponse
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Component
import java.security.Key
import javax.crypto.spec.SecretKeySpec

@Component
class CookieAuthenticationFilter : Filter {

    val secretKey = "abcdefghijklmnopqrstuvwxyz1234567890!@#$%^&*()_+-=[pipe]{}|;':\",.<div>?/"
    private val key: Key = SecretKeySpec(secretKey.toByteArray(), SignatureAlgorithm.HS256.jcaName)

    override fun doFilter(
        request: ServletRequest,
        response: ServletResponse,
        filterChain: FilterChain
    ) {
        val httpRequest = request as HttpServletRequest
        val httpResponse = response as HttpServletResponse
        // Skip authentication for login, register, and Swagger/OpenAPI endpoints
        if (httpRequest.requestURI.contains("/api/users/login") ||
            "OPTIONS" == httpRequest.method ||
            httpRequest.requestURI.contains("/api/users/register") ||
            httpRequest.requestURI.contains("swagger-ui") ||
            httpRequest.requestURI.contains("api-docs") ||
            httpRequest.requestURI == "/swagger-ui.html") {
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
                throw Exception("Token has expired")
            }

            // Token is valid, continue with the request
            filterChain.doFilter(httpRequest, httpResponse)
        } catch (e: Exception) {
            httpResponse.status = HttpStatus.UNAUTHORIZED.value()
            httpResponse.writer.write("Unauthorized: ${e.message ?: "Invalid token"}")
        }
    }
}

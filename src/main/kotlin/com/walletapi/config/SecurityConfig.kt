package com.walletapi.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.web.SecurityFilterChain
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter

@Configuration
@EnableWebSecurity
class SecurityConfig(
    private val cookieAuthenticationFilter: CookieAuthenticationFilter
) {

    @Bean
    fun securityFilterChain(http: HttpSecurity): SecurityFilterChain {
        http
            .csrf { csrf -> csrf.disable() }
            .authorizeHttpRequests { authorize ->
                authorize
                    .requestMatchers("/api/users/register", "/api/users/login").permitAll()
                    .anyRequest().authenticated()
            }
            // Add the cookie authentication filter
            .addFilterBefore(cookieAuthenticationFilter, UsernamePasswordAuthenticationFilter::class.java)

        return http.build()
    }
}
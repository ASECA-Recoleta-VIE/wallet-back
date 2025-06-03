package com.walletapi.config

import org.springframework.boot.web.servlet.FilterRegistrationBean
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.beans.factory.annotation.Autowired

@Configuration
class FilterConfig {

    @Autowired
    private lateinit var cookieAuthenticationFilter: CookieAuthenticationFilter

    @Bean
    fun cookieAuthenticationFilterRegistration(): FilterRegistrationBean<CookieAuthenticationFilter> {
        val registrationBean = FilterRegistrationBean<CookieAuthenticationFilter>()
        registrationBean.filter = cookieAuthenticationFilter
        registrationBean.urlPatterns = listOf("/*")
        registrationBean.order = 1
        return registrationBean
    }
}
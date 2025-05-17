package com.walletapi.config

import com.walletapi.config.CookieAuthenticationFilter
import org.springframework.boot.web.servlet.FilterRegistrationBean
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class WebConfig {

    @Bean
    fun cookieAuthenticationFilter(): FilterRegistrationBean<CookieAuthenticationFilter> {
        val registrationBean = FilterRegistrationBean<CookieAuthenticationFilter>()
        registrationBean.filter = CookieAuthenticationFilter()
        registrationBean.addUrlPatterns("/api/*")
        registrationBean.order = 1
        return registrationBean
    }
}
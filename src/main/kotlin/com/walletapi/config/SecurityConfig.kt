package com.walletapi.config

import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
 class FilterConfig {

    @Bean
    fun loggingFilterRegistration(): FilterRegistrationBean<CookieAuthenticationFilter> {
        val registrationBean = FilterRegistrationBean<CookieAuthenticationFilter>();
        registrationBean.filter = CookieAuthenticationFilter();
        registrationBean.order = 1;
        registrationBean.addUrlPatterns("*");
        registrationBean.setName("LoggingFilter");
        return registrationBean;
    }
}


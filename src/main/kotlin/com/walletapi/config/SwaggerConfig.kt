package com.walletapi.config

import io.swagger.v3.oas.models.ExternalDocumentation
import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.info.Info
import io.swagger.v3.oas.models.info.License
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class SwaggerConfig {
    
    @Bean
    fun walletApiDocs(): OpenAPI {
        return OpenAPI()
            .info(
                Info().title("Wallet API")
                    .description("Digital wallet management API")
                    .version("v1.0")
                    .license(
                        License().name("API License")
                            .url("https://your-license-url.com")
                    )
            )
            .externalDocs(
                ExternalDocumentation()
                    .description("Wallet API Documentation")
                    .url("https://your-documentation-url.com")
            )
    }
}

package br.com.geminiproject.dcl

import io.swagger.v3.oas.models.Components
import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.security.OAuthFlow
import io.swagger.v3.oas.models.security.OAuthFlows
import io.swagger.v3.oas.models.security.SecurityScheme
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class OpenApiConfig {

    @Bean
    fun customOpenAPI(): OpenAPI {
        return OpenAPI()
            .components(Components()
                .addSecuritySchemes("Keycloak", SecurityScheme()
                    .type(SecurityScheme.Type.OAUTH2)
                    .flows(OAuthFlows()
                        .password(OAuthFlow()
                            .tokenUrl("http://localhost:8081/realms/master/protocol/openid-connect/token")))))
            .addSecurityItem(io.swagger.v3.oas.models.security.SecurityRequirement().addList("Keycloak"))
    }
}
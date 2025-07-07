package de.org.dexterity.bookanything.wire.springdoc

import io.swagger.v3.oas.models.Components
import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.security.OAuthFlow
import io.swagger.v3.oas.models.security.OAuthFlows
import io.swagger.v3.oas.models.security.SecurityRequirement
import io.swagger.v3.oas.models.security.SecurityScheme
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class OpenApiConfig {

    @Bean
    fun customOpenAPI(): OpenAPI {
        return OpenAPI()
            .components(
                Components()
                .addSecuritySchemes("Keycloak", SecurityScheme()
                    .type(SecurityScheme.Type.OAUTH2)
                    .flows(
                        OAuthFlows()
                        .password(
                            OAuthFlow()
                            .tokenUrl("http://localhost:9080/realms/dexterity-apps-01/protocol/openid-connect/token")))))
            .addSecurityItem(SecurityRequirement().addList("Keycloak"))
    }
}
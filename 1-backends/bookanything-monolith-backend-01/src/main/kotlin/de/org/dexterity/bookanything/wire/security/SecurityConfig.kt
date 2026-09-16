package de.org.dexterity.bookanything.wire.security

import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.config.Customizer
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.oauth2.jwt.JwtDecoder
import org.springframework.security.oauth2.jwt.JwtDecoders
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder
import org.springframework.security.web.SecurityFilterChain

@Configuration
@EnableWebSecurity
class SecurityConfig {

    @Bean
    fun securityFilterChain(http: HttpSecurity): SecurityFilterChain {
        http
            .csrf { csrf -> csrf.disable() }
            .authorizeHttpRequests { auth ->
                auth.requestMatchers(
                    "/v3/api-docs/**",
                    "/swagger-ui/**",
                    "/swagger-ui.html",
                    "/api/cicd/status",
                    "/actuator/**"
                ).permitAll()
                auth.requestMatchers("/api/v1/localizable-places/**").authenticated()
                // auth.requestMatchers("/api/v1/geolocation/**").authenticated()
                auth.requestMatchers("/api/v1/addresses/**").authenticated()
                auth.anyRequest().permitAll()
            }
            .oauth2ResourceServer { oauth2 -> oauth2.jwt(Customizer.withDefaults()) }
            .sessionManagement { session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS) }
        return http.build()
    }

    @Bean
    @ConditionalOnMissingBean(JwtDecoder::class)
    fun jwtDecoder(
        @Value("\${spring.security.oauth2.resourceserver.jwt.jwk-set-uri:\${spring.security.oauth2.resource-server.jwt.jwk-set-uri:}}")
        jwkSetUri: String,
        @Value("\${spring.security.oauth2.resourceserver.jwt.issuer-uri:\${spring.security.oauth2.resource-server.jwt.issuer-uri:}}")
        issuerUri: String
    ): JwtDecoder {
        return when {
            jwkSetUri.isNotBlank() -> NimbusJwtDecoder.withJwkSetUri(jwkSetUri).build()
            issuerUri.isNotBlank() -> JwtDecoders.fromIssuerLocation(issuerUri)
            else -> NimbusJwtDecoder.withJwkSetUri(
                "http://tenant-keycloak.drr-tnt-swfabrik-europe-dev.svc.cluster.local:8080/realms/dexterity-apps-01/protocol/openid-connect/certs"
            ).build()
        }
    }
}
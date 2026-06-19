package com.cts.apiGateway.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.SecurityWebFiltersOrder;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.web.cors.reactive.CorsConfigurationSource;

import com.cts.apiGateway.security.JwtAuthenticationFilter;

import reactor.core.publisher.Mono;

@Configuration
@EnableWebFluxSecurity
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final CorsConfigurationSource corsConfigurationSource;

    public SecurityConfig(JwtAuthenticationFilter jwtAuthenticationFilter,
                          CorsConfigurationSource corsConfigurationSource) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
        this.corsConfigurationSource = corsConfigurationSource;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityWebFilterChain securityWebFilterChain(ServerHttpSecurity http) {

        return http

                .csrf(ServerHttpSecurity.CsrfSpec::disable)
                .httpBasic(ServerHttpSecurity.HttpBasicSpec::disable)
                .formLogin(ServerHttpSecurity.FormLoginSpec::disable)
                .cors(cors -> cors.configurationSource(corsConfigurationSource))  // ✅ Use explicit CORS config

                .exceptionHandling(eh ->
                        eh.authenticationEntryPoint((exchange, e) -> {
                            exchange.getResponse()
                                    .setStatusCode(HttpStatus.UNAUTHORIZED);
                            return Mono.empty();
                        }))

                .authorizeExchange(exchange -> exchange

                        // Allow all preflight requests
                        .pathMatchers(HttpMethod.OPTIONS, "/**")
                        .permitAll()

                        // Health and actuator endpoints
                        .pathMatchers("/actuator/**")
                        .permitAll()

                        .pathMatchers("/health")
                        .permitAll()

                        // Public auth APIs
                        .pathMatchers(HttpMethod.POST, "/auth/login")
                        .permitAll()

                        .pathMatchers(HttpMethod.POST, "/auth/register")
                        .permitAll()

                        .pathMatchers(HttpMethod.POST, "/auth/admin/create-superadmin")
                        .permitAll()

                        // Customer public auth endpoints (no JWT required for signup/login)
                        .pathMatchers(HttpMethod.POST, "/auth/customer/signup")
                        .permitAll()

                        .pathMatchers(HttpMethod.POST, "/auth/customer/login")
                        .permitAll()

                        // Customer API endpoints - require JWT with CUSTOMER role
                        .pathMatchers(HttpMethod.GET, "/api/customers/**")
                        .hasRole("CUSTOMER")

                        .pathMatchers(HttpMethod.POST, "/api/customers/**")
                        .hasRole("CUSTOMER")

                        .pathMatchers(HttpMethod.PUT, "/api/customers/**")
                        .hasRole("CUSTOMER")

                        // Transactions - require CUSTOMER role for customer operations
                        .pathMatchers(HttpMethod.GET, "/api/transactions/customer/**")
                        .hasRole("CUSTOMER")

                        .pathMatchers(HttpMethod.POST, "/api/transactions/customer/**")
                        .hasRole("CUSTOMER")

                        .pathMatchers(HttpMethod.PUT, "/api/transactions/**")
                        .hasRole("SUPER_ADMIN")

                        .pathMatchers(HttpMethod.DELETE, "/api/transactions/**")
                        .hasRole("SUPER_ADMIN")

                        // Investigation
                        .pathMatchers(HttpMethod.GET, "/api/investigation/**")
                        .hasAnyRole("SUPER_ADMIN", "FRAUD_ANALYST")

                        .pathMatchers(HttpMethod.PUT, "/api/investigation/cases/*/status")
                        .hasAnyRole("SUPER_ADMIN", "FRAUD_ANALYST")

                        .pathMatchers(HttpMethod.POST, "/api/investigation/**")
                        .hasRole("SUPER_ADMIN")

                        // SAR
                        .pathMatchers(HttpMethod.GET, "/sar/**")
                        .hasAnyRole("SUPER_ADMIN", "FRAUD_ANALYST")

                        .pathMatchers(HttpMethod.POST, "/sar/**")
                        .hasRole("SUPER_ADMIN")

                        // Enrichment
                        .pathMatchers("/api/enrich/**")
                        .hasAnyRole("SUPER_ADMIN", "RISK_MANAGER")

                        // Gemini
                        .pathMatchers("/api/gemini/**")
                        .hasAnyRole("SUPER_ADMIN", "RISK_MANAGER")

                        .anyExchange()
                        .authenticated())

                .addFilterAt(
                        jwtAuthenticationFilter,
                        SecurityWebFiltersOrder.AUTHENTICATION)

                .build();
    }
}
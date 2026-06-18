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
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.reactive.CorsWebFilter;
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;

import com.cts.apiGateway.security.JwtAuthenticationFilter;

import reactor.core.publisher.Mono;

@Configuration
@EnableWebFluxSecurity
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    public SecurityConfig(JwtAuthenticationFilter jwtAuthenticationFilter) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public CorsWebFilter corsWebFilter() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowCredentials(true);
        config.addAllowedOrigin("http://localhost:3000");
        config.addAllowedOrigin("http://localhost:5173");
        config.addAllowedOrigin("http://localhost:5174");
        config.addAllowedOrigin("https://bank-gaurd-frontend.vercel.app");
        config.addAllowedOrigin("https://bank-gaurd-customer.vercel.app");
        config.addAllowedHeader("*");
        config.addAllowedMethod("*");
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return new CorsWebFilter(source);
    }

    @Bean
    public SecurityWebFilterChain securityWebFilterChain(ServerHttpSecurity http) {

        return http

                .csrf(ServerHttpSecurity.CsrfSpec::disable)
                .httpBasic(ServerHttpSecurity.HttpBasicSpec::disable)
                .formLogin(ServerHttpSecurity.FormLoginSpec::disable)
                .cors(cors -> cors.disable())

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

                        // Public auth APIs
                        .pathMatchers(HttpMethod.POST, "/auth/login")
                        .permitAll()

                        .pathMatchers(HttpMethod.POST, "/auth/register")
                        .permitAll()

                        .pathMatchers(HttpMethod.POST,
                                "/auth/admin/create-superadmin")
                        .permitAll()

                        // Customer APIs
                        .pathMatchers("/api/customers/**")
                        .permitAll()

                        // Transactions
                        .pathMatchers(HttpMethod.GET,
                                "/api/transactions/**")
                        .permitAll()

                        .pathMatchers(HttpMethod.POST,
                                "/api/transactions/**")
                        .permitAll()

                        .pathMatchers(HttpMethod.PUT,
                                "/api/transactions/**")
                        .hasRole("SUPER_ADMIN")

                        .pathMatchers(HttpMethod.DELETE,
                                "/api/transactions/**")
                        .hasRole("SUPER_ADMIN")

                        // Investigation
                        .pathMatchers(HttpMethod.GET,
                                "/api/investigation/**")
                        .hasAnyRole(
                                "SUPER_ADMIN",
                                "FRAUD_ANALYST")

                        .pathMatchers(HttpMethod.PUT,
                                "/api/investigation/cases/*/status")
                        .hasAnyRole(
                                "SUPER_ADMIN",
                                "FRAUD_ANALYST")

                        .pathMatchers(HttpMethod.POST,
                                "/api/investigation/**")
                        .hasRole("SUPER_ADMIN")

                        // SAR
                        .pathMatchers(HttpMethod.GET,
                                "/sar/**")
                        .hasAnyRole(
                                "SUPER_ADMIN",
                                "FRAUD_ANALYST")

                        .pathMatchers(HttpMethod.POST,
                                "/sar/**")
                        .hasRole("SUPER_ADMIN")

                        // Enrichment
                        .pathMatchers("/api/enrich/**")
                        .hasAnyRole(
                                "SUPER_ADMIN",
                                "RISK_MANAGER")

                        // Gemini
                        .pathMatchers("/api/gemini/**")
                        .hasAnyRole(
                                "SUPER_ADMIN",
                                "RISK_MANAGER")

                        .anyExchange()
                        .authenticated())

                .addFilterAt(
                        jwtAuthenticationFilter,
                        SecurityWebFiltersOrder.AUTHENTICATION)

                .build();
    }
}
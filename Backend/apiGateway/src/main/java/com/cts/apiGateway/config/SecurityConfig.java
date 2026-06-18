package com.cts.apiGateway.config;

import java.util.List;

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
import org.springframework.web.cors.reactive.CorsConfigurationSource;
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
    public CorsConfigurationSource corsConfigurationSource() {

        CorsConfiguration config = new CorsConfiguration();

        config.setAllowedOriginPatterns(List.of(
                "http://localhost:*",
                "https://bank-gaurd-frontend.vercel.app",
                "https://bank-gaurd-customer.vercel.app" // replace with actual URL later
        ));

        config.setAllowedMethods(List.of(
                "GET",
                "POST",
                "PUT",
                "DELETE",
                "PATCH",
                "OPTIONS"
        ));

        config.setAllowedHeaders(List.of("*"));
        config.setAllowCredentials(true);
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source =
                new UrlBasedCorsConfigurationSource();

        source.registerCorsConfiguration("/**", config);

        return source;
    }

    @Bean
    public SecurityWebFilterChain securityWebFilterChain(ServerHttpSecurity http) {

        return http

                .csrf(ServerHttpSecurity.CsrfSpec::disable)
                .httpBasic(ServerHttpSecurity.HttpBasicSpec::disable)
                .formLogin(ServerHttpSecurity.FormLoginSpec::disable)
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))

                .exceptionHandling(eh -> eh.authenticationEntryPoint((swe, e) -> {
                    swe.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
                    return Mono.empty();
                }))

                .authorizeExchange(exchange -> exchange

                        // =================================================
                        // AUTH APIs
                        // =================================================
                        .pathMatchers(HttpMethod.POST, "/auth/login").permitAll()
                        .pathMatchers(HttpMethod.POST, "/auth/register").permitAll()
                        .pathMatchers(HttpMethod.POST, "/auth/admin/create-superadmin").permitAll()

                        // =================================================
                        // CUSTOMER APIs
                        // Entire customer module is public
                        // =================================================
                        .pathMatchers("/api/customers/**").permitAll()

                        // =================================================
                        // USER MANAGEMENT
                        // =================================================
                        .pathMatchers("/auth/users/**")
                        .hasRole("SUPER_ADMIN")

                        // =================================================
                        // TRANSACTION APIs
                        // =================================================
                        .pathMatchers(HttpMethod.GET, "/api/transactions/**")
                        .permitAll()

                        .pathMatchers(HttpMethod.POST, "/api/transactions/**")
                        .permitAll()

                        .pathMatchers(HttpMethod.PUT, "/api/transactions/**")
                        .hasRole("SUPER_ADMIN")

                        .pathMatchers(HttpMethod.DELETE, "/api/transactions/**")
                        .hasRole("SUPER_ADMIN")

                        // =================================================
                        // ALERT CASE SERVICE
                        // =================================================
                        .pathMatchers(HttpMethod.GET, "/api/investigation/**")
                        .hasAnyRole(
                                "SUPER_ADMIN",
                                "FRAUD_ANALYST"
                        )

                        .pathMatchers(HttpMethod.PUT,
                                "/api/investigation/cases/*/status")
                        .hasAnyRole(
                                "SUPER_ADMIN",
                                "FRAUD_ANALYST"
                        )

                        .pathMatchers(HttpMethod.POST, "/api/investigation/**")
                        .hasRole("SUPER_ADMIN")

                        // =================================================
                        // SAR REPORTS
                        // =================================================
                        .pathMatchers(HttpMethod.GET, "/sar/**")
                        .hasAnyRole(
                                "SUPER_ADMIN",
                                "FRAUD_ANALYST"
                        )

                        .pathMatchers(HttpMethod.POST, "/sar/**")
                        .hasRole("SUPER_ADMIN")

                        // =================================================
                        // ENRICHMENT SERVICE
                        // =================================================
                        .pathMatchers("/api/enrich/**")
                        .hasAnyRole(
                                "SUPER_ADMIN",
                                "RISK_MANAGER"
                        )

                        // =================================================
                        // DECISION ENGINE
                        // =================================================
                        .pathMatchers("/api/gemini/**")
                        .hasAnyRole(
                                "SUPER_ADMIN",
                                "RISK_MANAGER"
                        )

                        // =================================================
                        // EVERYTHING ELSE
                        // =================================================
                        .anyExchange().authenticated()

                )

                .addFilterAt(
                        jwtAuthenticationFilter,
                        SecurityWebFiltersOrder.AUTHENTICATION
                )

                .build();
    }
}
package com.cts.apiGateway.security;

import java.util.List;

import org.springframework.http.HttpHeaders;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;

import reactor.core.publisher.Mono;

@Component
public class JwtAuthenticationFilter implements WebFilter {

    private final JwtUtil jwtUtil;

    public JwtAuthenticationFilter(JwtUtil jwtUtil) {
        this.jwtUtil = jwtUtil;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {

        ServerHttpRequest request = exchange.getRequest();
        String path = request.getPath().value();

        // Public endpoints
        if (
                path.equals("/auth/login")
                        || path.equals("/auth/register")
                        || path.equals("/auth/admin/create-superadmin")
                        || path.startsWith("/api/customers")
                        || (path.startsWith("/api/transactions")
                            && request.getMethod().name().equals("POST"))
        ) {
            return chain.filter(exchange);
        }

        String authHeader =
                request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);

        // No token → continue
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return chain.filter(exchange);
        }

        String token = authHeader.substring(7);

        try {

            if (jwtUtil.isTokenValid(token)) {

                String username = jwtUtil.extractUsername(token);
                String role = jwtUtil.extractRole(token);

                List<SimpleGrantedAuthority> authorities =
                        List.of(
                                new SimpleGrantedAuthority("ROLE_" + role)
                        );

                UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(
                                username,
                                null,
                                authorities
                        );

                return chain.filter(exchange)
                        .contextWrite(
                                ReactiveSecurityContextHolder
                                        .withAuthentication(authentication)
                        );
            }

        } catch (Exception e) {
            // Ignore invalid token and continue
        }

        return chain.filter(exchange);
    }
}
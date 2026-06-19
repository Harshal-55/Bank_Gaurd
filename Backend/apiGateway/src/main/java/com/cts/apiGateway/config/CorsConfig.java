package com.cts.apiGateway.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

@Configuration
public class CorsConfig {

    @Bean
    public CorsFilter corsFilter() {
        CorsConfiguration config = new CorsConfiguration();
        
        // ✅ CRITICAL: Must be true for JWT token to be sent with credentials
        config.setAllowCredentials(true);
        
        // Dev servers
        config.addAllowedOrigin("http://localhost:3000");    // customer-frontend dev
        config.addAllowedOrigin("http://localhost:5173");    // admin frontend (Vite)
        config.addAllowedOrigin("http://localhost:5174");    // legacy/other dev
        
        // Production deployments
        config.addAllowedOrigin("https://bank-gaurd-frontend.vercel.app");          // admin frontend
        config.addAllowedOrigin("https://bank-gaurd-customer-frontend.vercel.app"); // customer frontend
        
        // Allow all headers (including Authorization with Bearer token)
        config.addAllowedHeader("*");
        
        // Allow all methods
        config.addAllowedMethod("*");
        
        // Preflight cache time
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        
        return new CorsFilter(source);
    }
}

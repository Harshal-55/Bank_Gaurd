package com.cts.gemini_test_try2.config;

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
        config.setAllowCredentials(true);
        
        // Dev servers
        config.addAllowedOrigin("http://localhost:3000");  // customer-frontend dev server
        config.addAllowedOrigin("http://localhost:5173");  // analyst/risk-manager dev server
        config.addAllowedOrigin("http://localhost:5174");  // legacy / other React dev server
        
        // Production deployments
        config.addAllowedOrigin("https://bank-gaurd-frontend.vercel.app");  // admin frontend
        config.addAllowedOrigin("https://bank-gaurd-customer.vercel.app");  // customer frontend
        
        config.addAllowedHeader("*");
        config.addAllowedMethod("*");
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        
        return new CorsFilter(source);
    }
}

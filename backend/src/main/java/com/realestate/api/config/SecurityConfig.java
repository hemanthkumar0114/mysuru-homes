package com.realestate.api.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Placeholder security config so the API is runnable before phone-OTP auth exists.
 * TODO(auth): replace with OTP request/verify endpoints issuing a JWT, then lock
 * down POST /api/listings, /api/enquiries, /api/visits to authenticated users
 * and admin-only endpoints to the ADMIN role, before Phase 1 owner-posting goes live.
 */
@Configuration
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http.csrf(csrf -> csrf.disable())
                .authorizeHttpRequests(auth -> auth.anyRequest().permitAll());
        return http.build();
    }
}

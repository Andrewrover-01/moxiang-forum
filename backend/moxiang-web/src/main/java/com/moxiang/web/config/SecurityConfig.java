package com.moxiang.web.config;

import com.moxiang.common.constant.AuthConstants;
import com.moxiang.web.filter.DeviceFingerprintFilter;
import com.moxiang.web.filter.JwtAuthFilter;
import com.moxiang.web.filter.SecurityMiddlewareFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Spring Security configuration — stateless JWT-based authentication.
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    private final JwtAuthFilter jwtAuthFilter;
    private final DeviceFingerprintFilter deviceFingerprintFilter;
    private final SecurityMiddlewareFilter securityMiddlewareFilter;

    public SecurityConfig(JwtAuthFilter jwtAuthFilter,
                          DeviceFingerprintFilter deviceFingerprintFilter,
                          SecurityMiddlewareFilter securityMiddlewareFilter) {
        this.jwtAuthFilter = jwtAuthFilter;
        this.deviceFingerprintFilter = deviceFingerprintFilter;
        this.securityMiddlewareFilter = securityMiddlewareFilter;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            // CSRF disabled: this is a stateless REST API using JWT tokens in the
            // Authorization header (not cookies), so CSRF attacks are not applicable.
            // See: https://docs.spring.io/spring-security/reference/features/exploits/csrf.html#csrf-when-to-use
            .csrf(AbstractHttpConfigurer::disable)
            .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                // Public endpoints
                .requestMatchers(
                    "/api/user/register",
                    "/api/user/login",
                    "/api/captcha/**",
                    "/api/forum/list",
                    "/api/forum/{id}",
                    "/swagger-ui/**",
                    "/v3/api-docs/**"
                ).permitAll()
                .requestMatchers(HttpMethod.GET,
                    "/api/post/**",
                    "/api/novel/**",
                    "/api/comment/**",
                    "/api/follow/stats/**"
                ).permitAll()
                // Admin-only endpoints
                .requestMatchers("/api/admin/**").hasRole(AuthConstants.ROLE_ADMIN)
                // Everything else needs authentication
                .anyRequest().authenticated()
            )
            .addFilterBefore(securityMiddlewareFilter, DeviceFingerprintFilter.class)
            .addFilterBefore(deviceFingerprintFilter, JwtAuthFilter.class)
            .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }
}

package org.stockbridge.inventory.infrastructure.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.stockbridge.inventory.infrastructure.security.JwtAuthenticationFilter;

@Configuration
@EnableWebSecurity
public class SecurityConfig {
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http, JwtAuthenticationFilter jwtFilter) throws Exception {
        return http
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling(exceptions -> exceptions
                        .authenticationEntryPoint((request, response, exception) -> {
                            response.setStatus(HttpStatus.UNAUTHORIZED.value());
                            response.setCharacterEncoding("UTF-8");
                            response.setContentType("application/json");
                            response.getWriter().write("{\"error\":\"No autorizado\",\"message\":\"Se requiere autenticación para acceder a este recurso.\"}");
                        })
                        .accessDeniedHandler((request, response, exception) -> {
                            response.setStatus(HttpStatus.FORBIDDEN.value());
                            response.setCharacterEncoding("UTF-8");
                            response.setContentType("application/json");
                            response.getWriter().write("{\"error\":\"Acceso denegado\",\"message\":\"No tiene permisos para realizar esta operación.\"}");
                        }))
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers("/auth/register", "/auth/login", "/actuator/health", "/actuator/health/**").permitAll()
                        .requestMatchers("/swagger-ui/**", "/v3/api-docs/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/products/**")
                            .hasAnyAuthority("ROLE_EMPLEADO", "ROLE_BODEGA", "ROLE_ADMIN")
                        .requestMatchers(HttpMethod.POST, "/api/products/**")
                            .hasAnyAuthority("ROLE_BODEGA", "ROLE_ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/api/products/**")
                            .hasAnyAuthority("ROLE_BODEGA", "ROLE_ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/api/products/**")
                            .hasAnyAuthority("ROLE_BODEGA", "ROLE_ADMIN")
                        .requestMatchers(HttpMethod.POST, "/api/requests/*/approve", "/api/requests/*/reject", "/api/requests/*/deliver")
                            .hasAnyAuthority("ROLE_BODEGA", "ROLE_ADMIN")
                        .requestMatchers("/api/requests/**")
                            .hasAnyAuthority("ROLE_EMPLEADO", "ROLE_BODEGA", "ROLE_ADMIN")
                        .anyRequest().authenticated())
                .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class)
                .build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}

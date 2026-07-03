package com.etiya.productservice.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.SecurityFilterChain;

/**
 * product-service'i bir OAuth2 Resource Server'a dönüştürür: her isteğin
 * {@code Authorization: Bearer <token>} başlığındaki JWT, Keycloak realm'inin
 * public key'i ile doğrulanır (issuer-uri config'ten gelir).
 *
 * Yetkilendirme kuralları:
 *   - Okuma (GET)  -> geçerli token'ı olan herkes (user veya admin)
 *   - Yazma (POST/PUT/DELETE) -> yalnızca 'admin' realm rolü
 */
@Configuration
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/actuator/**").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/products/**").hasAnyRole("user", "admin")
                .requestMatchers(HttpMethod.POST, "/api/products/**").hasRole("admin")
                .requestMatchers(HttpMethod.PUT, "/api/products/**").hasRole("admin")
                .requestMatchers(HttpMethod.DELETE, "/api/products/**").hasRole("admin")
                .anyRequest().authenticated())
            .oauth2ResourceServer(oauth2 -> oauth2.jwt(jwt ->
                    jwt.jwtAuthenticationConverter(jwtAuthenticationConverter())));

        return http.build();
    }

    private JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(new KeycloakRealmRoleConverter());
        return converter;
    }
}

package com.acadezer.api.config;
import com.acadezer.api.securityJwt.*;
import com.acadezer.api.dto.response.ErrorResponse;
import org.springframework.context.annotation.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.*;
import tools.jackson.databind.json.JsonMapper;
import java.util.List;
import com.acadezer.api.ratelimit.AuthRateLimitFilter;
import com.acadezer.api.ratelimit.AuthRequestLimiter;
import com.acadezer.api.ratelimit.ClientIpResolver;
@Configuration
public class SecurityConfig {
    @Bean SecurityFilterChain security(HttpSecurity http, JwtAuthenticationFilter jwt, RestAuthenticationEntryPoint entry, JsonMapper json, CorsConfigurationSource corsConfigurationSource, AuthRequestLimiter authLimits, ClientIpResolver ips) throws Exception {
        return http.csrf(AbstractHttpConfigurer::disable).cors(cors -> cors.configurationSource(corsConfigurationSource))
            .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .exceptionHandling(e -> e.authenticationEntryPoint(entry).accessDeniedHandler((req, res, ex) -> {
                res.setStatus(403); res.setContentType("application/json;charset=UTF-8");
                res.getWriter().write(json.writeValueAsString(ErrorResponse.of(403, "Acceso denegado", req.getRequestURI())));
            }))
            .authorizeHttpRequests(a -> a
                .requestMatchers(org.springframework.http.HttpMethod.POST, "/auth", "/auth/register", "/auth/refresh", "/auth/logout").permitAll()
                .requestMatchers(org.springframework.http.HttpMethod.GET, "/health").permitAll()
                .requestMatchers("/api/admin/**").hasRole("ADMIN")
                .anyRequest().authenticated())
            .addFilterBefore(jwt, UsernamePasswordAuthenticationFilter.class)
            .addFilterBefore(new AuthRateLimitFilter(authLimits, ips, json), JwtAuthenticationFilter.class)
            .build();
    }
    @Bean PasswordEncoder passwordEncoder() { return new BCryptPasswordEncoder(); }
    @Bean UrlBasedCorsConfigurationSource corsConfigurationSource(@Value("${app.cors-origin}") String origin) {
        CorsConfiguration c = new CorsConfiguration();
        List<String> origins = java.util.Arrays.stream(origin.split(","))
            .map(String::trim).filter(value -> !value.isEmpty()).toList();
        if (origins.isEmpty() || origins.stream().anyMatch(value -> value.contains("*")))
            throw new IllegalArgumentException("CORS_ORIGIN debe contener orígenes explícitos");
        // Preserve configured production origins while explicitly allowing local Angular.
        List<String> allowed = java.util.stream.Stream.concat(origins.stream(), java.util.stream.Stream.of("http://localhost:4200"))
            .distinct().toList();
        c.setAllowedOrigins(allowed);
        c.setAllowCredentials(true);
        c.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        c.setAllowedHeaders(List.of("Authorization", "Content-Type", "X-CSRF-Protection"));
        c.setExposedHeaders(List.of("Retry-After"));
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource(); source.registerCorsConfiguration("/**", c); return source;
    }
}

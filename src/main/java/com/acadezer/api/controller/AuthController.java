package com.acadezer.api.controller;

import com.acadezer.api.dto.request.RegisterRequest;
import com.acadezer.api.dto.request.AuthRequest;
import com.acadezer.api.dto.response.UserResponse;
import com.acadezer.api.dto.response.AuthResponse;
import com.acadezer.api.service.AuthService;
import com.acadezer.api.ratelimit.AuthRequestLimiter;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.http.ResponseCookie;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {
    private final AuthService auth;
    private final AuthRequestLimiter limits;
    private final com.acadezer.api.service.RefreshTokenService refreshTokens;
    private final UrlBasedCorsConfigurationSource cors;
    @Value("${app.refresh-cookie.secure:true}") private boolean secureCookie;
    @Value("${jwt.refresh-expiration}") private long refreshExpiration;

    @GetMapping("/me")
    public UserResponse currentUser(java.security.Principal principal) {
        return auth.currentUser(principal.getName());
    }

    @PostMapping("/register")
    public ResponseEntity<UserResponse> register(@Valid @RequestBody RegisterRequest input) {
        limits.registerAccount(input.email());
        return ResponseEntity.status(201).body(auth.register(input));
    }

    @PostMapping
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody AuthRequest input) {
        limits.loginAccount(input.email());
        return withCookie(auth.login(input));
    }

    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refresh(@CookieValue(name = "acadezer_refresh", required = false) String token, HttpServletRequest request) {
        requireTrustedOrigin(request);
        if (token == null || token.isBlank()) throw new org.springframework.security.authentication.BadCredentialsException("Refresh token requerido");
        return withCookie(auth.refresh(token));
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@CookieValue(name = "acadezer_refresh", required = false) String token, HttpServletRequest request) {
        requireTrustedOrigin(request);
        if (token != null && !token.isBlank()) refreshTokens.revoke(token);
        return ResponseEntity.noContent().header("Set-Cookie", cookie("", 0)).build();
    }

    private ResponseEntity<AuthResponse> withCookie(AuthResponse response) {
        return ResponseEntity.ok().header("Set-Cookie", cookie(response.refreshToken(), refreshExpiration / 1000)).body(response);
    }

    private String cookie(String value, long maxAge) {
        return ResponseCookie.from("acadezer_refresh", value).httpOnly(true).secure(secureCookie)
            .sameSite(secureCookie ? "None" : "Lax").path("/auth").maxAge(maxAge).build().toString();
    }

    private void requireTrustedOrigin(HttpServletRequest request) {
        String origin = request.getHeader("Origin");
        var config = cors.getCorsConfiguration(request);
        if (origin == null || config == null || config.checkOrigin(origin) == null
                || !"1".equals(request.getHeader("X-CSRF-Protection")))
            throw new com.acadezer.api.exception.ApiException(403, "Origen o protección CSRF inválidos");
    }
}

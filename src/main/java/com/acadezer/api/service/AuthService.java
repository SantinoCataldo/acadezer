package com.acadezer.api.service;

import com.acadezer.api.dto.request.RegisterRequest;
import com.acadezer.api.dto.request.AuthRequest;
import com.acadezer.api.dto.response.UserResponse;
import com.acadezer.api.dto.response.AuthResponse;
import com.acadezer.api.model.UserEntity;
import com.acadezer.api.mapper.UserMapper;
import com.acadezer.api.repository.UserRepository;
import com.acadezer.api.exception.ApiException;
import com.acadezer.api.securityJwt.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.Locale;
import java.nio.charset.StandardCharsets;

@Service
@RequiredArgsConstructor
public class AuthService {
    private final UserRepository users;
    private final UserMapper mapper;
    private final PasswordEncoder encoder;
    private final JwtService jwt;
    private final RefreshTokenService refreshTokens;
    @Value("${jwt.expiration}")
    private long expiration;

    private String email(String value) {
        return value.trim().toLowerCase(Locale.ROOT);
    }

    @Transactional
    public UserResponse register(RegisterRequest input) {
        if (input.password().getBytes(StandardCharsets.UTF_8).length > 72)
            throw new ApiException(400, "La contraseña no debe superar 72 bytes UTF-8");
        if (users.existsByEmail(email(input.email())))
            throw new ApiException(409, "El email ya está registrado");
        UserEntity u = mapper.toEntity(input);
        u.setPassword(encoder.encode(input.password()));
        users.saveAndFlush(u);
        return mapper.toResponse(u);
    }

    @Transactional
    public AuthResponse login(AuthRequest input) {
        UserEntity user = users.findByEmail(email(input.email()))
                .orElseThrow(() -> new BadCredentialsException("Credenciales inválidas"));
        if (!user.isEnabled() || !encoder.matches(input.password(), user.getPassword()))
            throw new BadCredentialsException("Credenciales inválidas");
        return response(user);
    }

    @Transactional
    public AuthResponse refresh(String token) {
        return response(refreshTokens.consume(token));
    }

    private AuthResponse response(UserEntity user) {
        return new AuthResponse(jwt.generateToken(user), refreshTokens.issue(user), "Bearer", expiration / 1000, mapper.toResponse(user));
    }

    @Transactional(readOnly = true)
    public UserResponse currentUser(String email) {
        return mapper.toResponse(users.findByEmail(email)
            .orElseThrow(() -> new BadCredentialsException("Credenciales inválidas")));
    }
}

package com.acadezer.api.service;

import com.acadezer.api.model.RefreshTokenEntity;
import com.acadezer.api.model.UserEntity;
import com.acadezer.api.repository.RefreshTokenRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;

@Service @RequiredArgsConstructor
public class RefreshTokenService {
    private final RefreshTokenRepository tokens;
    private final SecureRandom random = new SecureRandom();
    @Value("${jwt.refresh-expiration}") private long expiration;

    public String issue(UserEntity user) {
        if (expiration <= 0) throw new IllegalStateException("La duración del refresh token debe ser positiva");
        tokens.deleteExpired(Instant.now());
        byte[] bytes = new byte[32];
        random.nextBytes(bytes);
        String raw = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        RefreshTokenEntity token = new RefreshTokenEntity();
        token.setTokenHash(hash(raw));
        token.setUser(user);
        token.setExpiresAt(Instant.now().plusMillis(expiration));
        tokens.save(token);
        return raw;
    }

    @Transactional
    public UserEntity consume(String raw) {
        RefreshTokenEntity token = tokens.findForUpdate(hash(raw))
            .orElseThrow(() -> new BadCredentialsException("Refresh token inválido o vencido"));
        if (!token.getExpiresAt().isAfter(Instant.now()) || !token.getUser().isEnabled())
            throw new BadCredentialsException("Refresh token inválido o vencido");
        UserEntity user = token.getUser();
        tokens.delete(token);
        tokens.flush();
        return user;
    }

    @Transactional
    public void revoke(String raw) {
        tokens.findForUpdate(hash(raw)).ifPresent(tokens::delete);
    }

    private String hash(String raw) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(raw.getBytes(StandardCharsets.UTF_8)));
        } catch (java.security.NoSuchAlgorithmException ex) {
            throw new IllegalStateException(ex);
        }
    }
}

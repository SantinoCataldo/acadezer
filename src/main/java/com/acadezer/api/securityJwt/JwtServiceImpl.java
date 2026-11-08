package com.acadezer.api.securityJwt;

import io.jsonwebtoken.*;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import javax.crypto.SecretKey;
import java.util.Date;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

@Service
public class JwtServiceImpl implements JwtService {
    private final SecretKey key;
    private final long expiration;

    public JwtServiceImpl(@Value("${jwt.secret}") String secret, @Value("${jwt.expiration}") long expiration) {
        key = Keys.hmacShaKeyFor(Decoders.BASE64.decode(secret));
        this.expiration = expiration;
    }

    public String extractUsername(String token) {
        return claims(token).getSubject();
    }

    private Claims claims(String token) {
        return Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload();
    }

    public String generateToken(UserDetails user) {
        return Jwts.builder().subject(user.getUsername()).claim("type", "access").issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + expiration)).signWith(key).compact();
    }

    public boolean isTokenValid(String token, UserDetails user) {
        Claims c = claims(token);
        return user.getUsername().equals(c.getSubject()) && "access".equals(c.get("type", String.class))
                && user.isEnabled();
    }
}
